package club.escobar.entity;

import club.escobar.entity.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "business_profiles", indexes = {
        @Index(name = "idx_business_company_name", columnList = "company_name"),
        @Index(name = "idx_business_industry", columnList = "industry")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "gst_number", unique = true, length = 20)
    private String gstNumber;

    @Column(name = "contact_person_name", nullable = false, length = 150)
    private String contactPersonName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(length = 80)
    private String industry;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 300)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
