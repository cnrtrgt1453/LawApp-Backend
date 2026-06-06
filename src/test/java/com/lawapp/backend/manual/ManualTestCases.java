package com.lawapp.backend.manual;

/**
 * MANUEL TEST SENARYOLARI - LawApp Backend
 * =========================================
 * Roller: Manuel Tester
 *
 * Bu dosya ÇALIŞTIRILMAZ. Sadece QA Ekibinin referansı için
 * manuel test adımlarını ve beklenen sonuçları belgeler.
 *
 * Uygulama çalışırken: docker-compose up → mvn spring-boot:run
 * API Test Aracı: Postman veya curl
 */
public class ManualTestCases {

    /*
     * ====================================================================
     * TEST GEREKSİNİMLERİ
     * ====================================================================
     * 1. Backend çalışıyor olmalı: http://localhost:8080
     * 2. PostgreSQL çalışıyor olmalı (docker-compose up)
     * 3. Redis çalışıyor olmalı (docker-compose up)
     * 4. Postman veya curl kurulu olmalı
     *
     * POSTMAN COLLECTION HAZIRLIK:
     * - Base URL: http://localhost:8080
     * - Authorization: Bearer Token (signin'dan alınan JWT)
     * ====================================================================
     */

    /*
     * ====================================================================
     * MT-001: MÜVEKKİL KAYIT VE GİRİŞ AKIŞI
     * ====================================================================
     * AÇIKLAMA: Müvekkil platformuna ilk kez katılımını test eder.
     *
     * ADIM 1: POST http://localhost:8080/api/auth/signup
     * Request Body:
     * {
     *   "fullName": "Ahmet Yılmaz",
     *   "email": "ahmet@test.com",
     *   "password": "Test1234!",
     *   "phoneNumber": "05551234567",
     *   "role": "CLIENT"
     * }
     * BEKLENEN SONUÇ: HTTP 200, "User registered successfully!"
     *
     * ADIM 2: POST http://localhost:8080/api/auth/signin
     * Request Body:
     * {
     *   "email": "ahmet@test.com",
     *   "password": "Test1234!"
     * }
     * BEKLENEN SONUÇ: HTTP 200, JSON içinde "token" alanı dolu
     *
     * ADIM 3: Aynı e-posta ile tekrar signup dene
     * BEKLENEN SONUÇ: HTTP 400, "Error: Email is already in use!"
     * ====================================================================
     */

    /*
     * ====================================================================
     * MT-002: KVKK VERİ MASKELEMESİ TESTİ (KRİTİK)
     * ====================================================================
     * AÇIKLAMA: İlan açıklamasındaki telefon numaralarının maskelendiğini doğrular.
     *
     * ÖN KOŞUL: MT-001 tamamlanmış olmalı (JWT token alınmalı)
     *
     * ADIM 1: POST http://localhost:8080/api/leads/create
     * Authorization: Bearer {token}
     * Request Body:
     * {
     *   "title": "Boşanma Davası Yardımı",
     *   "description": "Kocamın avukatı beni 05551234567 numarasından aradı.",
     *   "category": "Aile Hukuku",
     *   "city": "İstanbul"
     * }
     * BEKLENEN SONUÇ: HTTP 200, response.description = "...beni [KVKK GEREĞİ GİZLENDİ] numarasından aradı."
     *
     * ADIM 2: GET http://localhost:8080/api/leads/all
     * Authorization: Bearer {avukat_token}
     * BEKLENEN SONUÇ: İlandaki description'da telefon numarası GÖRÜNMEMELİ
     * ====================================================================
     */

