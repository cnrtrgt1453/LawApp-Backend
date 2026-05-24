package com.lawapp.backend.service;

import com.lawapp.backend.model.*;
import com.lawapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final NotificationService notificationService;
    private final ChatSessionRepository chatSessionRepository;

    private BigDecimal getBidCostByCategory(String category) {
        if (category == null) return BigDecimal.valueOf(20.0);
        switch (category.toLowerCase()) {
            case "boşanma":
            case "ağır ceza":
                return BigDecimal.valueOf(30.0);
            case "iş hukuku":
            case "ticaret hukuku":
                return BigDecimal.valueOf(25.0);
            default:
                return BigDecimal.valueOf(20.0);
        }
    }

    @Transactional
    public Bid placeBid(Long leadId, String lawyerEmail, String message) {
        User lawyer = userRepository.findByEmail(lawyerEmail)
                .orElseThrow(() -> new RuntimeException("Lawyer not found"));

        if (!lawyer.getRole().equals(Role.LAWYER)) {
            throw new RuntimeException("Only lawyers can place bids");
        }

        if (!lawyer.isVerified()) {
            throw new RuntimeException("Hesabınız henüz doğrulanmamış. Lütfen baro numaranızı ve ruhsat belgenizi kaydedip sistem onayını bekleyin.");
        }

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new RuntimeException("This lead is no longer open for bids");
        }

        BigDecimal cost = getBidCostByCategory(lead.getCategory());

        if (lawyer.getCreditBalance().compareTo(cost) < 0) {
            throw new RuntimeException("Insufficient credits. Required: " + cost);
        }

        if (bidRepository.existsByLeadIdAndLawyerId(leadId, lawyer.getId())) {
            throw new RuntimeException("You have already placed a bid for this lead");
        }

        // Krediyi düş
        lawyer.setCreditBalance(lawyer.getCreditBalance().subtract(cost));
        userRepository.save(lawyer);

        // Teklifi kaydet
        Bid bid = Bid.builder()
                .lead(lead)
                .lawyer(lawyer)
                .message(message)
                .status(BidStatus.PENDING)
                .build();

        Bid savedBid = bidRepository.save(bid);

        // Müvekkile haber ver
        notificationService.sendNotification(
            lead.getClient().getId(), 
            "Yeni Ön Görüşme Başvurusu!", 
            lawyer.getFullName() + " isimli avukat danışmanlık talebinize ön görüşme başvurusu gönderdi."
        );

        return savedBid;
    }

    public List<Bid> getBidsForLead(Long leadId) {
        return bidRepository.findByLeadId(leadId);
    }

    public List<Bid> getBidsForLeadWithAuth(Long leadId, String userEmail) {
        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        
        // Güvenlik Kontrolü: 
        // 1. İsteği atan kişi CLIENT ise, ilanı açan müvekkil olmalı.
        // 2. İsteği atan kişi LAWYER ise, doğrulanmış (verified) bir avukat olmalı.
        if (requester.getRole() == Role.CLIENT && !lead.getClient().getId().equals(requester.getId())) {
            throw new RuntimeException("Bu talebin başvurularını görüntüleme yetkiniz yoktur!");
        }
        
        if (requester.getRole() == Role.LAWYER && !requester.isVerified()) {
            throw new RuntimeException("Başvuruları görüntülemek için doğrulanmış avukat olmalısınız!");
        }

        return bidRepository.findByLeadId(leadId);
    }

    @Transactional
    public Bid acceptBid(Long bidId, String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException("Bid not found"));

        Lead lead = bid.getLead();

        if (!lead.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("You can only accept bids for your own leads");
        }

        if (lead.getStatus() != LeadStatus.OPEN) {
            throw new RuntimeException("This lead is already closed or accepted");
        }

        // Statüleri güncelle
        bid.setStatus(BidStatus.ACCEPTED);
        lead.setStatus(LeadStatus.ACCEPTED);

        // Diğer teklifleri toplu olarak reddet (batch - tek SQL)
        List<Bid> allBids = bidRepository.findByLeadId(lead.getId());
        allBids.stream()
            .filter(b -> !b.getId().equals(bid.getId()))
            .forEach(b -> b.setStatus(BidStatus.REJECTED));
        bidRepository.saveAll(allBids);

        bidRepository.save(bid);
        leadRepository.save(lead);

        // Otomatik olarak ChatSession oluştur (varsa tekrar oluşturma)
        if (chatSessionRepository.findByLeadIdAndLawyerId(lead.getId(), bid.getLawyer().getId()).isEmpty()) {
            ChatSession chatSession = ChatSession.builder()
                    .lead(lead)
                    .client(client)
                    .lawyer(bid.getLawyer())
                    .active(true)
                    .build();
            chatSessionRepository.save(chatSession);
        }

        // Avukata kabul edildiğine dair bildirim gönder
        notificationService.sendNotification(
            bid.getLawyer().getId(),
            "Ön Görüşme Talebiniz Kabul Edildi!",
            "Müvekkil başvurunuzu onayladı. Sohbet odası üzerinden güvenli ön görüşmeye başlayabilirsiniz."
        );

        return bid;
    }
}
