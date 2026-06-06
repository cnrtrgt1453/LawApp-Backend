package com.lawapp.backend.service;

import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
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
 * Unit Testi: WalletService
 * Roller: Unit Tester, Manuel Tester (Double Submit senaryosu)
 *
 * WalletService'in bakiye işlemlerini ve token doğrulamasını test eder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: WalletService")
class WalletServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private User mockLawyer;

    @BeforeEach
    void setUp() {
        mockLawyer = User.builder()
                .id(1L)
                .email("avukat@test.com")
                .fullName("Test Avukat")
                .role(Role.LAWYER)
                .creditBalance(BigDecimal.valueOf(100))
                .build();
    }

    // --- BAKİYE SORGULAMA TESTLERİ ---

    @Test
    @DisplayName("Mevcut kullanıcının bakiyesi doğru döndürülmeli")
    void shouldReturnCorrectBalance() {
        when(userRepository.findByEmail("avukat@test.com")).thenReturn(Optional.of(mockLawyer));

        BigDecimal balance = walletService.getBalance("avukat@test.com");

        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Var olmayan kullanıcı bakiyesi sorgulanınca RuntimeException fırlatılmalı")
    void shouldThrowExceptionForNonExistentUser() {
        when(userRepository.findByEmail("yok@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getBalance("yok@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // --- KREDİ YÜKLEME TESTLERİ ---

    @Test
    @DisplayName("Geçerli token ile kredi yükleme başarılı olmalı")
    void shouldTopUpCreditSuccessfully() {
        when(userRepository.findByEmail("avukat@test.com")).thenReturn(Optional.of(mockLawyer));
        when(userRepository.save(any(User.class))).thenReturn(mockLawyer);

        BigDecimal newBalance = walletService.topUp("avukat@test.com", 500, "GECERLI_TOKEN_12345");

        // 100 (başlangıç) + 500 = 600
        assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(600));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Null token ile kredi yükleme IllegalArgumentException fırlatmalı")
    void shouldRejectTopUpWithNullToken() {
        when(userRepository.findByEmail("avukat@test.com")).thenReturn(Optional.of(mockLawyer));

        assertThatThrownBy(() -> walletService.topUp("avukat@test.com", 100, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Geçersiz ödeme imzası");

        // Veritabanına kayıt yapılmamalı
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("10 karakterden kısa token ile kredi yükleme reddedilmeli")
    void shouldRejectTopUpWithShortToken() {
        when(userRepository.findByEmail("avukat@test.com")).thenReturn(Optional.of(mockLawyer));

        assertThatThrownBy(() -> walletService.topUp("avukat@test.com", 100, "KISA"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("0 veya negatif kredi yüklenmesi bakiyeyi artırmamalı")
    void shouldNotIncreaseBalanceWithZeroOrNegativeAmount() {
        when(userRepository.findByEmail("avukat@test.com")).thenReturn(Optional.of(mockLawyer));
        when(userRepository.save(any(User.class))).thenReturn(mockLawyer);

        BigDecimal newBalance = walletService.topUp("avukat@test.com", 0, "GECERLI_TOKEN_12345");

        // Bakiye değişmemeli
        assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}
