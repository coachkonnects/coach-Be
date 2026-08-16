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
import java.security.SecureRandom;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OneTimePasswordRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void requestOtp(String email, String intendedRole) {
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isFirstUser = userRepository.count() == 0;
        
        if (user == null) {
            user = new User();
            user.setEmail(email);
            // The very first person to register becomes Admin, everyone else is a Student by default
            user.setRole(isFirstUser ? "ADMIN" : "STUDENT");
            user = userRepository.save(user);
        }

        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        OneTimePassword otp = new OneTimePassword();
        otp.setUserId(user.getId());
        otp.setCode(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        otpRepository.save(otp);

        String emailRole = intendedRole != null ? intendedRole : user.getRole();
        sendOtpEmail(email, otpCode, emailRole);
    }

    private void sendOtpEmail(String to, String code, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom("coachkonnects@gmail.com", "CoachKonnects Security");
            helper.setTo(to);
            
            String title = role.equalsIgnoreCase("ADMIN") ? "Admin Access" : 
                           role.equalsIgnoreCase("COACH") ? "Coach Access" : "Student Access";
            
            helper.setSubject("Your CoachKonnects " + title + " Code");

            String htmlMsg = "<div style=\"font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px; text-align: center; background-color: #ffffff;\">"
                    + "<div style=\"padding: 40px; border-radius: 16px; background: #ffffff; border: 1px solid #eaeaea; box-shadow: 0 4px 24px rgba(0,0,0,0.04);\">"
                    + "<h2 style=\"color: #1a1a1a; margin-top: 0; font-size: 24px; font-weight: 700;\">CoachKonnects " + title + "</h2>"
                    + "<p style=\"color: #666666; font-size: 16px; line-height: 1.5; margin-bottom: 24px;\">Please use the verification code below to securely log into your account.</p>"
                    + "<div style=\"background-color: #f26b21; color: #ffffff; padding: 20px 40px; border-radius: 12px; font-size: 32px; font-weight: 800; letter-spacing: 4px; display: inline-block; margin-bottom: 24px; box-shadow: 0 4px 12px rgba(242, 107, 33, 0.2);\">"
                    + code + "</div>"
                    + "<p style=\"color: #888888; font-size: 14px; margin-bottom: 0;\">This code expires in <strong>15 minutes</strong>.</p>"
                    + "</div>"
                    + "<p style=\"color: #bbbbbb; font-size: 12px; margin-top: 24px;\">If you didn't request this code, you can safely ignore this email.</p>"
                    + "</div>";

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
