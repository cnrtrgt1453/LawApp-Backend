package com.lawapp.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lawapp.backend.model.Role;
import com.lawapp.backend.model.User;
import com.lawapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

    public void sendNotification(Long userId, String title, String body) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                try {
                    Message message = Message.builder()
                            .setToken(user.getFcmToken())
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);
                    log.info("Successfully sent message to user {}: {}", userId, response);
                } catch (Exception e) {
                    log.error("Error sending FCM message to user {}", userId, e);
                }
            } else {
                log.warn("User {} has no FCM token. Skipping notification: {}", userId, title);
            }
        });
    }

    public void notifyLawyersAboutNewLead(String category, String leadTitle) {
        log.info("Notifying lawyers in category '{}': New lead: {}", category, leadTitle);
        // Tüm avukatları çekmek yerine sadece kategori ile eşleşen avukatlara bildirim gönder.
        // Avukatlarda kategori tercihi eklenene kadar FCM token'ı olan tüm avukatlara gönder.
        List<User> lawyers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.LAWYER)
                .filter(u -> u.isVerified())               // Sadece doğrulanmış avukatlara
                .filter(u -> u.getFcmToken() != null && !u.getFcmToken().isEmpty()) // Token'ı olanlara
                .collect(Collectors.toList());

        log.info("Sending new lead notification to {} verified lawyers", lawyers.size());
        for (User lawyer : lawyers) {
            sendNotification(lawyer.getId(), "Yeni İlan (" + category + ")", leadTitle);
        }
    }
}
