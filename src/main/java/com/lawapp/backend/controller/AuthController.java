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
        try {
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

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signUpRequest.getEmail(), signUpRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            return ResponseEntity.ok(new JwtResponse(jwt, user.getEmail(), user.getRole().name()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during signup: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // İleride token karaliste (blacklist) işlemleri buraya eklenebilir.
        // Şu an frontend tarafında TokenManager temizlenerek çıkış yapılıyor.
        return ResponseEntity.ok("Logged out successfully!");
    }

    @lombok.Data
    public static class SocialLoginRequest {
        private String token;
        private String role; // "LAWYER" or "CLIENT"
    }

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody SocialLoginRequest req) {
        try {
            String email;
            String name;
            
            if ("mock-google-token".equals(req.getToken())) {
                email = "test-google-" + req.getRole().toLowerCase() + "@lawapp.com";
                name = "Google Test " + (req.getRole().equals("LAWYER") ? "Avukat" : "Müvekkil");
            } else {
                // Verify token with Google
                String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + req.getToken();
                java.util.Map<?, ?> response = restTemplate.getForObject(url, java.util.Map.class);
                
                if (response == null || response.containsKey("error_description")) {
                    return ResponseEntity.badRequest().body("Invalid Google token");
                }
                
                email = (String) response.get("email");
                name = (String) response.get("name");
            }
            
            return processSocialUser(email, name, req.getRole());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Google Login Error: " + e.getMessage());
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(@RequestBody SocialLoginRequest req) {
        try {
            String email;
            String name;
            
            if ("mock-facebook-token".equals(req.getToken())) {
                email = "test-facebook-" + req.getRole().toLowerCase() + "@lawapp.com";
                name = "Facebook Test " + (req.getRole().equals("LAWYER") ? "Avukat" : "Müvekkil");
            } else {
                // Verify token with Facebook
                String url = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + req.getToken();
                java.util.Map<?, ?> response = restTemplate.getForObject(url, java.util.Map.class);
                
                if (response == null || response.containsKey("error")) {
                    return ResponseEntity.badRequest().body("Invalid Facebook token");
                }
                
                email = (String) response.get("email");
                name = (String) response.get("name");
                
                if (email == null) {
                    String id = (String) response.get("id");
                    email = id + "@facebook.com";
                }
            }
            
            return processSocialUser(email, name, req.getRole());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Facebook Login Error: " + e.getMessage());
        }
    }

    private ResponseEntity<?> processSocialUser(String email, String name, String roleStr) {
        com.lawapp.backend.model.Role role = com.lawapp.backend.model.Role.valueOf(roleStr);
        
        java.util.Optional<User> userOpt = userRepository.findByEmail(email);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = User.builder()
                    .fullName(name != null ? name : "Social User")
                    .email(email)
                    .password(encoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(role)
                    .phoneNumber("")
                    .creditBalance(role == com.lawapp.backend.model.Role.LAWYER ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                    .build();
            userRepository.save(user);
        }
        
        String jwt = jwtUtils.generateJwtTokenFromUsername(user.getEmail());
        return ResponseEntity.ok(new JwtResponse(jwt, user.getEmail(), user.getRole().name()));
    }
}
