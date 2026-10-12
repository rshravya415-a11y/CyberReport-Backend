package com.cyberreport.backend.complaint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cyberreport.backend.user.User;
import com.cyberreport.backend.user.UserRepository;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public ComplaintController(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            System.err.println("Could not create uploads directory: " + e.getMessage());
        }
    }

    // Submit a complaint
    @PostMapping
    public ResponseEntity<?> submitComplaint(
            @RequestBody Complaint complaint,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            Authentication authentication) {

        String email = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName();
        } else if (userEmail != null && !userEmail.isBlank()) {
            email = userEmail.trim();
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Please sign in before submitting a complaint."));
        }

        if (complaint.getTitle() == null || complaint.getTitle().isBlank()
                || complaint.getDescription() == null
                || complaint.getDescription().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Title and description are required."));
        }

        complaint.setUserEmail(email.toLowerCase());
        complaint.setStatus("Pending");

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null) {
            complaint.setUserId(user.getId());
            if (complaint.getUserName() == null || complaint.getUserName().isBlank()) {
                complaint.setUserName(user.getName());
            }
            if (complaint.getUserPhone() == null || complaint.getUserPhone().isBlank()) {
                complaint.setUserPhone(user.getPhone());
            }
        }

        if (complaint.getReferenceNumber() == null || complaint.getReferenceNumber().isBlank()) {
            int year = java.time.LocalDate.now().getYear();
            java.security.SecureRandom random = new java.security.SecureRandom();
            String candidate;
            do {
                int number = 100000 + random.nextInt(900000);
                candidate = "CR-" + year + "-" + number;
            } while (complaintRepository.existsByReferenceNumberIgnoreCase(candidate));
            complaint.setReferenceNumber(candidate);
        }

        Complaint saved = complaintRepository.save(complaint);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Complaint submitted successfully.",
            "referenceNumber", saved.getReferenceNumber(),
            "status", saved.getStatus(),
            "complaint", saved
        ));
    }

    // Upload evidence file with size and extension validation
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadEvidence(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            Authentication authentication) {

        if ((userEmail == null || userEmail.isBlank()) &&
                (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please sign in to upload evidence."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Uploaded file is empty."));
        }

        // 10 MB limit
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "File size exceeds the 10 MB limit."));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "evidence_" + System.currentTimeMillis();
        }

        String cleanName = Paths.get(originalName).getFileName().toString();
        String lowerName = cleanName.toLowerCase();

        boolean isAllowed = lowerName.endsWith(".png") || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg") || lowerName.endsWith(".pdf")
                || lowerName.endsWith(".txt") || lowerName.endsWith(".doc")
                || lowerName.endsWith(".docx");

        if (!isAllowed) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid file format. Allowed formats: PNG, JPG, JPEG, PDF, TXT, DOC, DOCX."
            ));
        }

        try {
            String storedFileName = UUID.randomUUID().toString() + "_" + cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetLocation = this.uploadDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(Map.of(
                    "message", "Evidence file uploaded successfully.",
                    "fileName", cleanName,
                    "filePath", storedFileName,
                    "fileSize", file.getSize()
            ));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not store file: " + ex.getMessage()));
        }
    }

    // Download/view evidence file (authenticated)
    @GetMapping("/evidence/{fileName}")
    public ResponseEntity<?> getEvidenceFile(
            @PathVariable String fileName,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            Authentication authentication) {

        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadDir)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Track a complaint by reference number
    @GetMapping("/track/{referenceNumber}")
    public ResponseEntity<?> trackComplaint(@PathVariable String referenceNumber) {
        return complaintRepository
            .findByReferenceNumberIgnoreCase(referenceNumber.trim())
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No complaint found with that reference number.")));
    }

    // Get complaints belonging to the signed-in user
    @GetMapping("/my")
    public ResponseEntity<?> myComplaints(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            Authentication authentication) {

        String email = null;
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName();
        } else if (userEmail != null && !userEmail.isBlank()) {
            email = userEmail.trim();
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Please sign in first."));
        }

        List<Complaint> complaints =
            complaintRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email);

        return ResponseEntity.ok(complaints);
    }

    // Get complaint by database ID with ownership check
    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaint(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            Authentication authentication) {

        Complaint complaint = complaintRepository.findById(id).orElse(null);
        if (complaint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Complaint not found."));
        }

        String email = null;
        boolean isAdmin = false;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName();
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        } else if (userEmail != null && !userEmail.isBlank()) {
            email = userEmail.trim();
            User u = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (u != null && "ADMIN".equalsIgnoreCase(u.getRole())) {
                isAdmin = true;
            }
        }

        if (isAdmin || (email != null && email.equalsIgnoreCase(complaint.getUserEmail()))) {
            return ResponseEntity.ok(complaint);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You are not authorized to view this complaint."));
    }
}