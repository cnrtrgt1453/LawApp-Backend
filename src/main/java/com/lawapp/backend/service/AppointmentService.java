package com.lawapp.backend.service;

import com.lawapp.backend.model.*;
import com.lawapp.backend.repository.AppointmentRepository;
import com.lawapp.backend.repository.CalendarSlotRepository;
import com.lawapp.backend.repository.LeadRepository;
import com.lawapp.backend.repository.UserRepository;
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

        if (!slot.isAvailable()) {
            throw new RuntimeException("Seçilen saat dilimi zaten dolu");
        }

        // Çakışma kontrolü
        boolean hasConflict = appointmentRepository.existsByLawyerIdAndAppointmentTimeAndStatusIn(
                lawyerId, appointmentTime, Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.ACCEPTED)
        );
        if (hasConflict) {
            throw new RuntimeException("Bu saat dilimi için zaten bekleyen veya onaylanmış bir randevu mevcut");
        }

        BigDecimal fee = getPlatformFeeByCategory(category);

        // Slotu kapat
        slot.setAvailable(false);
        calendarSlotRepository.save(slot);

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

        appointment.setStatus(AppointmentStatus.ACCEPTED);
        appointment.setRoomId(UUID.randomUUID().toString()); // Görüntülü konuşma odası atanır

        Appointment saved = appointmentRepository.save(appointment);

        // Müvekkile bildirim gönder
        notificationService.sendNotification(
                appointment.getClient().getId(),
                "Randevunuz Onaylandı!",
                "Av. " + lawyer.getFullName() + " randevu talebinizi onayladı. Görüşme saatinden 10 dakika önce uygulamadan arayabilirsiniz."
        );

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
