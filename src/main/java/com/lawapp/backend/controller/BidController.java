package com.lawapp.backend.controller;

import com.lawapp.backend.model.Bid;
import com.lawapp.backend.service.BidService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/place")
    public ResponseEntity<?> placeBid(@RequestBody BidRequest bidRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Bid bid = bidService.placeBid(bidRequest.getLeadId(), email, bidRequest.getMessage());
            return ResponseEntity.ok(bid);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<Bid>> getBidsByLead(@PathVariable Long leadId) {
        // Güvenlik kontrolü: Sadece ilanı açan müvekkil kendi ilanının tekliflerini görebilmeli.
        // Bu aşamada avukatın iletişim bilgileri de (telefon vb.) müvekkile açık hale gelir.
        return ResponseEntity.ok(bidService.getBidsForLead(leadId));
    }

    @PostMapping("/{bidId}/accept")
    public ResponseEntity<?> acceptBid(@PathVariable Long bidId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Bid acceptedBid = bidService.acceptBid(bidId, email);
            return ResponseEntity.ok(acceptedBid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class BidRequest {
        private Long leadId;
        private String message;
    }
}
