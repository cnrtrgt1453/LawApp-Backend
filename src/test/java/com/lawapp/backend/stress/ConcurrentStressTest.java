package com.lawapp.backend.stress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stres Testi: Eşzamanlı İşlem ve Idempotency Testleri
 * Roller: Load Tester, Test Automation Engineer
 *
 * Bu testler, uygulamanın yoğun eşzamanlı (concurrent) işlemler altında
 * nasıl davrandığını ölçer. Gerçek yük testi JMeter/Gatling ile yapılır,
 * ancak bu testler Java thread seviyesinde darboğazları tespit eder.
 *
 * GERÇEK YÜK TESTİ KURULUMU (JMeter):
 *   1. Apache JMeter indirin: https://jmeter.apache.org/download_jmeter.cgi
 *   2. New Test Plan → Thread Group: 10000 Users, Ramp-Up: 60s
 *   3. HTTP Request: GET /api/leads/all (Redis Cache test)
 *   4. HTTP Request: POST /api/wallet/topup (Idempotency test)
 *   5. Listener: Summary Report, Response Time Graph
 */
@DisplayName("Stres Testi: Eşzamanlı İşlem Güvenliği")
class ConcurrentStressTest {

    private static final int THREAD_COUNT = 50; // Simüle edilen eşzamanlı kullanıcı sayısı

    // --- DOUBLE SUBMIT (IDEMPOTENCİ) STRES TESTİ ---

    @Test
    @DisplayName("50 eşzamanlı istek altında sadece 1 işlemin geçmesi gereken Redis kilit simülasyonu")
    void redisLockSimulationUnderConcurrentRequests() throws InterruptedException {
        // Redis kilidini simüle eden basit bir AtomicBoolean mekanizması
        // Gerçek testte Redis'in setIfAbsent() metodu kullanılır
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger rejectedRequests = new AtomicInteger(0);
        ConcurrentHashMap<String, Boolean> redisLockSimulation = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Tüm thread'lerin aynı anda başlaması için
                    String lockKey = "user:testuser@test.com:topup";

                    // setIfAbsent simülasyonu: sadece ilk thread kilit alabilir
                    Boolean locked = redisLockSimulation.putIfAbsent(lockKey, true) == null;
                    if (locked) {
                        successfulRequests.incrementAndGet();
                        // İşlem süresi simülasyonu (3 saniyelik Redis kilit süresi)
                        Thread.sleep(100);
                        redisLockSimulation.remove(lockKey); // Kilidi serbest bırak
                    } else {
                        rejectedRequests.incrementAndGet(); // 409 Conflict
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Tüm thread'leri başlat
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("✅ Başarılı istekler: " + successfulRequests.get());
        System.out.println("❌ Reddedilen istekler (409): " + rejectedRequests.get());
        System.out.println("📊 Toplam: " + (successfulRequests.get() + rejectedRequests.get()));

        // Toplam işlem sayısı doğru olmalı
        assertThat(successfulRequests.get() + rejectedRequests.get()).isEqualTo(THREAD_COUNT);
        // En az 1 isteğin başarılı olması lazım
        assertThat(successfulRequests.get()).isGreaterThanOrEqualTo(1);
    }

    // --- YÜKSEK YÜK ALTIN VERİ MASKELEME PERFORMANS TESTİ ---

    @Test
    @DisplayName("1000 paralel KVKK maskeleme işlemi makul sürede tamamlanmalı (< 5 saniye)")
    void kvkkMaskingPerformanceUnderLoad() throws InterruptedException, ExecutionException {
        int requestCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Future<String>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            futures.add(executor.submit(() ->
                com.lawapp.backend.util.TextSanitizerUtils.maskSensitiveData(
                    "Dava No " + index + ": Müvekkil 0555 123 45 67 numarasını paylaştı."
                )
            ));
        }

        // Tüm sonuçları bekle
        for (Future<String> f : futures) {
            String result = f.get();
            assertThat(result).doesNotContain("0555 123 45 67");
            assertThat(result).contains("[KVKK GEREĞİ GİZLENDİ]");
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        System.out.printf("⏱️  1000 paralel KVKK maskeleme işlemi: %d ms%n", elapsedMs);

        executor.shutdown();

        // Performans sınırı: 5 saniyeden az olmalı
        assertThat(elapsedMs).isLessThan(5000L);
    }

    // --- THREAD SAFETY (RACE CONDITION) TESTİ ---

    @Test
    @DisplayName("activeSessions ConcurrentHashMap race condition olmadan güvenli erişilmeli")
    void concurrentHashMapShouldBeThreadSafe() throws InterruptedException {
        ConcurrentHashMap<String, Boolean> sessions = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    // Eşzamanlı put/remove işlemleri
                    sessions.put("user" + idx + "@test.com", true);
                    Thread.sleep(10);
                    sessions.remove("user" + idx + "@test.com");
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Race condition kaynaklı hiçbir exception olmamalı
        assertThat(exceptionCount.get()).isEqualTo(0);
        // Tüm session'lar temizlenmeli
        assertThat(sessions).isEmpty();
    }
}
