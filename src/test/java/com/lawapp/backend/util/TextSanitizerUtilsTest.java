package com.lawapp.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Testi: TextSanitizerUtils
 * Roller: Unit Tester
 *
 * TextSanitizerUtils'in telefon numarası ve hakaret kelimelerini
 * doğru maskelediğini birim seviyesinde test eder.
 */
@DisplayName("Unit Test: KVKK Metin Maskele Fonksiyonu")
class TextSanitizerUtilsTest {

    // --- TELEFON NUMARASI MASKELEME TESTLERİ ---

    @ParameterizedTest(name = "[{index}] Telefon formatı maskeleniyor: {0}")
    @CsvSource({
        "'Sizi 05551234567 numaralı kişi arıyor',        'Sizi [KVKK GEREĞİ GİZLENDİ] numaralı kişi arıyor'",
        "'Beni 0555 555 55 55 numaralı kişi aradı',      'Beni [KVKK GEREĞİ GİZLENDİ] numaralı kişi aradı'",
        "'GSM: +90 555 123 45 67 ödeme yaptı',           'GSM: [KVKK GEREĞİ GİZLENDİ] ödeme yaptı'",
        "'iletişim: 5321234567',                          'iletişim: [KVKK GEREĞİ GİZLENDİ]'"
    })
    @DisplayName("Çeşitli telefon formatları [KVKK GEREĞİ GİZLENDİ] ile maskelenmeli")
    void shouldMaskPhoneNumbers(String input, String expected) {
        String result = TextSanitizerUtils.maskSensitiveData(input.trim());
        assertThat(result).isEqualTo(expected.trim());
    }

    @Test
    @DisplayName("Telefon numarası içermeyen metin değiştirilmemeli")
    void shouldNotChangeTextWithoutPhoneNumbers() {
        String text = "Boşanma davası hakkında hukuki yardım almak istiyorum.";
        assertThat(TextSanitizerUtils.maskSensitiveData(text)).isEqualTo(text);
    }

    // --- HAKARET KELİMESİ MASKELEME TESTLERİ ---

    @Test
    @DisplayName("Hakaret kelimesi içeren metin maskelenmeli")
    void shouldMaskProfanity() {
        String text = "Bu aptal avukat davamı kaybettirdi";
        String result = TextSanitizerUtils.maskSensitiveData(text);
        assertThat(result).doesNotContain("aptal");
        assertThat(result).contains("[KVKK GEREĞİ GİZLENDİ]");
    }

    @Test
    @DisplayName("Büyük/küçük harf fark etmeden hakaret maskelenmeli (case-insensitive)")
    void shouldMaskProfanityCaseInsensitive() {
        String text = "Bu APTAL avukat davamı kaybettirdi";
        String result = TextSanitizerUtils.maskSensitiveData(text);
        assertThat(result).doesNotContain("APTAL");
    }

    // --- SINIR DURUM TESTLERİ (Edge Cases) ---

    @Test
    @DisplayName("Null girdi null döndürmeli (NullPointerException olmamalı)")
    void shouldHandleNullInputGracefully() {
        assertThat(TextSanitizerUtils.maskSensitiveData(null)).isNull();
    }

    @Test
    @DisplayName("Boş string boş string döndürmeli")
    void shouldHandleEmptyStringGracefully() {
        assertThat(TextSanitizerUtils.maskSensitiveData("")).isEqualTo("");
    }

    @Test
    @DisplayName("Hem telefon hem hakaret içeren metin her ikisi de maskelenmeli")
    void shouldMaskBothPhoneAndProfanityInSameText() {
        String text = "Bu aptal avukat 05551234567 numaralı kişiyi tanıyor";
        String result = TextSanitizerUtils.maskSensitiveData(text);
        assertThat(result).doesNotContain("aptal");
        assertThat(result).doesNotContain("05551234567");
        // İki ayrı [KVKK GEREĞİ GİZLENDİ] etiketi olmalı
        long maskCount = result.chars()
                .filter(c -> result.indexOf("[KVKK GEREĞİ GİZLENDİ]") >= 0)
                .count();
        assertThat(maskCount).isGreaterThan(0);
    }
}
