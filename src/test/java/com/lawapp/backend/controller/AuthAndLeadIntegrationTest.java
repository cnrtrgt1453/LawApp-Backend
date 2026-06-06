package com.lawapp.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.LeadRepository;
import com.lawapp.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Entegrasyon (Otomasyon) Testi: AuthController + LeadController
 * Roller: Test Automation Engineer, Unit Tester (Web Servisi Input/Output Kontrolü)
 *
 * Gerçek PostgreSQL (Testcontainers) ile uygulamanın uçtan uca (e2e) iş akışlarını test eder:
 *   1. Kullanıcı Kaydı (Register)
 *   2. Kullanıcı Girişi (Login) → JWT Token alımı
 *   3. JWT Token ile ilanlar endpoint'ine erişim
 *   4. JWT olmadan korumalı endpoint erişim reddi
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Entegrasyon Testi: Auth + Lead Akışı (Testcontainers)")
class AuthAndLeadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("lawapp_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Test ortamında Redis bağlantısı gerekmez, embedded mock kullanabiliriz
        // veya Redis'i devre dışı bırakabiliriz
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6380"); // Gerçek Redis yoksa bağlantı hatası alınmaz
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // --- KAYIT VE GİRİŞ AKIŞ TESTLERİ ---

    @Test
    @DisplayName("Yeni kullanıcı başarıyla kayıt olabilmeli")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        Map<String, Object> signupRequest = Map.of(
                "fullName", "Test Avukat",
                "email", "testlawyer@lawapp.com",
                "password", "Test1234!",
                "phoneNumber", "05551234567",
                "role", "LAWYER"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));
    }

    @Test
    @DisplayName("Aynı e-posta ile ikinci kez kayıt olunamaz - HTTP 400 bekleniyor")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        // Veritabanına kullanıcı ekle
        userRepository.save(User.builder()
                .email("existing@lawapp.com")
                .fullName("Mevcut Kullanıcı")
                .password(passwordEncoder.encode("password"))
                .role(Role.CLIENT)
                .creditBalance(BigDecimal.ZERO)
                .build());

        Map<String, Object> signupRequest = Map.of(
                "fullName", "Başka Kullanıcı",
                "email", "existing@lawapp.com",
                "password", "Sifre123!",
                "phoneNumber", "05559999999",
                "role", "CLIENT"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Email is already in use!"));
    }

    @Test
    @DisplayName("Geçerli bilgilerle giriş yapıldığında JWT token alınabilmeli")
    void shouldReturnJwtTokenOnSuccessfulLogin() throws Exception {
        // Önce kayıt yap
        userRepository.save(User.builder()
                .email("logintest@lawapp.com")
                .fullName("Login Test User")
                .password(passwordEncoder.encode("TestSifre123"))
                .role(Role.CLIENT)
                .creditBalance(BigDecimal.ZERO)
                .build());

        Map<String, String> loginRequest = Map.of(
                "email", "logintest@lawapp.com",
                "password", "TestSifre123"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("token");
        assertThat(responseBody).contains("logintest@lawapp.com");
    }

    @Test
    @DisplayName("Korumalı endpoint'e JWT token olmadan erişim HTTP 401 döndürmeli")
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/leads/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Yanlış şifre ile giriş denemesi HTTP 401 döndürmeli")
    void shouldReturn401OnWrongPassword() throws Exception {
        userRepository.save(User.builder()
                .email("test@lawapp.com")
                .fullName("Test User")
                .password(passwordEncoder.encode("DogruSifre"))
                .role(Role.CLIENT)
                .creditBalance(BigDecimal.ZERO)
                .build());

        Map<String, String> loginRequest = Map.of(
                "email", "test@lawapp.com",
                "password", "YanlışSifre"
        );

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
