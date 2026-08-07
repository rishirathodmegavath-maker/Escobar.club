package club.escobar.repository;

import club.escobar.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, Instant now);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.id = :id and r.user.id = :userId and r.revoked = false")
    int revokeForUser(@Param("id") Long id, @Param("userId") Long userId);
}
