package club.escobar.entity;

import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignDisplayStatus;
import club.escobar.entity.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaigns_business", columnList = "business_id"),
        @Index(name = "idx_campaigns_status", columnList = "status"),
        @Index(name = "idx_campaigns_dates", columnList = "publish_start_at, publish_end_at"),
        @Index(name = "idx_campaigns_submission_dates", columnList = "submission_open_at, submission_deadline")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private User business;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Creator submission window: uploads are only accepted while today is within [submissionOpenAt, submissionDeadline].
    @Column(name = "submission_open_at", nullable = false)
    private LocalDate submissionOpenAt;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDate submissionDeadline;

    // Publishing window: the phase where already-approved content goes live and views/analytics count.
    @Column(name = "publish_start_at", nullable = false)
    private LocalDate publishStartAt;

    @Column(name = "publish_end_at", nullable = false)
    private LocalDate publishEndAt;

    @Column(name = "rate_per_thousand_views_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerThousandViewsInr;

    // Optional total spend ceiling for this campaign. Null means unlimited (today's behavior).
    // Gates NEW submissions only, once committed payouts (PENDING_KYC+PAYABLE+PAID) reach this
    // amount - it never claws back or caps a payout already earned by a creator. See
    // ContentServiceImpl.submit().
    @Column(name = "max_budget_inr", precision = 12, scale = 2)
    private BigDecimal maxBudgetInr;

    // Only DRAFT/CANCELLED/PUBLISHED are ever persisted here. PUBLISHED means "governed by dates" -
    // getEffectiveStatus() resolves it to UPCOMING/LIVE/COMPLETED at read time so the lifecycle can
    // never go stale the way a manually-picked status could.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private boolean urgent = false;

    // Platform-moderation gate: only APPROVED campaigns are ever visible to creators, regardless of
    // status above. Independent of the business's own DRAFT/PUBLISHED/CANCELLED choice.
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    // Admin-curated label shown to creators once approved (Upcoming/Live/Hot/Closed). Null until an
    // admin sets one; frontend falls back to getEffectiveStatus()/isHot() in that case.
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_display_status", length = 20)
    private CampaignDisplayStatus adminDisplayStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // The status actually shown to clients: DRAFT/CANCELLED pass through as manual overrides,
    // anything else is derived fresh from today vs. the publish window every time it's called.
    public CampaignStatus getEffectiveStatus() {
        if (status == CampaignStatus.DRAFT || status == CampaignStatus.CANCELLED) {
            return status;
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(publishStartAt)) {
            return CampaignStatus.UPCOMING;
        }
        if (!today.isAfter(publishEndAt)) {
            return CampaignStatus.LIVE;
        }
        return CampaignStatus.COMPLETED;
    }

    // Creators may only submit content during the Upcoming phase, and only within the submission window.
    public boolean isOpenForSubmissions() {
        if (getEffectiveStatus() != CampaignStatus.UPCOMING) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(submissionOpenAt) && !today.isAfter(submissionDeadline);
    }

    public String submissionClosedReason() {
        CampaignStatus effective = getEffectiveStatus();
        if (effective == CampaignStatus.DRAFT) {
            return "This campaign has not been published yet.";
        }
        if (effective == CampaignStatus.CANCELLED) {
            return "This campaign has been cancelled.";
        }
        if (LocalDate.now().isBefore(submissionOpenAt)) {
            return "Submissions have not opened yet.";
        }
        return "Submission period has ended.";
    }

    // A live campaign is "Hot" when the brand marked it urgent, its submission window closes within 72
    // hours, or its rate is in the top tier of currently-open-for-submission campaigns (activeRateThreshold,
    // computed by the caller). Only ever true for campaigns already open for submissions.
    public boolean isHot(BigDecimal activeRateThreshold) {
        if (!isOpenForSubmissions()) {
            return false;
        }
        if (urgent) {
            return true;
        }
        Instant deadline = submissionDeadline.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long hoursLeft = Duration.between(Instant.now(), deadline).toHours();
        if (hoursLeft <= 72) {
            return true;
        }
        return activeRateThreshold != null && ratePerThousandViewsInr.compareTo(activeRateThreshold) >= 0;
    }
}
