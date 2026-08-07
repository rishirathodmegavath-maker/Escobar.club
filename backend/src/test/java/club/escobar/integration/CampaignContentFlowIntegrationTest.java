package club.escobar.integration;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.LoginResponse;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.content.ContentUpdateRequest;
import club.escobar.entity.User;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.UserRole;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignContentFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

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
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return createResponse.getBody().id();
    }

    @Test
    void fullLifecycle_fromDirectSubmissionToApprovedContent_withChangeRequestRoundTrip() {
        AuthResponse creatorAuth = registerAndLogin("creator1@test.com", UserRole.CREATOR, "Jamie Creator");
        AuthResponse businessAuth = registerAndLogin("business1@test.com", UserRole.BUSINESS, "Acme Co");
        Long campaignId = createCampaignOpenForSubmissions(businessAuth, "Acme Summer Launch");

        // 1. Creator submits content directly, with no application/approval step
        var submitResponse = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId + "/content", HttpMethod.POST,
                new HttpEntity<>(new ContentCreateRequest(campaignId, "Check out this post!", "http://media/1.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                ContentResponse.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long contentId = submitResponse.getBody().id();
        assertThat(submitResponse.getBody().status()).isEqualTo(ContentStatus.SUBMITTED);
        assertThat(submitResponse.getBody().version()).isEqualTo(1);

        // 2. Business requests changes
        var changesResponse = rest.exchange(baseUrl() + "/api/content/" + contentId + "/review", HttpMethod.PATCH,
                new HttpEntity<>(new ContentReviewRequest(ContentStatus.CHANGES_REQUESTED, "Please brighten the photo"),
                        authHeaders(businessAuth.accessToken())),
                ContentResponse.class);
        assertThat(changesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changesResponse.getBody().status()).isEqualTo(ContentStatus.CHANGES_REQUESTED);
        assertThat(changesResponse.getBody().reviewNotes()).hasSize(1);

        // 3. Creator resubmits (new version), history is preserved not overwritten
        var resubmitResponse = rest.exchange(baseUrl() + "/api/content/" + contentId, HttpMethod.PATCH,
                new HttpEntity<>(new ContentUpdateRequest("Brighter version!", "http://media/2.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                ContentResponse.class);
        assertThat(resubmitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resubmitResponse.getBody().status()).isEqualTo(ContentStatus.SUBMITTED);
        assertThat(resubmitResponse.getBody().version()).isEqualTo(2);

        // 4. Business approves the resubmission
        var finalApproval = rest.exchange(baseUrl() + "/api/content/" + contentId + "/review", HttpMethod.PATCH,
                new HttpEntity<>(new ContentReviewRequest(ContentStatus.APPROVED, "Looks great now"),
                        authHeaders(businessAuth.accessToken())),
                ContentResponse.class);
        assertThat(finalApproval.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalApproval.getBody().status()).isEqualTo(ContentStatus.APPROVED);
        assertThat(finalApproval.getBody().reviewNotes()).hasSize(2);
        assertThat(finalApproval.getBody().reviewNotes().get(0).noteText()).isEqualTo("Please brighten the photo");
        assertThat(finalApproval.getBody().reviewNotes().get(1).noteText()).isEqualTo("Looks great now");
    }

    @Test
    void creatorCanSubmitMultiplePiecesOfContent_toTheSameCampaign() {
        AuthResponse creatorAuth = registerAndLogin("creator2@test.com", UserRole.CREATOR, "Alex Creator");
        AuthResponse businessAuth = registerAndLogin("business2@test.com", UserRole.BUSINESS, "Beta Co");
        Long campaignId = createCampaignOpenForSubmissions(businessAuth, "Beta Launch");

        var first = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId + "/content", HttpMethod.POST,
                new HttpEntity<>(new ContentCreateRequest(campaignId, "first piece", "http://media/1.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                ContentResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var second = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId + "/content", HttpMethod.POST,
                new HttpEntity<>(new ContentCreateRequest(campaignId, "second piece", "http://media/2.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                ContentResponse.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().id()).isNotEqualTo(first.getBody().id());
    }

    @Test
    void submittingContent_toACampaignNotAcceptingSubmissions_isRejected() {
        AuthResponse creatorAuth = registerAndLogin("creator3@test.com", UserRole.CREATOR, "Jordan Creator");
        AuthResponse businessAuth = registerAndLogin("business3@test.com", UserRole.BUSINESS, "Gamma Co");

        var createResponse = rest.exchange(baseUrl() + "/api/campaigns", HttpMethod.POST,
                new HttpEntity<>(new CampaignCreateRequest("Gamma Launch", "Campaign description",
                        LocalDate.now().plusDays(20), LocalDate.now().plusDays(25),
                        LocalDate.now().plusDays(26), LocalDate.now().plusDays(40),
                        new BigDecimal("100.00"), false),
                        authHeaders(businessAuth.accessToken())),
                CampaignResponse.class);
        Long campaignId = createResponse.getBody().id();
        // Submissions don't open for another 20 days, so it should not accept submissions yet.

        var submitResponse = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId + "/content", HttpMethod.POST,
                new HttpEntity<>(new ContentCreateRequest(campaignId, "caption", "http://media/1.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                String.class);

        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
