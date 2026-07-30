package club.escobar.entity;

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
        @Index(name = "idx_campaigns_dates", columnList = "start_date, end_date")
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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "rate_per_thousand_views_inr", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerThousandViewsInr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private boolean urgent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isOpenForSubmissions() {
        LocalDate today = LocalDate.now();
        return status == CampaignStatus.ACTIVE && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    // A live campaign is "Hot" when the brand marked it urgent, its submission window closes within 72
    // hours, or its rate is in the top tier of currently-live campaigns (activeRateThreshold, computed by
    // the caller across all ACTIVE campaigns). Only ever true for campaigns already open for submissions.
    public boolean isHot(BigDecimal activeRateThreshold) {
        if (!isOpenForSubmissions()) {
            return false;
        }
        if (urgent) {
            return true;
        }
        Instant deadline = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long hoursLeft = Duration.between(Instant.now(), deadline).toHours();
        if (hoursLeft <= 72) {
            return true;
        }
        return activeRateThreshold != null && ratePerThousandViewsInr.compareTo(activeRateThreshold) >= 0;
    }
}
