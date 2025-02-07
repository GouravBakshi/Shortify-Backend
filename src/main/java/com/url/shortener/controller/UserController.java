package com.url.shortener.controller;

import com.url.shortener.dtos.ChangePassword;
import com.url.shortener.dtos.MailBody;
import com.url.shortener.dtos.UserDto;
import com.url.shortener.models.ForgotPassword;
import com.url.shortener.models.User;
import com.url.shortener.repository.ForgotPasswordRepository;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.service.EmailService;
import com.url.shortener.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private UserService userService;
    private UserRepository userRepository;
    private ForgotPasswordRepository forgotPasswordRepository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;

    @GetMapping("/details")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserDto> UserDetails(Principal principal){
        User user = userService.findByUsername(principal.getName());
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());

        return ResponseEntity.ok(userDto);
    }


    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteUser(Principal principal)
    {
        User user = userService.findByUsername(principal.getName());

        if(user == null)
        {
            return ResponseEntity.status(404).body("User not found");
        }

        userService.deleteUser(user.getId());
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/resetPassword/verifyMail")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> verifyEmail(Principal principal) {

        User user1 = userService.findByUsername(principal.getName());

        User user = userRepository.findByEmail(user1.getEmail())
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


        // Create ForgotPassword entity and associate it with the user
        ForgotPassword fp = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 3 * 60 * 1000)) // OTP expires in 70 seconds
                .user(user)
                .verified(false)
                .build();

        String textContent = "Shortify - Password Reset Request\n\n" +
                "Hello " + user.getUsername() + ",\n\n" +
                "We received a request to reset your password. Please use the following One-Time Password (OTP) to complete the process:\n\n" +
                "OTP: " + otp + "\n\n" +
                "This OTP is valid for the next 3 minutes. After that, it will expire and you will need to request a new OTP.\n\n" +
                "If you did not request a password reset, please ignore this email, and your account will remain secure.\n\n" +
                "Do not reply to this email. This is an automated message.\n\n" +
                "Thank you for using Shortify!";


        // Prepare mail body with OTP
        MailBody mailBody = MailBody.builder()
                .to(user.getEmail())
                .subject("OTP for Reset Password request")
                .text(textContent)
                .html(htmlContent)
                .build();

        // Send OTP email
        emailService.sendHtmlMessage(mailBody);

        // Save ForgotPassword entity
        forgotPasswordRepository.save(fp);

        return ResponseEntity.ok("Email sent for verification!");
    }

    @PostMapping("/resetPassword/verify-otp/{otp}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, Principal principal) {
        // Find the user by email
        User user1 = userService.findByUsername(principal.getName());

        User user = userRepository.findByEmail(user1.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Email is not found!"));

        // Retrieve the ForgotPassword entry for the user
        ForgotPassword fp = forgotPasswordRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Invalid OTP for email: " + user.getEmail()));

        // Check if OTP is expired
        if (fp.getExpirationTime().before(Date.from(Instant.now()))) {
            forgotPasswordRepository.deleteByFpid(fp.getFpid());
            return new ResponseEntity<>("OTP has expired and has been deleted.", HttpStatus.BAD_REQUEST);
        }

        fp.setVerified(true);
        forgotPasswordRepository.save(fp);
        return ResponseEntity.ok("OTP Verified!");
    }


    @PostMapping("/resetPassword/changePassword")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> changePasswordHandler(@RequestBody ChangePassword changePassword,
                                                        Principal principal) {

        User user1 = userService.findByUsername(principal.getName());

        User user = userRepository.findByEmail(user1.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Email is not found!"));

        // Retrieve the ForgotPassword entry for the user
        ForgotPassword fp = forgotPasswordRepository.findByUser(user);
        if (fp == null) {
            throw new RuntimeException("No OTP request found for email: " + user.getEmail());
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
        userRepository.updatePassword(user.getEmail(), encodedPassword);

        forgotPasswordRepository.deleteByFpid(fp.getFpid());

        return ResponseEntity.ok("Password has been changed!");
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999); // Generate a random 6-digit OTP
    }

}
