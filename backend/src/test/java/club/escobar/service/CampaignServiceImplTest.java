package club.escobar.service;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.entity.Campaign;
import club.escobar.entity.User;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.mapper.CampaignMapper;
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

    @Test
    void create_startsAsActive_whenStartDateAlreadyWithinRange() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toResponse(any(Campaign.class))).thenReturn(mock(CampaignResponse.class));

        campaignService.create(2L, new CampaignCreateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(10), new BigDecimal("100.00")));

        var captor = org.mockito.ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(captor.getValue().getBusiness()).isEqualTo(business);
    }

    @Test
    void create_startsAsStartingSoon_whenStartDateInFuture() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toResponse(any(Campaign.class))).thenReturn(mock(CampaignResponse.class));

        campaignService.create(2L, new CampaignCreateRequest("Launch", "desc",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10), new BigDecimal("100.00")));

        var captor = org.mockito.ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CampaignStatus.STARTING_SOON);
    }

    @Test
    void create_startsAsClosed_whenDateRangeAlreadyInPast() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toResponse(any(Campaign.class))).thenReturn(mock(CampaignResponse.class));

        campaignService.create(2L, new CampaignCreateRequest("Launch", "desc",
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), new BigDecimal("100.00")));

        var captor = org.mockito.ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CampaignStatus.CLOSED);
    }

    @Test
    void create_rejectsWhenCallerIsNotBusiness() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> campaignService.create(1L, new CampaignCreateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(10), new BigDecimal("100.00"))))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void create_rejectsWhenEndDateBeforeStartDate() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(business));

        assertThatThrownBy(() -> campaignService.create(2L, new CampaignCreateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().minusDays(1), new BigDecimal("100.00"))))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void update_rejectsWhenNotOwner() {
        Campaign campaign = Campaign.builder().id(5L).business(business).title("Launch")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.DRAFT).build();
        when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.update(999L, 5L, new CampaignUpdateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(10), new BigDecimal("100.00"), CampaignStatus.ACTIVE)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void update_allowsOwnerToActivate() {
        Campaign campaign = Campaign.builder().id(5L).business(business).title("Launch")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .ratePerThousandViewsInr(new BigDecimal("100.00")).status(CampaignStatus.DRAFT).build();
        when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignMapper.toResponse(any(Campaign.class))).thenReturn(mock(CampaignResponse.class));

        campaignService.update(2L, 5L, new CampaignUpdateRequest("Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(10), new BigDecimal("150.00"), CampaignStatus.ACTIVE));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaign.getRatePerThousandViewsInr()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void isOpenForSubmissions_trueOnlyWhenActiveAndWithinDateWindow() {
        Campaign active = Campaign.builder().business(business).title("Launch")
                .startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(1))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.ACTIVE).build();
        assertThat(active.isOpenForSubmissions()).isTrue();

        Campaign draft = Campaign.builder().business(business).title("Launch")
                .startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(1))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.DRAFT).build();
        assertThat(draft.isOpenForSubmissions()).isFalse();

        Campaign expired = Campaign.builder().business(business).title("Launch")
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().minusDays(1))
                .ratePerThousandViewsInr(BigDecimal.TEN).status(CampaignStatus.ACTIVE).build();
        assertThat(expired.isOpenForSubmissions()).isFalse();
    }
}
