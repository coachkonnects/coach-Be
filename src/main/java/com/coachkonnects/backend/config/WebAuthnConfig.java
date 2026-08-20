package com.coachkonnects.backend.config;

import com.coachkonnects.backend.model.WebAuthnCredential;
import com.coachkonnects.backend.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class WebAuthnConfig {

    @Value("${app.domain:localhost}")
    private String appDomain;

    @Autowired
    private WebAuthnCredentialRepository credentialRepository;

    private static String toBase64Url(byte[] bytes) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] fromBase64Url(String s) {
        return java.util.Base64.getUrlDecoder().decode(s);
    }

    @Bean
    public RelyingParty relyingParty(CredentialRepository credentialRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
            .id(appDomain)
            .name("CoachKonnects Admin")
            .build();

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(credentialRepository)
            .allowOriginPort(true)
            .allowOriginSubdomain(true)
            .build();
    }

    @Bean
    public CredentialRepository credentialRepository() {
        return new CredentialRepository() {
            @Override
            public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
                List<WebAuthnCredential> creds = credentialRepository.findByEmail(username.toLowerCase().trim());
                return creds.stream()
                    .map(c -> PublicKeyCredentialDescriptor.builder()
                        .id(new ByteArray(fromBase64Url(c.getCredentialId())))
                        .build())
                    .collect(Collectors.toSet());
            }

            @Override
            public Optional<ByteArray> getUserHandleForUsername(String username) {
                List<WebAuthnCredential> creds = credentialRepository.findByEmail(username.toLowerCase().trim());
                if (creds.isEmpty()) return Optional.empty();
                return Optional.of(new ByteArray(fromBase64Url(creds.get(0).getUserHandle())));
            }

            @Override
            public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
                String encoded = toBase64Url(userHandle.getBytes());
                return credentialRepository.findByUserHandle(encoded)
                    .map(WebAuthnCredential::getEmail);
            }

            @Override
            public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
                String encodedCredId = toBase64Url(credentialId.getBytes());
                return credentialRepository.findByCredentialId(encodedCredId)
                    .map(c -> RegisteredCredential.builder()
                        .credentialId(credentialId)
                        .userHandle(userHandle)
                        .publicKeyCose(new ByteArray(fromBase64Url(c.getPublicKeyCose())))
                        .signatureCount(c.getSignatureCount())
                        .build());
            }

            @Override
            public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
                String encodedCredId = toBase64Url(credentialId.getBytes());
                return credentialRepository.findByCredentialId(encodedCredId)
                    .map(c -> RegisteredCredential.builder()
                        .credentialId(credentialId)
                        .userHandle(new ByteArray(fromBase64Url(c.getUserHandle())))
                        .publicKeyCose(new ByteArray(fromBase64Url(c.getPublicKeyCose())))
                        .signatureCount(c.getSignatureCount())
                        .build())
                    .stream().collect(Collectors.toSet());
            }
        };
    }
}
