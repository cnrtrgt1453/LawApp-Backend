package com.lawapp.backend.service;

import com.lawapp.backend.model.*;
import com.lawapp.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BidService için kritik iş mantığı testleri.
 * Karar-7 (Zeynep Öz - Test Mühendisi): En az 5 senaryo zorunlu.
 *
 * Not: Gerçek PostgreSQL container ile çalışmak için
 * Testcontainers konfigürasyonu ayrı bir @TestConfiguration ile eklenebilir.
 * Şimdilik H2 in-memory kullanılarak mantık doğrulanmaktadır.
 */
@SpringBootTest
@Transactional
class BidServiceIntegrationTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private BidRepository bidRepository;

    @MockBean
    private NotificationService notificationService; // WireMock yerine mock — FCM bağımlılığı yok

    private User lawyer;
    private User client;
    private Lead lead;

    @BeforeEach
    void setUp() {
        // Doğrulanmış avukat
        lawyer = User.builder()
                .fullName("Av. Test")
                .email("avukat@test.com")
                .password("hashed")
                .role(Role.LAWYER)
                .creditBalance(BigDecimal.valueOf(100))
                .verified(true)
                .barNumber("06-12345")
                .build();
        userRepository.save(lawyer);

        // Müvekkil
        client = User.builder()
                .fullName("Test Müvekkil")
                .email("muvekkil@test.com")
                .password("hashed")
                .role(Role.CLIENT)
                .creditBalance(BigDecimal.ZERO)
                .verified(false)
                .build();
        userRepository.save(client);

        // Açık ilan
        lead = Lead.builder()
                .title("Boşanma davası")
                .description("Eşimden ayrılmak istiyorum")
                .category("Boşanma")
                .city("İstanbul")
                .status(LeadStatus.OPEN)
                .client(client)
                .build();
        leadRepository.save(lead);

        // Notification mock'la
        doNothing().when(notificationService).sendNotification(anyLong(), anyString(), anyString());
    }

    // === SENARYO 1 ===
    @Test
    @DisplayName("Karar-3: Başarılı teklif → Boşanma kategorisi 30 kredi düşmeli")
    void placeBid_whenValid_shouldDeductCorrectCredits() {
        bidService.placeBid(lead.getId(), lawyer.getEmail(), "Uzmanım, yardımcı olurum.");

        User updatedLawyer = userRepository.findByEmail(lawyer.getEmail()).orElseThrow();
        assertThat(updatedLawyer.getCreditBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(70)); // 100 - 30 = 70
    }

    // === SENARYO 2 ===
    @Test
    @DisplayName("Karar-1: Doğrulanmamış avukat teklif verememeli")
    void placeBid_whenNotVerified_shouldThrow() {
        lawyer.setVerified(false);
        userRepository.save(lawyer);

        assertThatThrownBy(() ->
            bidService.placeBid(lead.getId(), lawyer.getEmail(), "Teklif")
        ).hasMessageContaining("doğrulanmamış");
    }

    // === SENARYO 3 ===
    @Test
    @DisplayName("Yetersiz kredi durumunda teklif reddedilmeli")
    void placeBid_whenInsufficientCredits_shouldThrow() {
        lawyer.setCreditBalance(BigDecimal.valueOf(5)); // 5 kredi, Boşanma için 30 gerekli
        userRepository.save(lawyer);

        assertThatThrownBy(() ->
            bidService.placeBid(lead.getId(), lawyer.getEmail(), "Teklif")
        ).hasMessageContaining("Insufficient credits");
    }

    // === SENARYO 4 ===
    @Test
    @DisplayName("Karar-3: Teklif kabulünde diğer teklifler saveAll ile REJECTED olmalı")
    void acceptBid_shouldRejectAllOtherBids_inBatch() {
        // İkinci avukat
        User lawyer2 = User.builder()
                .fullName("Av. İkinci")
                .email("avukat2@test.com")
                .password("hashed")
                .role(Role.LAWYER)
                .creditBalance(BigDecimal.valueOf(100))
                .verified(true)
                .barNumber("06-99999")
                .build();
        userRepository.save(lawyer2);

        // Her iki avukat da teklif versin
        Bid bid1 = bidService.placeBid(lead.getId(), lawyer.getEmail(), "Birinci teklif");
        bidService.placeBid(lead.getId(), lawyer2.getEmail(), "İkinci teklif");

        // Müvekkil birinci teklifi kabul etsin
        bidService.acceptBid(bid1.getId(), client.getEmail());

        // bid1 ACCEPTED, bid2 REJECTED olmalı
        assertThat(bidRepository.findById(bid1.getId()).orElseThrow().getStatus())
                .isEqualTo(BidStatus.ACCEPTED);
        assertThat(bidRepository.findByLeadId(lead.getId()).stream()
                .filter(b -> !b.getId().equals(bid1.getId()))
                .allMatch(b -> b.getStatus() == BidStatus.REJECTED))
                .isTrue();
    }

    // === SENARYO 5 ===
    @Test
    @DisplayName("Aynı avukat aynı ilana iki kez teklif verememeli")
    void placeBid_whenAlreadyBid_shouldThrow() {
        bidService.placeBid(lead.getId(), lawyer.getEmail(), "Birinci teklif");

        assertThatThrownBy(() ->
            bidService.placeBid(lead.getId(), lawyer.getEmail(), "İkinci teklif")
        ).hasMessageContaining("already placed a bid");
    }

    // === SENARYO 6 ===
    @Test
    @DisplayName("Karar-2: Kabul sonrası müvekkil olmayan biri teklif kabul edemez")
    void acceptBid_whenNotOwner_shouldThrow() {
        User stranger = User.builder()
                .fullName("Yabancı Müvekkil")
                .email("yabanci@test.com")
                .password("hashed")
                .role(Role.CLIENT)
                .creditBalance(BigDecimal.ZERO)
                .verified(false)
                .build();
        userRepository.save(stranger);

        Bid bid = bidService.placeBid(lead.getId(), lawyer.getEmail(), "Teklif");

        assertThatThrownBy(() ->
            bidService.acceptBid(bid.getId(), stranger.getEmail())
        ).hasMessageContaining("your own leads");
    }
}
