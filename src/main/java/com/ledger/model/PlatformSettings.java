package com.ledger.model;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_settings")
public class PlatformSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String platformName = "Ledger LMS";
    private String supportEmail = "support@lms.edu";
    private boolean allowSelfRegistration = true;
    private boolean requireEnrollmentApproval = false;
    /** When false, student portal pages and login are blocked. */
    private boolean studentPortalEnabled = true;

    public PlatformSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
    public boolean isAllowSelfRegistration() { return allowSelfRegistration; }
    public void setAllowSelfRegistration(boolean allowSelfRegistration) { this.allowSelfRegistration = allowSelfRegistration; }
    public boolean isRequireEnrollmentApproval() { return requireEnrollmentApproval; }
    public void setRequireEnrollmentApproval(boolean requireEnrollmentApproval) { this.requireEnrollmentApproval = requireEnrollmentApproval; }
    public boolean isStudentPortalEnabled() { return studentPortalEnabled; }
    public void setStudentPortalEnabled(boolean studentPortalEnabled) { this.studentPortalEnabled = studentPortalEnabled; }
}
