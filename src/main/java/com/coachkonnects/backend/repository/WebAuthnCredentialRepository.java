package com.coachkonnects.backend.repository;

import com.coachkonnects.backend.model.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findByEmail(String email);
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    Optional<WebAuthnCredential> findByUserHandle(String userHandle);
}
