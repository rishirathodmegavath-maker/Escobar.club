package club.escobar.entity;

import club.escobar.entity.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "creator_kyc_profiles", indexes = {
        @Index(name = "idx_creator_kyc_profiles_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorKycProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User creator;

    @Column(name = "pan_number", nullable = false, length = 10)
    private String panNumber;

    @Column(name = "name_on_pan", nullable = false, length = 150)
    private String nameOnPan;

    // Column name kept as document_url for schema stability, but this now holds an opaque private
    // storage key (see StorageService.storePrivate), never a fetchable URL.
    @Column(name = "document_url", nullable = false, length = 500)
    private String documentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KycStatus status = KycStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