    /*
     * ====================================================================
     * MT-003: DOUBLE SUBMIT (IDEMPOTENSİ) TESTİ
     * ====================================================================
     * AÇIKLAMA: Avukatın kredi yükleme butonuna art arda tıklamasını simüle eder.
     *
     * ÖN KOŞUL: Avukat hesabı açık, JWT token hazır
     *
     * ADIM 1: POST http://localhost:8080/api/wallet/topup
     * Authorization: Bearer {avukat_token}
     * Request Body: { "amount": 100, "paymentToken": "GECERLI_TOKEN_12345" }
     * BEKLENEN SONUÇ: HTTP 200, bakiye arttı
     *
     * ADIM 2: Hemen ardından (< 3 saniye) TEKRAR POST /api/wallet/topup
     * Aynı body ile aynı isteği tekrar gönder
     * BEKLENEN SONUÇ: HTTP 409, "İşleminiz devam ediyor, lütfen bekleyin."
     *
     * ADIM 3: 4 saniye bekle, tekrar POST /api/wallet/topup
     * BEKLENEN SONUÇ: HTTP 200, Redis kilidi kalktı, işlem geçti
     * ====================================================================
     */

    /*
     * ====================================================================
     * MT-004: JWT TOKEN GÜVENLİK TESTİ
     * ====================================================================
     * AÇIKLAMA: Geçersiz veya süresi dolmuş token ile koruma altındaki endpoint'e erişim.
     *
     * ADIM 1: Herhangi bir endpoint'e token olmadan istek gönder
     * GET http://localhost:8080/api/leads/all (Authorization header'ı olmadan)
     * BEKLENEN SONUÇ: HTTP 401 Unauthorized
     *
     * ADIM 2: Bozuk bir token ile istek gönder
     * Authorization: Bearer bozuk.jwt.token123
     * BEKLENEN SONUÇ: HTTP 401 Unauthorized
     *
     * ADIM 3: Müvekkil token'ı ile sadece avukata özel bir endpoint'e eriş
     * (Örn: Cüzdan bakiye endpoint'i CLIENT'a açık ama topup sadece LAWYER için)
     * BEKLENEN SONUÇ: HTTP 403 Forbidden
     * ====================================================================
     */

    /*
     * ====================================================================
     * MT-005: REDİS CACHE PERFORMANS TESTİ
     * ====================================================================
     * AÇIKLAMA: İş havuzunun önbellekten servise edildiğini doğrular.
     *
     * ADIM 1: GET /api/leads/all → İlk istek (Cache MISS, veritabanına gidilir)
     * Response Time'ı not al (örn: 250ms)
     *
     * ADIM 2: GET /api/leads/all → İkinci istek (Cache HIT, Redis'ten gelir)
     * Response Time'ı not al (örn: 15ms)
     *
     * BEKLENEN SONUÇ: İkinci istek ilk istekten çok daha hızlı olmalı (>5x)
     *
     * ADIM 3: POST /api/leads/create ile yeni ilan ekle
     * ADIM 4: GET /api/leads/all → Cache evict edildi, veritabanından yeni veriler geldi
     * BEKLENEN SONUÇ: Yeni ilan listede gözükmeli
     * ====================================================================
     */

    /*
     * ====================================================================
     * MT-006: WEB SOCKET SOHBET TESTİ
     * ====================================================================
     * ARAÇ: WebSocket Client (wscat, Postman veya tarayıcı DevTools)
     *
     * ADIM 1: wscat -c "ws://localhost:8080/ws/chat?token={avukat_jwt}"
     * BEKLENEN: Bağlantı kabul edildi
     *
     * ADIM 2: Token olmadan bağlan: wscat -c "ws://localhost:8080/ws/chat"
     * BEKLENEN: Bağlantı reddedildi (CloseStatus BAD_DATA)
     *
     * ADIM 3: Geçerli bağlantıda mesaj gönder:
     * { "sessionId": 1, "content": "Merhaba Avukat Bey" }
     * BEKLENEN: Mesaj karşı tarafa anlık iletildi, DB'ye kaydedildi
     *
     * ADIM 4: Mesaj içeriğine telefon numarası ekleyerek gönder:
     * { "sessionId": 1, "content": "Numaram: 05551234567" }
     * BEKLENEN: E2EE hazırlığı sebebiyle raw halde iletilir (şifreleme Android'de yapılacak)
     * ====================================================================
     */
}
