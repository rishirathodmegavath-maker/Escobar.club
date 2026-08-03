package club.escobar.service;

import club.escobar.config.MetricsSyncProperties;
import club.escobar.dto.metrics.ContentMetricsSnapshotResponse;
import club.escobar.dto.metrics.LeaderboardEntryResponse;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.ContentMetricsSnapshot;
import club.escobar.entity.User;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.exception.RateLimitExceededException;
import club.escobar.integration.apify.ApifyInstagramClient;
import club.escobar.integration.apify.ApifyPostMetrics;
import club.escobar.mapper.ContentMetricsMapper;
import club.escobar.repository.ContentMetricsSnapshotRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.LeaderboardRow;
import club.escobar.service.impl.ContentMetricsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentMetricsServiceImplTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private ContentMetricsSnapshotRepository contentMetricsSnapshotRepository;
    @Mock
    private ApifyInstagramClient apifyInstagramClient;
    @Mock
    private ContentMetricsMapper contentMetricsMapper;
    @Mock
    private PayoutService payoutService;

    private ContentMetricsServiceImpl service;

    private User creator;
    private User business;
    private Campaign campaign;
    private Content publishedContent;

    @BeforeEach
    void setUp() {
        MetricsSyncProperties properties = new MetricsSyncProperties(15);
        service = new ContentMetricsServiceImpl(contentRepository, contentMetricsSnapshotRepository,
                apifyInstagramClient, properties, contentMetricsMapper, payoutService);

        creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).build();
        business = User.builder().id(2L).email("business@test.com").role(UserRole.BUSINESS).build();
        campaign = Campaign.builder()
                .id(3L).business(business).title("Summer Launch")
                .submissionOpenAt(LocalDate.now().minusDays(10)).submissionDeadline(LocalDate.now().minusDays(2))
                .publishStartAt(LocalDate.now().minusDays(1)).publishEndAt(LocalDate.now().plusDays(30))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.PUBLISHED)
                .build();
        publishedContent = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("post.png").mediaType(MediaType.IMAGE).status(ContentStatus.PUBLISHED)
                .postUrl("https://www.instagram.com/p/Cabc123/").version(1).build();
    }

    @Test
    void syncMetrics_createsSnapshot_whenPublishedAndOwnedByCreator() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L)).thenReturn(Optional.empty());
        when(apifyInstagramClient.fetchPostMetrics(publishedContent.getPostUrl()))
                .thenReturn(new ApifyPostMetrics(10L, 2L, 100L, "{}"));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMetricsMapper.toResponse(any(ContentMetricsSnapshot.class))).thenReturn(mock(ContentMetricsSnapshotResponse.class));

        service.syncMetrics(1L, 20L);

        assertThat(publishedContent.getMetricsSnapshots()).hasSize(1);
        assertThat(publishedContent.getMetricsSnapshots().get(0).getViewCount()).isEqualTo(100L);
        verify(payoutService).recalculate(20L);
    }

    @Test
    void syncMetrics_createsSnapshot_whenPublishedAndOwnedByBusiness() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L)).thenReturn(Optional.empty());
        when(apifyInstagramClient.fetchPostMetrics(publishedContent.getPostUrl()))
                .thenReturn(new ApifyPostMetrics(10L, 2L, null, "{}"));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMetricsMapper.toResponse(any(ContentMetricsSnapshot.class))).thenReturn(mock(ContentMetricsSnapshotResponse.class));

        service.syncMetrics(2L, 20L);

        assertThat(publishedContent.getMetricsSnapshots()).hasSize(1);
        assertThat(publishedContent.getMetricsSnapshots().get(0).getViewCount()).isNull();
        verify(payoutService).recalculate(20L);
    }

    @Test
    void syncMetrics_rejectsWhenContentNotPublished() {
        publishedContent.setStatus(ContentStatus.APPROVED);
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));

        assertThatThrownBy(() -> service.syncMetrics(1L, 20L))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void syncMetrics_rejectsWhenRequesterIsNeitherCreatorNorBusiness() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));

        assertThatThrownBy(() -> service.syncMetrics(999L, 20L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void syncMetrics_rejectsWhenSyncedTooRecently() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        ContentMetricsSnapshot recent = ContentMetricsSnapshot.builder()
                .content(publishedContent).likeCount(1L).commentCount(0L).fetchedAt(Instant.now()).build();
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L)).thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.syncMetrics(1L, 20L))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void syncMetrics_allowsSyncAfterIntervalElapsed() {
        when(contentRepository.findById(20L)).thenReturn(Optional.of(publishedContent));
        ContentMetricsSnapshot old = ContentMetricsSnapshot.builder()
                .content(publishedContent).likeCount(1L).commentCount(0L)
                .fetchedAt(Instant.now().minus(20, ChronoUnit.MINUTES)).build();
        when(contentMetricsSnapshotRepository.findTopByContent_IdOrderByFetchedAtDesc(20L)).thenReturn(Optional.of(old));
        when(apifyInstagramClient.fetchPostMetrics(publishedContent.getPostUrl()))
                .thenReturn(new ApifyPostMetrics(10L, 2L, 100L, "{}"));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMetricsMapper.toResponse(any(ContentMetricsSnapshot.class))).thenReturn(mock(ContentMetricsSnapshotResponse.class));

        service.syncMetrics(1L, 20L);

        verify(apifyInstagramClient).fetchPostMetrics(publishedContent.getPostUrl());
        verify(payoutService).recalculate(20L);
    }

    @Test
    void businessLeaderboard_rejectsWhenRequesterDoesNotOwnBusiness() {
        assertThatThrownBy(() -> service.businessLeaderboard(999L, 2L, PageRequest.of(0, 10)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void businessLeaderboard_returnsRankedEntries_whenOwnedByRequester() {
        LeaderboardRow row = mock(LeaderboardRow.class);
        when(row.getCreatorId()).thenReturn(1L);
        when(row.getCreatorDisplayName()).thenReturn("Jane Creator");
        when(row.getTotalViews()).thenReturn(500L);
        when(row.getPublishedContentCount()).thenReturn(2L);

        Pageable pageable = PageRequest.of(0, 10);
        when(contentRepository.findBusinessLeaderboard(2L, pageable)).thenReturn(new PageImpl<>(List.of(row), pageable, 1));

        var result = service.businessLeaderboard(2L, 2L, pageable);

        assertThat(result.content()).hasSize(1);
        LeaderboardEntryResponse entry = result.content().get(0);
        assertThat(entry.rank()).isEqualTo(1);
        assertThat(entry.creatorId()).isEqualTo(1L);
        assertThat(entry.totalViews()).isEqualTo(500L);
    }
}
