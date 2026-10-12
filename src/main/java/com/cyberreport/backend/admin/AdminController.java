
package com.cyberreport.backend.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cyberreport.backend.complaint.Complaint;
import com.cyberreport.backend.complaint.ComplaintRepository;
import com.cyberreport.backend.user.User;
import com.cyberreport.backend.user.UserRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public AdminController(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    private void enrichComplaintWithUserDetails(Complaint c) {
        if (c == null) return;
        User user = null;
        if (c.getUserId() != null) {
            user = userRepository.findById(c.getUserId()).orElse(null);
        }
        if (user == null && c.getUserEmail() != null && !c.getUserEmail().isBlank()) {
            user = userRepository.findByEmailIgnoreCase(c.getUserEmail().trim()).orElse(null);
        }

        if (user != null) {
            if (c.getUserName() == null || c.getUserName().isBlank()) {
                c.setUserName(user.getName());
            }
            if (c.getUserPhone() == null || c.getUserPhone().isBlank()) {
                c.setUserPhone(user.getPhone());
            }
            if (c.getUserEmail() == null || c.getUserEmail().isBlank()) {
                c.setUserEmail(user.getEmail());
            }
        }
    }

    // Get all complaints for the admin dashboard
    @GetMapping("/complaints")
    public List<Complaint> getAllComplaints() {
        List<Complaint> list = complaintRepository.findAllByOrderByCreatedAtDesc();
        for (Complaint c : list) {
            enrichComplaintWithUserDetails(c);
        }
        return list;
    }

    // Get dashboard summary counts
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        List<Complaint> complaints = complaintRepository.findAll();

        long total = complaints.size();
        long pending = complaints.stream()
                .filter(c -> "Pending".equalsIgnoreCase(c.getStatus()))
                .count();
        long investigating = complaints.stream()
                .filter(c -> "Under Investigation".equalsIgnoreCase(c.getStatus()))
                .count();
        long resolved = complaints.stream()
                .filter(c -> "Resolved".equalsIgnoreCase(c.getStatus()))
                .count();
        long rejected = complaints.stream()
                .filter(c -> "Rejected".equalsIgnoreCase(c.getStatus()))
                .count();

        return Map.of(
                "totalComplaints", total,
                "pendingComplaints", pending,
                "pending", pending,
                "underInvestigation", investigating,
                "resolvedComplaints", resolved,
                "resolved", resolved,
                "rejectedComplaints", rejected,
                "rejected", rejected
        );
    }

    // Get single complaint details for admin review
    @GetMapping("/complaints/{id}")
    public Complaint getComplaintById(@PathVariable Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));
        enrichComplaintWithUserDetails(complaint);
        return complaint;
    }

    // Update a complaint's status and optional remarks
    @PatchMapping("/complaints/{id}/status")
    public Complaint updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));

        String status = request.get("status");
        String remarks = request.get("adminRemarks");
        if (remarks == null) {
            remarks = request.get("remarks");
        }

        if (status != null) {
            if (!List.of(
                    "Pending",
                    "Under Investigation",
                    "Resolved",
                    "Rejected"
            ).contains(status)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid complaint status");
            }
            complaint.setStatus(status);
        }

        if (remarks != null) {
            complaint.setAdminRemarks(remarks);
        }

        return complaintRepository.save(complaint);
    }

    // Update investigation remarks specifically
    @PatchMapping("/complaints/{id}/remarks")
    public Complaint updateRemarks(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));

        String remarks = request.get("adminRemarks");
        if (remarks == null) {
            remarks = request.get("remarks");
        }

        if (remarks != null) {
            complaint.setAdminRemarks(remarks);
        }

        return complaintRepository.save(complaint);
    }
}
