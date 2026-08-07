package club.escobar.repository;

import club.escobar.entity.UsedTwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsedTwoFactorChallengeRepository extends JpaRepository<UsedTwoFactorChallenge, String> {
}
