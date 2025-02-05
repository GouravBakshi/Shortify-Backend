package com.url.shortener.repository;

import com.url.shortener.models.ForgotPassword;
import com.url.shortener.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, Integer> {

    @Query("select fp from ForgotPassword fp where fp.otp = ?1 and fp.user = ?2")
    Optional<ForgotPassword> findByOtpAndUser(Integer otp, User user);

    ForgotPassword findByUser(User user);

    @Modifying
    @Query("DELETE FROM ForgotPassword fp WHERE fp.fpid = ?1")
    @Transactional
    void deleteByFpid(Integer fpid);


}
