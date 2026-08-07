package club.escobar.service;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.Campaign;
import club.escobar.entity.User;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.mapper.CampaignMapper;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CampaignRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.impl.CampaignServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessProfileRepository businessProfileRepository;
    @Mock
    private CampaignMapper campaignMapper;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    private User business;
    private User creator;

    @BeforeEach
    void setUp() {
        business = User.builder().id(2L).email("business@test.com").role(UserRole.BUSINESS).active(true).build();
        creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).active(true).build();
    }

    private CampaignCreateRequest createRequest(LocalDate submissionOpenAt, LocalDate submissionDeadline,
                                                  LocalDate publishStartAt, LocalDate publishEndAt) {
        return new CampaignCreateRequest("Launch", "desc", submissionOpenAt, submissionDeadline,
                publishStartAt, publishEndAt, new BigDecimal("100.00"), false);
    }

    @Test
    void create_alwaysPersistsAsPublished_lettingDatesDriveThePhase() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));
        when(businessProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(
                BusinessProfile.builder().approvalStatus(ApprovalStatus.APPROVED).build()));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toResponse(any(Campaign.class))).thenReturn(mock(CampaignResponse.class));

        campaignService.create(2L, createRequest(LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6), LocalDate.now().plusDays(20)));

        var captor = org.mockito.ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CampaignStatus.PUBLISHED);
        assertThat(captor.getValue().getBusiness()).isEqualTo(business);
    }

    @Test
    void create_rejectsWhenCallerIsNotBusiness() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> campaignService.create(1L, createRequest(LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6), LocalDate.now().plusDays(20))))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void create_rejectsWhenSubmissionDeadlineBeforeSubmissionOpen() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));

        assertThatThrownBy(() -> campaignService.create(2L, createRequest(LocalDate.now(), LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(6), LocalDate.now().plusDays(20))))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void create_rejectsWhenPublishStartsBeforeSubmissionDeadline() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));

        assertThatThrownBy(() -> campaignService.create(2L, createRequest(LocalDate.now(), LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(20))))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void create_rejectsWhenPublishEndBeforePublishStart() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));

        assertThatThrownBy(() -> campaignService.create(2L, createRequest(LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(6), LocalDate.now().plusDays(1))))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void update_rejectsWhenNotOwner() {
        Campaign campaign = Campaign.builder().id(5L).business(business).title("Launch")
                .submissionOpenAt(LocalDate.now()).submissionDeadline(LocalDate.now().plusDays(5))
                .publishStartAt(LocalDate.now().plusDays(6)).publishEndAt(LocalDate.now().plusDays(20))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.DRAFT).build();
        when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.update(999L, 5L, new CampaignUpdateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), LocalDate.now().plusDays(20),
                new BigDecimal("100.00"), CampaignStatus.PUBLISHED, false)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void update_rejectsManuallySettingAComputedStatus() {
        Campaign campaign = Campaign.builder().id(5L).business(business).title("Launch")
                .submissionOpenAt(LocalDate.now()).submissionDeadline(LocalDate.now().plusDays(5))
                .publishStartAt(LocalDate.now().plusDays(6)).publishEndAt(LocalDate.now().plusDays(20))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.DRAFT).build();
        when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.update(2L, 5L, new CampaignUpdateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), LocalDate.now().plusDays(20),
                new BigDecimal("100.00"), CampaignStatus.LIVE, false)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void update_allowsOwnerToPublish() {
        Campaign campaign = Campaign.builder().id(5L).business(business).title("Launch")
                .submissionOpenAt(LocalDate.now()).submissionDeadline(LocalDate.now().plusDays(5))
                .publishStartAt(LocalDate.now().plusDays(6)).publishEndAt(LocalDate.now().plusDays(20))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.DRAFT).build();
        when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toResponse(any(Campaign.class))).thenReturn(mock(CampaignResponse.class));

        campaignService.update(2L, 5L, new CampaignUpdateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), LocalDate.now().plusDays(20),
                new BigDecimal("150.00"), CampaignStatus.PUBLISHED, false));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PUBLISHED);
        assertThat(campaign.getRatePerThousandViewsInr()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void getEffectiveStatus_isUpcomingWhenBeforePublishStart() {
        Campaign campaign = Campaign.builder().business(business).title("Launch")
                .submissionOpenAt(LocalDate.now().minusDays(1)).submissionDeadline(LocalDate.now().plusDays(4))
                .publishStartAt(LocalDate.now().plusDays(5)).publishEndAt(LocalDate.now().plusDays(20))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.PUBLISHED).build();

        assertThat(campaign.getEffectiveStatus()).isEqualTo(CampaignStatus.UPCOMING);
        assertThat(campaign.isOpenForSubmissions()).isTrue();
    }

    @Test
    void getEffectiveStatus_isLiveDuringPublishWindow_andClosedForSubmissions() {
        Campaign campaign = Campaign.builder().business(business).title("Launch")
                .submissionOpenAt(LocalDate.now().minusDays(10)).submissionDeadline(LocalDate.now().minusDays(1))
                .publishStartAt(LocalDate.now().minusDays(1)).publishEndAt(LocalDate.now().plusDays(10))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.PUBLISHED).build();

        assertThat(campaign.getEffectiveStatus()).isEqualTo(CampaignStatus.LIVE);
        assertThat(campaign.isOpenForSubmissions()).isFalse();
    }

    @Test
    void getEffectiveStatus_isCompletedAfterPublishEnd() {
        Campaign campaign = Campaign.builder().business(business).title("Launch")
                .submissionOpenAt(LocalDate.now().minusDays(30)).submissionDeadline(LocalDate.now().minusDays(20))
                .publishStartAt(LocalDate.now().minusDays(19)).publishEndAt(LocalDate.now().minusDays(1))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.PUBLISHED).build();

        assertThat(campaign.getEffectiveStatus()).isEqualTo(CampaignStatus.COMPLETED);
        assertThat(campaign.isOpenForSubmissions()).isFalse();
    }

    @Test
    void isOpenForSubmissions_falseWhenManuallyDraft_evenWithinSubmissionWindow() {
        Campaign draft = Campaign.builder().business(business).title("Launch")
                .submissionOpenAt(LocalDate.now().minusDays(1)).submissionDeadline(LocalDate.now().plusDays(4))
                .publishStartAt(LocalDate.now().plusDays(5)).publishEndAt(LocalDate.now().plusDays(20))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.DRAFT).build();

        assertThat(draft.getEffectiveStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(draft.isOpenForSubmissions()).isFalse();
    }
}
