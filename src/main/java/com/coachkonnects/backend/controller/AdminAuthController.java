package com.coachkonnects.backend.controller;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.samstevens.totp.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.coachkonnects.backend.util.JwtUtil jwtUtil;

    // Simple in-memory map for interim tokens between login and 2FA
    private Map<String, Long> interimTokens = new HashMap<>();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Basic check (assuming plaintext/hashed match for demo purposes)
            if (user.getPasswordHash().equals(password) && "ADMIN".equals(user.getRole())) {
                String interimToken = UUID.randomUUID().toString();
                interimTokens.put(interimToken, user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("status", "2FA_REQUIRED");
                response.put("token", interimToken);
                response.put("isSetupRequired", user.getTotpSecret() == null);
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials or not an admin"));
    }

    @GetMapping("/2fa/setup")
    public ResponseEntity<?> setup2fa(@RequestParam("token") String interimToken) throws QrGenerationException {
        Long userId = interimTokens.get(interimToken);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userRepository.findById(userId).orElseThrow();
        if (user.getTotpSecret() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "2FA already setup"));
        }

        // Generate Secret
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();
        user.setTotpSecret(secret);
        userRepository.save(user);

        // Generate QR Code
        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("CoachKonnects")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = generator.generate(data);
        String mimeType = generator.getImageMimeType();
        String dataUri = Utils.getDataUriForImage(imageData, mimeType);

        Map<String, String> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCode", dataUri);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> request) {
        String interimToken = request.get("token");
        String code = request.get("code");

        Long userId = interimTokens.get(interimToken);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userRepository.findById(userId).orElseThrow();
        
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

        if (verifier.isValidCode(user.getTotpSecret(), code)) {
            // Successful 2FA, issue final token
            String finalToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
            interimTokens.remove(interimToken);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "token", finalToken));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid 2FA code"));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.getPasswordHash().equals(oldPassword) || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid current password."));
        }

        user.setPasswordHash(newPassword);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
