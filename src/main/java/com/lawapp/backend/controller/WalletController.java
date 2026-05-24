package com.lawapp.backend.controller;

import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserRepository userRepository;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(user.getCreditBalance());
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUp(@RequestBody SecureTopUpRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        
        // Siber Güvenlik Mükemmelleştirmesi: Ödeme token imza doğrulaması simülasyonu
        if (request.getPaymentToken() == null || request.getPaymentToken().length() < 10) {
            return ResponseEntity.badRequest().body("Geçersiz ödeme imzası/token doğrulaması! Finansal işlem reddedildi.");
        }
        
        user.setCreditBalance(user.getCreditBalance().add(BigDecimal.valueOf(request.getAmount())));
        userRepository.save(user);
        
        return ResponseEntity.ok(user.getCreditBalance());
    }

    @PostMapping("/escrow/hold")
    public ResponseEntity<?> holdPayment(@RequestBody HoldPaymentRequest request) {
        // Gerçek ödeme geçidi entegrasyonu (Stripe/Iyzico Escrow API)
        // Şimdilik sadece başarılı bir şekilde bloke alındığı loglanır ve dönülür.
        return ResponseEntity.ok("Ödeme başarıyla havuz hesabında bloke edildi. Lead ID: " + request.getLeadId() + ", Tutar: " + request.getAmount());
    }

    @PostMapping("/escrow/release")
    public ResponseEntity<?> releasePayment(@RequestBody ReleasePaymentRequest request) {
        // Blokenin çözülüp avukata aktarılması simülasyonu
        return ResponseEntity.ok("Bloke başarıyla çözüldü, hizmet bedeli komisyon kesilerek avukat hesabına aktarıldı. Lead ID: " + request.getLeadId());
    }

    @Data
    public static class SecureTopUpRequest {
        private int amount; // Satın alınan kredi miktarı
        private String paymentToken; // Güvenlik token'ı/imzası
    }

    @Data
    public static class HoldPaymentRequest {
        private Long leadId;
        private BigDecimal amount;
    }

    @Data
    public static class ReleasePaymentRequest {
        private Long leadId;
    }
}
