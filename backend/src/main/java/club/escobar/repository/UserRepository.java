package club.escobar.repository;

import club.escobar.entity.User;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByGoogleId(String googleId);

    Page<User> findByRole(UserRole role, Pageable pageable);

    long countByRole(UserRole role);

    @Query("""
            SELECT u FROM User u
            JOIN CreatorKycProfile k ON k.creator = u
            WHERE u.role = :role AND k.status = :kycStatus
            """)
    Page<User> findByRoleAndKycStatus(@Param("role") UserRole role, @Param("kycStatus") KycStatus kycStatus, Pageable pageable);
}
