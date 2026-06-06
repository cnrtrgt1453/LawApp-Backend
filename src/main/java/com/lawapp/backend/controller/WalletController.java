package com.lawapp.backend.controller;

import com.lawapp.backend.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return ResponseEntity.ok(walletService.getBalance(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUp(@RequestBody SecureTopUpRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Idempotency (Double-Submit) koruması: Kullanıcı art arda tıklarsa engelle
        String lockKey = "user:" + email + ":topup";
        Boolean isLocked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 3, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(isLocked)) {
            return ResponseEntity.status(409).body("İşleminiz devam ediyor, lütfen bekleyin.");
        }

        try {
            return ResponseEntity.ok(walletService.topUp(email, request.getAmount(), request.getPaymentToken()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
