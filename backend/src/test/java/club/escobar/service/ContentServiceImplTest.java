package club.escobar.service;

import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentPublishRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.content.ContentUpdateRequest;
import club.escobar.entity.Campaign;
import club.escobar.entity.Content;
import club.escobar.entity.User;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.mapper.ContentMapper;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.impl.ContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContentMapper contentMapper;
    @Mock
    private CreatorKycProfileRepository creatorKycProfileRepository;

    private ContentServiceImpl contentService;

    private User creator;
    private User business;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        contentService = new ContentServiceImpl(contentRepository, campaignRepository, userRepository, creatorKycProfileRepository, contentMapper);
        lenient().when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(anyLong(), any(), any()))
                .thenReturn(true);
        creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).build();
        business = User.builder().id(2L).email("business@test.com").role(UserRole.BUSINESS).build();
        campaign = Campaign.builder()
                .id(3L).business(business).title("Summer Launch")
                .submissionOpenAt(LocalDate.now().minusDays(1)).submissionDeadline(LocalDate.now().plusDays(5))
                .publishStartAt(LocalDate.now().plusDays(6)).publishEndAt(LocalDate.now().plusDays(30))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.PUBLISHED)
                .build();
    }

    @Test
    void submit_createsContentInSubmittedStatus_whenCampaignOpen() {
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.submit(1L, new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE));

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ContentStatus.SUBMITTED);
        assertThat(captor.getValue().getVersion()).isEqualTo(1);
        assertThat(captor.getValue().getCampaign()).isEqualTo(campaign);
        assertThat(captor.getValue().getBusiness()).isEqualTo(business);
        assertThat(captor.getValue().getCreator()).isEqualTo(creator);
    }

    @Test
    void submit_allowsMultipleSubmissionsToTheSameCampaign() {
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.submit(1L, new ContentCreateRequest(3L, "first", "http://x/media1.png", MediaType.IMAGE));
        contentService.submit(1L, new ContentCreateRequest(3L, "second", "http://x/media2.png", MediaType.IMAGE));

        verify(contentRepository, times(2)).save(any(Content.class));
    }

    @Test
    void submit_rejectsWhenCreatorKycNotAdminVerified() {
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.submit(1L,
                new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    // The four scenarios below all resolve through the same admin-verified derived query and
    // therefore hit the identical code path in ContentServiceImpl - they're kept as separate,
    // explicitly-named tests (rather than folded into the generic test above) so each of the
    // real-world KYC states this gate must block is independently documented and regression-tested.

    @Test
    void submit_rejectsWhenCreatorHasNotStartedKyc() {
        // No CreatorKycProfile row exists yet - the derived query naturally returns false.
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.submit(1L,
                new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void submit_rejectsWhenCreatorKycIsPending() {
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.submit(1L,
                new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void submit_rejectsWhenCreatorKycIsRejected() {
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.submit(1L,
                new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void submit_rejectsWhenKycVerifiedByBusinessOnly_notByAdmin() {
        // A business's peer review can set status=VERIFIED, but the derived query specifically
        // requires reviewedBy.role=ADMIN, so a business-only verification still returns false.
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.submit(1L,
                new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void submit_allowsWhenCreatorKycIsAdminVerified() {
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(true);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.submit(1L, new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE));

        verify(contentRepository).save(any(Content.class));
    }

    @Test
    void submit_rejectsWhenCampaignNotOpenForSubmissions() {
        campaign.setStatus(CampaignStatus.DRAFT);
        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> contentService.submit(1L,
                new ContentCreateRequest(3L, "caption", "http://x/media.png", MediaType.IMAGE)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void resubmit_incrementsVersionAndReturnsToSubmitted_whenChangesWereRequested() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.CHANGES_REQUESTED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.resubmit(1L, 20L, new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE));

        assertThat(content.getVersion()).isEqualTo(2);
        assertThat(content.getStatus()).isEqualTo(ContentStatus.SUBMITTED);
        assertThat(content.getMediaUrl()).isEqualTo("new.png");
    }

    @Test
    void resubmit_rejectsWhenCreatorKycNotAdminVerified() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.CHANGES_REQUESTED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.resubmit(1L, 20L,
                new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    // As with submit() above, these resolve through the same derived query and hit the same code
    // path - kept explicit so every real-world KYC state this gate must block on resubmission is
    // independently documented and regression-tested.

    private Content changesRequestedContent() {
        return Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.CHANGES_REQUESTED).version(1).build();
    }

    @Test
    void resubmit_rejectsWhenCreatorHasNotStartedKyc() {
        Content content = changesRequestedContent();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.resubmit(1L, 20L,
                new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void resubmit_rejectsWhenCreatorKycIsPending() {
        Content content = changesRequestedContent();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.resubmit(1L, 20L,
                new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void resubmit_rejectsWhenCreatorKycIsRejected() {
        Content content = changesRequestedContent();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.resubmit(1L, 20L,
                new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void resubmit_rejectsWhenKycVerifiedByBusinessOnly_notByAdmin() {
        Content content = changesRequestedContent();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(false);

        assertThatThrownBy(() -> contentService.resubmit(1L, 20L,
                new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void resubmit_allowsWhenCreatorKycIsAdminVerified() {
        Content content = changesRequestedContent();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(creatorKycProfileRepository.existsByCreator_IdAndStatusAndReviewedBy_Role(1L, KycStatus.VERIFIED, UserRole.ADMIN))
                .thenReturn(true);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.resubmit(1L, 20L, new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.SUBMITTED);
    }

    @Test
    void resubmit_rejectsWhenStatusIsNotChangesRequested() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.SUBMITTED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.resubmit(1L, 20L,
                new ContentUpdateRequest("new caption", "new.png", MediaType.IMAGE)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void review_rejectsReviewingDraftContent() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.DRAFT).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.review(2L, 20L,
                new ContentReviewRequest(ContentStatus.APPROVED, "looks good")))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void review_appendsNoteToHistory_withoutOverwritingPriorNotes() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.SUBMITTED).version(2).build();
        content.addReviewNote(club.escobar.entity.ContentReviewNote.builder()
                .authoredBy(business).contentVersion(1).decision(ContentStatus.CHANGES_REQUESTED).noteText("fix lighting").build());

        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(userRepository.getReferenceById(2L)).thenReturn(business);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.review(2L, 20L, new ContentReviewRequest(ContentStatus.APPROVED, "great work"));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.APPROVED);
        assertThat(content.getReviewNotes()).hasSize(2);
        assertThat(content.getReviewNotes().get(0).getNoteText()).isEqualTo("fix lighting");
        assertThat(content.getReviewNotes().get(1).getNoteText()).isEqualTo("great work");
    }

    @Test
    void review_isNotBlockedByCreatorKycStatus() {
        // Business review of already-submitted content must not be re-gated on the creator's current
        // KYC status - the gate only applies at submit()/resubmit() time, before content exists.
        // (No creatorKycProfileRepository stub here on purpose: review() must never consult it.)
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.SUBMITTED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(userRepository.getReferenceById(2L)).thenReturn(business);
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.review(2L, 20L, new ContentReviewRequest(ContentStatus.APPROVED, "looks good"));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.APPROVED);
        verify(creatorKycProfileRepository, never()).existsByCreator_IdAndStatusAndReviewedBy_Role(any(), any(), any());
    }

    @Test
    void review_rejectsWhenContentNotOwnedByBusiness() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.SUBMITTED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.review(999L, 20L,
                new ContentReviewRequest(ContentStatus.APPROVED, "ok")))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void publish_transitionsApprovedToPublished_andStoresUrlAndTimestamp() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.APPROVED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));
        when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contentMapper.toResponse(any(Content.class))).thenReturn(mock(ContentResponse.class));

        contentService.publish(1L, 20L, new ContentPublishRequest("https://www.instagram.com/p/Cabc123/"));

        assertThat(content.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(content.getPostUrl()).isEqualTo("https://www.instagram.com/p/Cabc123/");
        assertThat(content.getPublishedAt()).isNotNull();
    }

    @Test
    void publish_rejectsWhenNotApproved() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.SUBMITTED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.publish(1L, 20L,
                new ContentPublishRequest("https://www.instagram.com/p/Cabc123/")))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void publish_rejectsWhenNotOwnedByCreator() {
        Content content = Content.builder().id(20L).creator(creator).campaign(campaign).business(business)
                .mediaUrl("old.png").mediaType(MediaType.IMAGE).status(ContentStatus.APPROVED).version(1).build();
        when(contentRepository.findById(20L)).thenReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.publish(999L, 20L,
                new ContentPublishRequest("https://www.instagram.com/p/Cabc123/")))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
