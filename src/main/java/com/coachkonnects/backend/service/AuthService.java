package com.coachkonnects.backend.service;

import com.coachkonnects.backend.model.OneTimePassword;
import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.repository.OneTimePasswordRepository;
import com.coachkonnects.backend.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OneTimePasswordRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    private static final List<String> ALLOWED_ADMINS = List.of(
            "kavita.ganatra1@gmail.com",
            "coachkonnects@gmail.com",
            "sameer6306@gmail.com",
            "sameer6306khan@gmail.com");

    public void requestOtp(String email) {
        if (!ALLOWED_ADMINS.contains(email)) {
            throw new RuntimeException("Unauthorized: This email does not have Admin privileges.");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setRole("ADMIN");
            return userRepository.save(newUser);
        });

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        OneTimePassword otp = new OneTimePassword();
        otp.setUserId(user.getId());
        otp.setCode(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        otpRepository.save(otp);

        sendOtpEmail(email, otpCode);
    }

    private void sendOtpEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("coachkonnects@gmail.com", "CoachKonnects Security");
            helper.setTo(to);
            helper.setSubject("Your CoachKonnects Admin Login Code");

            String htmlMsg = "<div style=\"font-family: Arial; padding: 20px; border: 1px solid #e0e0e0;\">"
                    + "<h2 style=\"color: #f26b21;\">CoachKonnects Admin Access</h2>"
                    + "<p>Your One-Time Password (OTP) is:</p>"
                    + "<div style=\"background-color: #fff2e8; padding: 15px; font-size: 24px; font-weight: bold;\">"
                    + code + "</div>"
                    + "<p>This code expires in 15 minutes.</p></div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    public User verifyOtp(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email."));

        List<OneTimePassword> otps = otpRepository.findByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());

        for (OneTimePassword otp : otps) {
            if (otp.getCode().equals(code) && otp.getExpiresAt().isAfter(LocalDateTime.now())) {
                otp.setUsed(true);
                otpRepository.save(otp);
                return user;
            }
        }

        throw new RuntimeException("Invalid or expired code.");
    }
}
