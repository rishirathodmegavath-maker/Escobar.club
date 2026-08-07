package club.escobar.integration;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.LoginResponse;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.common.PageResponse;
import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentPublishRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.metrics.ContentMetricsSnapshotResponse;
import club.escobar.dto.metrics.LeaderboardEntryResponse;
import club.escobar.entity.User;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.UserRole;
import club.escobar.integration.apify.ApifyInstagramClient;
import club.escobar.integration.apify.ApifyPostMetrics;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PublishingAndMetricsFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @MockBean
    private ApifyInstagramClient apifyInstagramClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private AuthResponse registerAndLogin(String email, UserRole role, String displayName) {
        var registerResponse = rest.postForEntity(baseUrl() + "/api/auth/register",
                new RegisterRequest(email, "password123", role, displayName,
                        uniqueGstNumber(email), "Contact Person", "9876543210"),
                RegisterResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Newly-registered businesses start PENDING admin approval; grandfather them here so existing
        // test flows that create/publish campaigns don't need to simulate the admin-approval step.
        if (role == UserRole.BUSINESS) {
            businessProfileRepository.findByUser_Id(user.getId()).ifPresent(profile -> {
                profile.setApprovalStatus(ApprovalStatus.APPROVED);
                businessProfileRepository.save(profile);
            });
        }

        var loginResponse = rest.postForEntity(baseUrl() + "/api/auth/login",
                new LoginRequest(email, "password123"),
                LoginResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginResponse.getBody().auth();
    }

    // GST numbers are now unique per business account; derive a distinct-but-valid-looking one per email
    // instead of reusing the same literal across every registration in this test class.
    private String uniqueGstNumber(String email) {
        return "22AAAAA" + String.format("%04d", Math.abs(email.hashCode()) % 10000) + "A1Z5";
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    // Submissions opened yesterday and stay open for 10 more days; publishing hasn't started yet - so
    // the campaign is Upcoming and currently accepting creator submissions.
    private Long createCampaignOpenForSubmissions(AuthResponse businessAuth, String title) {
        var createResponse = rest.exchange(baseUrl() + "/api/campaigns", HttpMethod.POST,
                new HttpEntity<>(new CampaignCreateRequest(title, "Campaign description",
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(11), LocalDate.now().plusDays(40),
                        new BigDecimal("100.00"), false),
                        authHeaders(businessAuth.accessToken())),
                CampaignResponse.class);
        return createResponse.getBody().id();
    }

    @Test
    void publishSyncAndLeaderboard_fullFlow() {
        AuthResponse creatorAuth = registerAndLogin("creator3@test.com", UserRole.CREATOR, "Jordan Creator");
        AuthResponse businessAuth = registerAndLogin("business3@test.com", UserRole.BUSINESS, "Gamma Co");
        Long businessId = businessAuth.user().id();
        Long campaignId = createCampaignOpenForSubmissions(businessAuth, "Gamma Launch");

        var submitResponse = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId + "/content", HttpMethod.POST,
                new HttpEntity<>(new ContentCreateRequest(campaignId, "caption", "http://media/1.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                ContentResponse.class);
        Long contentId = submitResponse.getBody().id();

        rest.exchange(baseUrl() + "/api/content/" + contentId + "/review", HttpMethod.PATCH,
                new HttpEntity<>(new ContentReviewRequest(ContentStatus.APPROVED, "Looks great"),
                        authHeaders(businessAuth.accessToken())),
                ContentResponse.class);

        // Publish
        var publishResponse = rest.exchange(baseUrl() + "/api/content/" + contentId + "/publish", HttpMethod.PATCH,
                new HttpEntity<>(new ContentPublishRequest("https://www.instagram.com/p/Cabc123/"),
                        authHeaders(creatorAuth.accessToken())),
                ContentResponse.class);
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody().status()).isEqualTo(ContentStatus.PUBLISHED);

        // Publishing again is rejected (no longer APPROVED)
        var publishAgain = rest.exchange(baseUrl() + "/api/content/" + contentId + "/publish", HttpMethod.PATCH,
                new HttpEntity<>(new ContentPublishRequest("https://www.instagram.com/p/Cabc123/"),
                        authHeaders(creatorAuth.accessToken())),
                String.class);
        assertThat(publishAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Stub the Apify call - real Apify is out of scope for this test environment
        when(apifyInstagramClient.fetchPostMetrics(any())).thenReturn(new ApifyPostMetrics(10L, 3L, 250L, "{}"));

        var syncResponse = rest.exchange(baseUrl() + "/api/content/" + contentId + "/metrics/sync", HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(creatorAuth.accessToken())),
                ContentMetricsSnapshotResponse.class);
        assertThat(syncResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(syncResponse.getBody().viewCount()).isEqualTo(250L);

        // Syncing again immediately is rate-limited
        var syncAgain = rest.exchange(baseUrl() + "/api/content/" + contentId + "/metrics/sync", HttpMethod.POST,
                new HttpEntity<>(null, authHeaders(creatorAuth.accessToken())),
                String.class);
        assertThat(syncAgain.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Business leaderboard shows the creator
        var businessLeaderboard = rest.exchange(baseUrl() + "/api/businesses/" + businessId + "/leaderboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders(businessAuth.accessToken())),
                new org.springframework.core.ParameterizedTypeReference<PageResponse<LeaderboardEntryResponse>>() {});
        assertThat(businessLeaderboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(businessLeaderboard.getBody().content()).hasSize(1);
        assertThat(businessLeaderboard.getBody().content().get(0).totalViews()).isEqualTo(250L);

        // Global leaderboard requires authentication
        var unauthenticatedGlobal = rest.getForEntity(baseUrl() + "/api/leaderboard/global", String.class);
        assertThat(unauthenticatedGlobal.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Authenticated (either role) can view it
        var authenticatedGlobal = rest.exchange(baseUrl() + "/api/leaderboard/global", HttpMethod.GET,
                new HttpEntity<>(authHeaders(creatorAuth.accessToken())),
                new org.springframework.core.ParameterizedTypeReference<PageResponse<LeaderboardEntryResponse>>() {});
        assertThat(authenticatedGlobal.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authenticatedGlobal.getBody().content()).isNotEmpty();
    }
}
