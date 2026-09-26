package com.briefai.auth.repository;

import com.briefai.auth.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp>

    findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    List<EmailVerificationOtp> findAllByUserIdAndUsedFalse(Long userId);
}
