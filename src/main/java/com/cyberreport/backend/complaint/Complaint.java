package com.cyberreport.backend.complaint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String referenceNumber;

    private String title;
    private String crimeType;
    private String platform;
    private LocalDate incidentDate;

    @Column(length = 5000)
    private String description;

    private String suspectDetails;
    private String status = "Pending";
    private String userEmail;
    private Long userId;
    private String userName;
    private String userPhone;

    @Column(length = 5000)
    private String adminRemarks;

    private String evidenceFileName;
    private String evidenceFilePath;

    @Column(length = 2000)
    private String evidence;

    private LocalDateTime createdAt;

    @PrePersist
    public void beforeSave() {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            int year = java.time.LocalDate.now().getYear();
            int number = 100000 + (int) (Math.random() * 900000);
            referenceNumber = "CR-" + year + "-" + number;
        }
        if (status == null || status.isBlank()) {
            status = "Pending";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getTitle() { return title; }
    public String getCrimeType() { return crimeType; }
    public String getPlatform() { return platform; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public String getDescription() { return description; }
    public String getSuspectDetails() { return suspectDetails; }
    public String getStatus() { return status; }
    public String getUserEmail() { return userEmail; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserPhone() { return userPhone; }
    public String getAdminRemarks() { return adminRemarks; }
    public String getEvidenceFileName() { return evidenceFileName; }
    public String getEvidenceFilePath() { return evidenceFilePath; }
    public String getEvidence() { return evidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public void setTitle(String title) { this.title = title; }
    public void setCrimeType(String crimeType) { this.crimeType = crimeType; }
    public void setPlatform(String platform) { this.platform = platform; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public void setDescription(String description) { this.description = description; }
    public void setSuspectDetails(String suspectDetails) { this.suspectDetails = suspectDetails; }
    public void setStatus(String status) { this.status = status; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }
    public void setEvidenceFileName(String evidenceFileName) { this.evidenceFileName = evidenceFileName; }
    public void setEvidenceFilePath(String evidenceFilePath) { this.evidenceFilePath = evidenceFilePath; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}