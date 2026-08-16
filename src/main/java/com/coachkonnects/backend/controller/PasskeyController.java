package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/passkeys")
public class PasskeyController {

    @Autowired
    private UserRepository userRepository;

    private Map<String, String> registeredCredentials = new HashMap<>();

    @Value("${app.domain:localhost}")
    private String appDomain;

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        Map<String, Object> options = new HashMap<>();

        Map<String, Object> rp = new HashMap<>();
        rp.put("name", "CoachKonnects");
        rp.put("id", appDomain);

        Map<String, Object> user = new HashMap<>();
        user.put("id", Base64.getUrlEncoder().withoutPadding().encodeToString("admin123".getBytes()));
        user.put("name", "admin@coachkonnects.com");
        user.put("displayName", "Admin");

        Map<String, Object> pubKeyCredParams = new HashMap<>();
        pubKeyCredParams.put("type", "public-key");
        pubKeyCredParams.put("alg", -7); // ES256

        Map<String, Object> pubKeyCredParams2 = new HashMap<>();
        pubKeyCredParams2.put("type", "public-key");
        pubKeyCredParams2.put("alg", -257); // RS256

        Map<String, Object> authenticatorSelection = new HashMap<>();
        authenticatorSelection.put("userVerification", "preferred");
        authenticatorSelection.put("residentKey", "required");
        authenticatorSelection.put("requireResidentKey", true);

        byte[] challenge = new byte[32];
        new Random().nextBytes(challenge);

        options.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge));
        options.put("rp", rp);
        options.put("user", user);
        options.put("pubKeyCredParams", Arrays.asList(pubKeyCredParams, pubKeyCredParams2));
        options.put("timeout", 60000);
        options.put("attestation", "none");
        options.put("authenticatorSelection", authenticatorSelection);

        return ResponseEntity.ok(options);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<?> finishRegistration(@RequestBody Map<String, Object> request) {
        System.out.println("Passkey registration received: " + request);

        registeredCredentials.put("admin", "registered");

        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @GetMapping("/login/start")
    public ResponseEntity<?> startLogin() {
        Map<String, Object> options = new HashMap<>();

        byte[] challenge = new byte[32];
        new Random().nextBytes(challenge);

        options.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge));
        options.put("timeout", 60000);
        options.put("userVerification", "preferred");
        options.put("rpId", appDomain);

        return ResponseEntity.ok(options);
    }

    @Autowired
    private com.coachkonnects.backend.util.JwtUtil jwtUtil;

    @PostMapping("/login/finish")
    public ResponseEntity<?> finishLogin(@RequestBody Map<String, Object> request) {
        System.out.println("Passkey login received: " + request);
        
        // Passkey login is temporarily disabled due to lack of cryptographic signature verification.
        // Users must use OTP login.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message", "Passkey login is temporarily disabled for security reasons. Please use OTP login."));
    }
}
