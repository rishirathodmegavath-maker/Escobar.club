package club.escobar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

// One row per redeemed 2FA login-challenge JWT (keyed by its jti), so the same stateless token
// can't be replayed to mint additional sessions before it naturally expires.
@Entity
@Table(name = "used_two_factor_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsedTwoFactorChallenge {

    @Id
    @Column(length = 64)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;
}
