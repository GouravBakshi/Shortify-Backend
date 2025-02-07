package com.url.shortener.controller;

import com.url.shortener.dtos.ChangePassword;
import com.url.shortener.dtos.MailBody;
import com.url.shortener.models.ForgotPassword;
import com.url.shortener.models.User;
import com.url.shortener.repository.ForgotPasswordRepository;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.service.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RestController
@RequestMapping("/forgotPassword")
@AllArgsConstructor
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/verifyMail/{email}")
    public ResponseEntity<String> verifyEmail(@PathVariable String email) {
        // Check if the user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email is not found!"));

        // Check if an OTP already exists for this email
        ForgotPassword existingOtp = forgotPasswordRepository.findByUser(user);
        if (existingOtp != null) {
            if(existingOtp.getExpirationTime().before(Date.from(Instant.now())))
            {
                forgotPasswordRepository.deleteByFpid(existingOtp.getFpid());
            }
            else{
                return new ResponseEntity<>("OTP is already sent! ", HttpStatus.BAD_REQUEST);
            }
        }

        // Generate OTP
        int otp = otpGenerator();

        String htmlContent = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Password Reset OTP</title>" +
                "  <style>" +
                "    body { font-family: Arial, sans-serif; background-color: #f4f4f9; color: #333; padding: 20px; margin: 0; }" +
                "    .container { background-color: #fff; border-radius: 8px; padding: 20px; max-width: 600px; margin: 20px auto; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }" +
                "    h2 { color: #4CAF50; }" +
                "    .otp-box { background-color: #e8f5e9; padding: 15px; font-size: 18px; font-weight: bold; border-radius: 5px; margin: 20px 0; text-align: center; }" +
                "    .footer { font-size: 14px; color: #888; text-align: center; margin-top: 20px; }" +
                "    .footer a { color: #4CAF50; text-decoration: none; }" +
                "    .header { background-color: #3B82F6; color: white; padding: 10px; text-align: center; font-size: 24px; font-weight: bold; border-radius: 8px 8px 0 0; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"header\">Shortify</div>" +
                "  <div class=\"container\">" +
                "    <h2>Password Reset Request</h2>" +
                "    <p>Hello <strong>" + user.getUsername() + "</strong>,</p>" +
                "    <p>We received a request to reset your password. Please use the following One-Time Password (OTP) to complete the process:</p>" +
                "    <div class=\"otp-box\">" + otp + "</div>" +
                "    <p>This OTP is valid for the next 3 minutes. After that, it will expire and you will need to request a new OTP.</p>" +
                "    <p>If you did not request a password reset, please ignore this email, and your account will remain secure.</p>" +
                "    <p><strong>Do not reply to this email.</strong> This is an automated message.</p>" +
                "    <div class=\"footer\">" +
                "      <p>Thank you for using Shortify!</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";

        String textContent = "Shortify - Forgot Password Request\n\n" +
                "Hello " + user.getUsername() + ",\n\n" +
                "We received a request to reset your password. Please use the following One-Time Password (OTP) to complete the process:\n\n" +
                "OTP: " + otp + "\n\n" +
                "This OTP is valid for the next 3 minutes. After that, it will expire and you will need to request a new OTP.\n\n" +
                "If you did not request a password reset, please ignore this email, and your account will remain secure.\n\n" +
                "Do not reply to this email. This is an automated message.\n\n" +
                "Thank you for using Shortify!";




        // Create ForgotPassword entity and associate it with the user
        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 3 * 60 * 1000)) // OTP expires in 70 seconds
                .user(user)
                .verified(false)
                .build();

        // Prepare mail body with OTP
        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("OTP for Forgot Password request")
                .text(textContent)
                .html(htmlContent)
                .build();

        // Send OTP email
        emailService.sendHtmlMessage(mailBody);

        // Save ForgotPassword entity
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("Email sent for verification!");
    }

    @PostMapping("/verify-otp/{otp}/{email}")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        // Find the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Please provide a valid email!"));

        // Retrieve the ForgotPassword entry for the user
        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + email));

        // Check if OTP is expired
        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByFpid(fp.getFpid());
            return new ResponseEntity<>("OTP has expired and has been deleted.", HttpStatus.BAD_REQUEST);
        }

        fp.setVerified(true);
        forgotPasswordRepository.save(fp);
        return ResponseEntity.ok("OTP Verified!");
    }

    @PostMapping("/changePassword/{email}")
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword,
                                                        @PathVariable String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email does not exist!"));

        // Retrieve the ForgotPassword entry for the user
        ForgotPassword fp = forgotPasswordRepository.findByUser(user);
        if (fp == null) {
            throw new RuntimeException("No OTP request found for email: " + email);
        }

        // Check if the OTP has been verified
        if (!fp.isVerified()) {
            return new ResponseEntity<>("Please verify the OTP before changing the password.", HttpStatus.FORBIDDEN);
        }

        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByFpid(fp.getFpid());
            return new ResponseEntity<>("OTP has been expired.", HttpStatus.BAD_REQUEST);
        }

        // Check if password and repeatPassword match
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Passwords do not match!", HttpStatus.BAD_REQUEST);
        }

        // Encode the password and update it in the database
        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);

        forgotPasswordRepository.deleteByFpid(fp.getFpid());

        return ResponseEntity.ok("Password has been changed!");
    }

    // Helper method to generate OTP
    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999); // Generate a random 6-digit OTP
    }
}
