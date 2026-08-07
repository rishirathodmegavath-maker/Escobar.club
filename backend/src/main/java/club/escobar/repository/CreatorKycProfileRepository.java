package club.escobar.repository;

import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreatorKycProfileRepository extends JpaRepository<CreatorKycProfile, Long> {

    Optional<CreatorKycProfile> findByCreator_Id(Long creatorId);

    long countByStatus(KycStatus status);

    // "Admin-approved" is stricter than a bare VERIFIED status: a business can also verify a
    // creator's KYC (peer review, scoped to their own content relationship), but only an admin's
    // sign-off unlocks platform-wide actions like content submission and payouts.
    boolean existsByCreator_IdAndStatusAndReviewedBy_Role(Long creatorId, KycStatus status, UserRole role);
}
