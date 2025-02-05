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
import org.springframework.web.client.HttpClientErrorException;

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

        // Prepare mail body with OTP
        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("OTP for Forgot Password request")
                .text("This is the OTP for your Forgot Password Request: " + otp)
                .build();

        // Create ForgotPassword entity and associate it with the user
        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 3 * 60 * 1000)) // OTP expires in 70 seconds
                .user(user)
                .verified(false)
                .build();

        // Send OTP email
        emailService.sendSimpleMessage(mailBody);

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
