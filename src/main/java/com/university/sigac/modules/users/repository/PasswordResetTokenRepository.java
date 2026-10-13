package com.university.sigac.modules.users.repository;

import com.university.sigac.modules.users.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);
}
