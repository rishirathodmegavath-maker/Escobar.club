package club.escobar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

// Append-only audit row for a campaign reschedule - never updated or deleted, mirrors
// ContentReviewNote's shape (parent entity + author + created_at, one row per change).
@Entity
@Table(name = "campaign_schedule_changes", indexes = {
        @Index(name = "idx_campaign_schedule_changes_campaign", columnList = "campaign_id, changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignScheduleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "old_submission_open_at", nullable = false)
    private LocalDate oldSubmissionOpenAt;

    @Column(name = "old_submission_deadline", nullable = false)
    private LocalDate oldSubmissionDeadline;

    @Column(name = "old_publish_start_at", nullable = false)
    private LocalDate oldPublishStartAt;

    @Column(name = "old_publish_end_at", nullable = false)
    private LocalDate oldPublishEndAt;

    @Column(name = "new_submission_open_at", nullable = false)
    private LocalDate newSubmissionOpenAt;

    @Column(name = "new_submission_deadline", nullable = false)
    private LocalDate newSubmissionDeadline;

    @Column(name = "new_publish_start_at", nullable = false)
    private LocalDate newPublishStartAt;

    @Column(name = "new_publish_end_at", nullable = false)
    private LocalDate newPublishEndAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;
}
