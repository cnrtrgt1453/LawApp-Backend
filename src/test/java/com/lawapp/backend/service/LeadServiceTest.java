package com.lawapp.backend.service;

import com.lawapp.backend.model.Lead;
import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.LeadRepository;
import com.lawapp.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Testi: LeadService
 * Roller: Unit Tester
 *
 * LeadService iş mantığını veritabanından bağımsız (Mock) olarak test eder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: LeadService")
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LeadService leadService;

    private User mockClientUser;
    private User mockLawyerUser;

    @BeforeEach
    void setUp() {
        mockClientUser = User.builder()
                .id(1L)
                .email("musvekkil@test.com")
                .fullName("Test Müvekkil")
                .role(Role.CLIENT)
                .creditBalance(BigDecimal.ZERO)
                .build();

        mockLawyerUser = User.builder()
                .id(2L)
                .email("avukat@test.com")
                .fullName("Test Avukat")
                .role(Role.LAWYER)
                .creditBalance(BigDecimal.valueOf(100))
                .build();
    }

    // --- İLAN OLUŞTURMA TESTLERİ ---

    @Test
    @DisplayName("Müvekkil başarıyla ilan oluşturabilmeli")
    void clientShouldCreateLeadSuccessfully() {
        // Given
        Lead lead = Lead.builder()
                .title("Boşanma Davası Desteği")
                .description("Eşimden boşanmak istiyorum.")
                .category("Aile Hukuku")
                .city("İstanbul")
                .build();

        when(userRepository.findByEmail("musvekkil@test.com")).thenReturn(Optional.of(mockClientUser));
        when(leadRepository.save(any(Lead.class))).thenReturn(lead);
        doNothing().when(notificationService).notifyLawyersAboutNewLead(any(), any());

        // When
        Lead result = leadService.createLead("musvekkil@test.com", lead);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Boşanma Davası Desteği");
        verify(leadRepository, times(1)).save(any(Lead.class));
        verify(notificationService, times(1)).notifyLawyersAboutNewLead(any(), any());
    }

    @Test
    @DisplayName("Avukat ilan oluşturmaya çalışırsa IllegalArgumentException fırlatılmalı")
    void lawyerShouldNotBeAbleToCreateLead() {
        // Given
        Lead lead = Lead.builder().title("Geçersiz İlan").build();
        when(userRepository.findByEmail("avukat@test.com")).thenReturn(Optional.of(mockLawyerUser));

        // When & Then
        assertThatThrownBy(() -> leadService.createLead("avukat@test.com", lead))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only clients can create leads");

        verify(leadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Açıklamadaki telefon numaraları kayıt öncesinde maskelenmeli")
    void phoneNumbersInDescriptionShouldBeMasked() {
        // Given
        Lead lead = Lead.builder()
                .title("Tazminat Davası")
                .description("Avukatım 05551234567 numaralı kişiyi aramasını söyledi.")
                .category("İş Hukuku")
                .city("Ankara")
                .build();

        when(userRepository.findByEmail("musvekkil@test.com")).thenReturn(Optional.of(mockClientUser));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(notificationService).notifyLawyersAboutNewLead(any(), any());

        // When
        Lead result = leadService.createLead("musvekkil@test.com", lead);

        // Then - Telefon numarası veritabanına kayıt edilmeden önce maskelenmeli
        assertThat(result.getDescription()).doesNotContain("05551234567");
        assertThat(result.getDescription()).contains("[KVKK GEREĞİ GİZLENDİ]");
    }

    @Test
    @DisplayName("Var olmayan kullanıcı için ilan oluşturulmamalı")
    void nonExistentUserShouldThrowException() {
        // Given
        when(userRepository.findByEmail("yok@test.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> leadService.createLead("yok@test.com", new Lead()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Client not found");
    }
}
