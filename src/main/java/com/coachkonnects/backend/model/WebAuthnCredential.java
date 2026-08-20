package com.coachkonnects.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // Stored as base64url string to avoid JPA bytea comparison issues
    @Column(nullable = false, columnDefinition = "text")
    private String userHandle;

    @Column(nullable = false, columnDefinition = "text", unique = true)
    private String credentialId;

    @Column(nullable = false, columnDefinition = "text")
    private String publicKeyCose;

    @Column(nullable = false)
    private long signatureCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserHandle() { return userHandle; }
    public void setUserHandle(String userHandle) { this.userHandle = userHandle; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getPublicKeyCose() { return publicKeyCose; }
    public void setPublicKeyCose(String publicKeyCose) { this.publicKeyCose = publicKeyCose; }

    public long getSignatureCount() { return signatureCount; }
    public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }
}
