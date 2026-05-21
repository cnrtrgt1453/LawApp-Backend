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
    public ResponseEntity<?> topUp(@RequestBody TopUpRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        
        // Gerçek dünyada burada bir ödeme sistemi (Iyzico/Stripe) doğrulaması olur.
        // Şimdilik ödemenin başarılı olduğunu varsayıp krediyi ekliyoruz.
        user.setCreditBalance(user.getCreditBalance().add(BigDecimal.valueOf(request.getAmount())));
        userRepository.save(user);
        
        return ResponseEntity.ok(user.getCreditBalance());
    }

    @Data
    public static class TopUpRequest {
        private int amount; // Satın alınan kredi miktarı
    }
}
