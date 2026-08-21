package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.WebAuthnCredential;
import com.coachkonnects.backend.repository.WebAuthnCredentialRepository;
import com.coachkonnects.backend.repository.UserRepository;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/passkeys")
public class PasskeyController {

    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private WebAuthnCredentialRepository credentialRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    // Temporary storage for challenges
    private final Map<String, RegistrationRequest> registrationRequests = new ConcurrentHashMap<>();
    private final Map<String, AssertionRequest> assertionRequests = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestBody Map<String, String> request) {
        try {
            // Usually this requires being logged in. We expect email to be provided for now in request.
            String email = request.get("email");
            if (email == null) return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));

            byte[] userHandle;
            java.util.List<com.coachkonnects.backend.model.WebAuthnCredential> existingCreds = credentialRepository.findByEmail(email);
            if (!existingCreds.isEmpty()) {
                userHandle = java.util.Base64.getUrlDecoder().decode(existingCreds.get(0).getUserHandle());
            } else {
                userHandle = new byte[32];
                new SecureRandom().nextBytes(userHandle);
            }

            StartRegistrationOptions startOptions = StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                    .name(email)
                    .displayName("Admin")
                    .id(new ByteArray(userHandle))
                    .build())
                .build();

            PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(startOptions);
            
            // Store request by user handle base64
            RegistrationRequest registrationRequest = new RegistrationRequest(email, userHandle, options);
            String requestKey = options.getChallenge().getBase64Url();
            registrationRequests.put(requestKey, registrationRequest);

            return ResponseEntity.ok(options.toCredentialsCreateJson());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register/finish")
    public ResponseEntity<?> finishRegistration(@RequestBody String jsonResponse) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonResponse);
            String email = rootNode.get("email").asText().toLowerCase().trim();
            com.fasterxml.jackson.databind.JsonNode responseNode = rootNode.get("response");
            String responseJson = responseNode != null ? responseNode.toString() : rootNode.toString();

            // Parse the attestation response first so we can extract the challenge from clientDataJSON
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = 
                PublicKeyCredential.parseRegistrationResponseJson(responseJson);

            // Extract challenge from clientDataJSON — use it as the lookup key
            byte[] clientDataBytes = pkc.getResponse().getClientDataJSON().getBytes();
            com.fasterxml.jackson.databind.JsonNode clientData = mapper.readTree(clientDataBytes);
            String challengeFromResponse = clientData.get("challenge").asText();

            // Normalize: remove base64 padding and convert + -> - and / -> _
            String normalizedChallenge = challengeFromResponse.replace("+", "-").replace("/", "_").replace("=", "");

            // Find the request whose challenge matches what the browser returned
            RegistrationRequest req = null;
            for (Map.Entry<String, RegistrationRequest> entry : registrationRequests.entrySet()) {
                String storedKey = entry.getKey().replace("=", "");
                if (storedKey.equals(normalizedChallenge)) {
                    req = entry.getValue();
                    break;
                }
            }
            if (req == null) {
                throw new RuntimeException("Challenge not found. Please try registering again.");
            }
            if (!req.email.equalsIgnoreCase(email)) {
                throw new RuntimeException("Email mismatch for this passkey session.");
            }

            FinishRegistrationOptions finishOptions = FinishRegistrationOptions.builder()
                .request(req.options)
                .response(pkc)
                .build();

            RegistrationResult result = relyingParty.finishRegistration(finishOptions);

            String b64UserHandle = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(req.userHandle);
            String b64CredId = result.getKeyId().getId().getBase64Url();
            String b64Cose = result.getPublicKeyCose().getBase64Url();

            WebAuthnCredential cred = new WebAuthnCredential();
            cred.setEmail(email);
            cred.setUserHandle(b64UserHandle);
            cred.setCredentialId(b64CredId);
            cred.setPublicKeyCose(b64Cose);
            cred.setSignatureCount(result.getSignatureCount());
            
            credentialRepository.save(cred);
            registrationRequests.remove(req.options.getChallenge().getBase64Url());

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login/start")
    public ResponseEntity<?> startLogin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email").toLowerCase().trim();
            if (email == null || email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));

            StartAssertionOptions options = StartAssertionOptions.builder()
                .username(email)
                .build();

            AssertionRequest assertionRequest = relyingParty.startAssertion(options);
            
            String requestKey = assertionRequest.getPublicKeyCredentialRequestOptions().getChallenge().getBase64Url();
            assertionRequests.put(requestKey, assertionRequest);

            return ResponseEntity.ok(assertionRequest.getPublicKeyCredentialRequestOptions().toCredentialsGetJson());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login/finish")
    public ResponseEntity<?> finishLogin(@RequestBody String jsonResponse) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonResponse);
            String email = rootNode.get("email").asText().toLowerCase().trim();
            com.fasterxml.jackson.databind.JsonNode responseNode2 = rootNode.get("response");
            String responseJson = responseNode2 != null ? responseNode2.toString() : rootNode.toString();

            // Parse assertion first to extract challenge from clientDataJSON
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc = 
                PublicKeyCredential.parseAssertionResponseJson(responseJson);

            byte[] clientDataBytes = pkc.getResponse().getClientDataJSON().getBytes();
            com.fasterxml.jackson.databind.JsonNode clientData = mapper.readTree(clientDataBytes);
            String challengeFromResponse = clientData.get("challenge").asText();
            String normalizedChallenge = challengeFromResponse.replace("+", "-").replace("/", "_").replace("=", "");

            AssertionRequest req = null;
            for (Map.Entry<String, AssertionRequest> entry : assertionRequests.entrySet()) {
                String storedKey = entry.getKey().replace("=", "");
                if (storedKey.equals(normalizedChallenge)) {
                    req = entry.getValue();
                    break;
                }
            }
            if (req == null) {
                throw new RuntimeException("Challenge not found. Please try logging in again.");
            }

            FinishAssertionOptions finishOptions = FinishAssertionOptions.builder()
                .request(req)
                .response(pkc)
                .build();

            AssertionResult result = relyingParty.finishAssertion(finishOptions);

            if (result.isSuccess()) {
                // Update signature count
                Optional<WebAuthnCredential> credOpt = credentialRepository.findByCredentialId(result.getCredential().getCredentialId().getBase64Url());
                if (credOpt.isPresent()) {
                    WebAuthnCredential cred = credOpt.get();
                    cred.setSignatureCount(result.getSignatureCount());
                    credentialRepository.save(cred);
                }

                User user = userRepository.findByEmail(email).orElse(null);
                String role = (user != null && user.getRole() != null) ? user.getRole() : "STUDENT";
                String token = jwtUtil.generateToken(email, role);
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "token", token, "email", email, "role", role));
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid passkey signature"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    private static class RegistrationRequest {
        public String email;
        public byte[] userHandle;
        public PublicKeyCredentialCreationOptions options;

        public RegistrationRequest(String email, byte[] userHandle, PublicKeyCredentialCreationOptions options) {
            this.email = email;
            this.userHandle = userHandle;
            this.options = options;
        }
    }
}
