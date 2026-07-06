package com.lawapp.backend.service;

import com.lawapp.backend.model.*;
import com.lawapp.backend.repository.AppointmentRepository;
import com.lawapp.backend.repository.CalendarSlotRepository;
import com.lawapp.backend.repository.LeadRepository;
import com.lawapp.backend.repository.UserRepository;
import com.lawapp.backend.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CalendarSlotRepository calendarSlotRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final NotificationService notificationService;

    private BigDecimal getPlatformFeeByCategory(String category) {
        return BigDecimal.valueOf(100.0);
    }

    @Transactional
    public Appointment createAppointment(String clientEmail, Long lawyerId, Long leadId, LocalDateTime appointmentTime) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (client.getRole() != Role.CLIENT) {
            throw new RuntimeException("Only clients can book appointments");
        }

        User lawyer = userRepository.findById(lawyerId)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        if (lawyer.getRole() != Role.LAWYER) {
            throw new RuntimeException("Target user is not a lawyer");
        }

        if (!lawyer.isVerified()) {
            throw new RuntimeException("Bu avukat henüz baro tarafından doğrulanmamıştır.");
        }

        Lead lead = leadId != null ? leadRepository.findById(leadId).orElse(null) : null;
        String category = lead != null ? lead.getCategory() : "Genel Danışmanlık";

        // Takvim slotunu kontrol et
        CalendarSlot slot = calendarSlotRepository.findByLawyerIdAndSlotTime(lawyerId, appointmentTime)
                .orElseThrow(() -> new RuntimeException("Bu saat dilimi avukatın çalışma takviminde bulunmuyor"));

        // Çakışma kontrolü: Sadece onaylanmış randevularla çakışma olmasın, bekleyen tekliflerin birikmesine izin verilsin
        boolean hasAcceptedConflict = appointmentRepository.existsByLawyerIdAndAppointmentTimeAndStatusIn(
                lawyerId, appointmentTime, Arrays.asList(AppointmentStatus.ACCEPTED)
        );
        if (hasAcceptedConflict) {
            throw new RuntimeException("Seçilen saat dilimi zaten dolu ve onaylanmış bir randevu mevcut");
        }

        BigDecimal fee = getPlatformFeeByCategory(category);

        // NOT: Slotu kapatmıyoruz. Slot ancak randevu kabul edildiğinde (ACCEPTED) kapatılacak.

        // Randevuyu oluştur (Ödeme başarılı varsayılıyor, PaymentCheckoutScreen simüle eder)
        Appointment appointment = Appointment.builder()
                .client(client)
                .lawyer(lawyer)
                .lead(lead)
                .appointmentTime(appointmentTime)
                .platformFee(fee)
                .paymentStatus("PAID") // Simüle ödeme başarılı
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // Avukata bildirim gönder
        notificationService.sendNotification(
                lawyer.getId(),
                "Yeni Randevu Talebi!",
                client.getFullName() + " isimli müvekkil sizden " + appointmentTime.toString() + " tarihi için görüntülü görüşme randevusu talep etti."
        );

        return saved;
    }

    @Transactional
    public Appointment acceptAppointment(String lawyerEmail, Long appointmentId) {
        User lawyer = userRepository.findByEmail(lawyerEmail)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getLawyer().getId().equals(lawyer.getId())) {
            throw new RuntimeException("You can only accept appointments scheduled with you");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new RuntimeException("Appointment is not in PENDING state");
        }

        // Takvim slotunu kapat
        calendarSlotRepository.findByLawyerIdAndSlotTime(lawyer.getId(), appointment.getAppointmentTime())
                .ifPresent(slot -> {
                    slot.setAvailable(false);
                    calendarSlotRepository.save(slot);
                });

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setRoomId(UUID.randomUUID().toString()); // Görüntülü konuşma odası atanır

        Appointment saved = appointmentRepository.save(appointment);

        // Müvekkile bildirim gönder
        notificationService.sendNotification(
                appointment.getClient().getId(),
                "Randevunuz Onaylandı!",
                "Av. " + lawyer.getFullName() + " randevu talebinizi onayladı. Görüşme saatinden 10 dakika önce uygulamadan arayabilirsiniz."
        );

        // Reddedilmek zorunda kalan diğer müvekkillerin bekleyen randevularını bul
        List<Appointment> pendingConflicts = appointmentRepository.findByLawyerIdAndAppointmentTimeAndStatus(
                lawyer.getId(), appointment.getAppointmentTime(), AppointmentStatus.PENDING
        );

        for (Appointment conflictingApp : pendingConflicts) {
            if (conflictingApp.getId().equals(appointmentId)) {
                continue; // Kabul edilen randevuyu atla
            }
            // Durumunu REDDEDİLDİ yap ve iade et
            conflictingApp.setStatus(AppointmentStatus.REJECTED);
            conflictingApp.setPaymentStatus("REFUNDED");
            appointmentRepository.save(conflictingApp);

            // Bildirim gönder
            notificationService.sendNotification(
                    conflictingApp.getClient().getId(),
                    "Randevu Talebi Çakışma Nedeniyle Reddedildi",
                    "Av. " + lawyer.getFullName() + " bu saatteki başka bir randevu talebini onayladı. Randevunuz iptal edildi ve ücret iade edildi. Avukatınız sizinle mesajlar üzerinden iletişime geçecektir."
            );

            // Sohbet odası oluştur (avukatın müvekkile mesaj atabilmesi için)
            Lead conflictingLead = conflictingApp.getLead();
            if (conflictingLead == null) {
                // Eğer randevuya bağlı ilan yoksa, müvekkilin ilk ilanını bul, yoksa yeni bir genel danışmanlık ilanı oluştur
                conflictingLead = leadRepository.findByClientId(conflictingApp.getClient().getId()).stream().findFirst().orElse(null);
                if (conflictingLead == null) {
                    conflictingLead = Lead.builder()
                            .title("Hukuki Danışmanlık Talebi")
                            .description("Sistem tarafından otomatik oluşturulan danışmanlık ilanı.")
                            .category("Genel Danışmanlık")
                            .city("İstanbul")
                            .client(conflictingApp.getClient())
                            .status(LeadStatus.OPEN)
                            .build();
                    conflictingLead = leadRepository.save(conflictingLead);
                }
            }

            ChatSession chatSession = chatSessionRepository.findByLeadIdAndLawyerId(conflictingLead.getId(), lawyer.getId())
                    .orElse(null);
            if (chatSession == null) {
                chatSession = ChatSession.builder()
                        .lead(conflictingLead)
                        .client(conflictingApp.getClient())
                        .lawyer(lawyer)
                        .active(true)
                        .build();
                chatSessionRepository.save(chatSession);
            } else if (!chatSession.isActive()) {
                chatSession.setActive(true);
                chatSessionRepository.save(chatSession);
            }
        }

        return saved;
    }

    @Transactional
    public Appointment rejectAppointment(String lawyerEmail, Long appointmentId) {
        User lawyer = userRepository.findByEmail(lawyerEmail)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getLawyer().getId().equals(lawyer.getId())) {
            throw new RuntimeException("You can only reject appointments scheduled with you");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new RuntimeException("Cannot reject appointment in this state");
        }

        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setPaymentStatus("REFUNDED"); // Platform ücreti müvekkile iade edilir

        // Takvim slotunu tekrar aç
        calendarSlotRepository.findByLawyerIdAndSlotTime(lawyer.getId(), appointment.getAppointmentTime())
                .ifPresent(slot -> {
                    slot.setAvailable(true);
                    calendarSlotRepository.save(slot);
                });

        Appointment saved = appointmentRepository.save(appointment);

        // Müvekkile bildirim gönder
        notificationService.sendNotification(
                appointment.getClient().getId(),
                "Randevu Talebi Reddedildi",
                "Av. " + lawyer.getFullName() + " randevu talebinizi reddetti. Platform kullanım ücretiniz iade edilmiştir."
        );

        return saved;
    }

    public List<Appointment> getAppointmentsForClient(String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return appointmentRepository.findByClientId(client.getId());
    }

    public List<Appointment> getAppointmentsForLawyer(String lawyerEmail) {
        User lawyer = userRepository.findByEmail(lawyerEmail)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));
        return appointmentRepository.findByLawyerId(lawyer.getId());
    }
}
