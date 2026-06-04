package com.lawapp.backend.service;

import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;

    public BigDecimal getBalance(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getCreditBalance();
    }

    @Transactional
    public BigDecimal topUp(String email, int amount, String paymentToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Siber Güvenlik Mükemmelleştirmesi: Ödeme token imza doğrulaması simülasyonu
        if (paymentToken == null || paymentToken.length() < 10) {
            throw new IllegalArgumentException("Geçersiz ödeme imzası/token doğrulaması! Finansal işlem reddedildi.");
        }

        user.setCreditBalance(user.getCreditBalance().add(BigDecimal.valueOf(amount)));
        userRepository.save(user);

        return user.getCreditBalance();
    }
}
