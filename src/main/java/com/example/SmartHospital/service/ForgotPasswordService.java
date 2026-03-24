package com.example.SmartHospital.service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String emailHost;
    

    @Async
    public CompletableFuture<String> sendOtp(String email) { // CompletableFuture<String> help to return token asynchronously
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));
        
        String otp = generateOTP();
        String token = UUID.randomUUID().toString();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "utf-8");
            
            helper.setFrom(emailHost, "SmartHospital Support");
            helper.setTo(email);
            helper.setSubject("[SmartHospital] Your OTP for Password Reset");
            String htmlContent = getHTMLContent(otp, user.getFullName());
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            //Start the timer after mail is sent 
            HashMap<String, String> data = new HashMap<>();
            data.put("email", email);
            data.put("otp", otp);
            redisTemplate.opsForHash().putAll("otp:" + token, data);
            redisTemplate.expire("otp:" + token, Duration.ofMinutes(10)); // OTP
            return CompletableFuture.completedFuture(token);

        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    public CompletableFuture<String> verifyOtp(String token, String otp) {
        try {
            String value = redisTemplate.opsForValue().get("otp:" + token);

            if (value == null) {
                throw new IllegalArgumentException("OTP expired or invalid");
            }

            String savedOtp = value;

            if (!savedOtp.equals(otp)) {
                throw new IllegalArgumentException("Invalid OTP");
            }
            
            String email = redisTemplate.opsForValue().get("email:" + token);
            String resetToken = UUID.randomUUID().toString();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));

            // Start the timer for password reset token
            redisTemplate.opsForValue().set(
                "reset-email:" + resetToken,
                email,
                Duration.ofMinutes(15)
            );
            redisTemplate.opsForValue().set(
                "reset-token:" + resetToken,
                resetToken,
                Duration.ofMinutes(15)
            );
            redisTemplate.delete("email:" + token);
            redisTemplate.delete("otp:" + token);
            return CompletableFuture.completedFuture(resetToken); // Return a password reset token
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify OTP", e);
        }
    }

    public void resetPassword(String token, String newPassword) {
        try {
            // Validate password strength
            if (newPassword.length() < 8 ||
                !newPassword.matches(".*[A-Z].*") ||
                !newPassword.matches(".*[a-z].*") ||
                !newPassword.matches(".*\\d.*") ||
                !newPassword.matches(".*[!@#$%^&*()].*")) {
                throw new IllegalArgumentException("Password is not strong enough");
            }
            String email = redisTemplate.opsForValue().get("reset-email:" + token);
            if (email == null) {
                throw new IllegalArgumentException("Invalid or expired password reset token");
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found"));
            user.setHashedPassword(passwordEncoder.encode(newPassword)); 
            userRepository.save(user);
            redisTemplate.delete("reset-email:" + token); // Invalidate the reset token after use
            redisTemplate.delete("reset-token:" + token);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset password", e);
        }
        
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); 
        return String.valueOf(otp);
    }

    private String getHTMLContent(String otp,  String fullName) {
        ClassPathResource resource = new ClassPathResource("mailTemplate/otpMail.html");
        try (var inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes());
            return content.replace("{{OTP}}", otp)
                          .replace("{{name}}", fullName);
        } catch(Exception e){
            throw new RuntimeException("Fail to find HTMLTemplate", e);
        }
    }


}
