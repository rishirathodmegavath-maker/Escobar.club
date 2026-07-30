package club.escobar.entity;

import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content", indexes = {
        @Index(name = "idx_content_creator", columnList = "creator_id"),
        @Index(name = "idx_content_business", columnList = "business_id"),
        @Index(name = "idx_content_campaign", columnList = "campaign_id"),
        @Index(name = "idx_content_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Denormalized for query simplicity on review queues / inboxes.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private User business;

    // Denormalized for query simplicity, same rationale as creator/business above.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    @Column(name = "post_url", length = 500)
    private String postUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ContentReviewNote> reviewNotes = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fetchedAt DESC")
    @Builder.Default
    private List<ContentMetricsSnapshot> metricsSnapshots = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public void addReviewNote(ContentReviewNote note) {
        note.setContent(this);
        this.reviewNotes.add(note);
    }

    public void addMetricsSnapshot(ContentMetricsSnapshot snapshot) {
        snapshot.setContent(this);
        this.metricsSnapshots.add(snapshot);
    }
}
