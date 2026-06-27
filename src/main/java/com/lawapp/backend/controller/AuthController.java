package com.lawapp.backend.controller;

import com.lawapp.backend.dto.*;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.UserRepository;
import com.lawapp.backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();
        
        return ResponseEntity.ok(new JwtResponse(jwt, user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = User.builder()
                .fullName(signUpRequest.getFullName())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .role(signUpRequest.getRole())
                .phoneNumber(signUpRequest.getPhoneNumber())
                .creditBalance(signUpRequest.getRole().name().equals("LAWYER") ? BigDecimal.valueOf(100) : BigDecimal.ZERO) // Başlangıç kredisi
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // İleride token karaliste (blacklist) işlemleri buraya eklenebilir.
        // Şu an frontend tarafında TokenManager temizlenerek çıkış yapılıyor.
        return ResponseEntity.ok("Logged out successfully!");
    }
}
