package club.escobar.repository;

import club.escobar.entity.LoginOtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginOtpTokenRepository extends JpaRepository<LoginOtpToken, Long> {

    Optional<LoginOtpToken> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<LoginOtpToken> findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
