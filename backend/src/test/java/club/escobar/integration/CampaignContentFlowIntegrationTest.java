package club.escobar.integration;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.content.ContentUpdateRequest;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.UserRole;
import org.junit.jupiter.api.Test;
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

    private final TestRestTemplate rest = new TestRestTemplate();

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private AuthResponse registerAndLogin(String email, UserRole role, String displayName) {
        var response = rest.postForEntity(baseUrl() + "/api/auth/register",
                new RegisterRequest(email, "password123", role, displayName,
                        "22AAAAA0000A1Z5", "Contact Person", "9876543210"),
                AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private Long createActiveCampaign(AuthResponse businessAuth, String title) {
        var createResponse = rest.exchange(baseUrl() + "/api/campaigns", HttpMethod.POST,
                new HttpEntity<>(new CampaignCreateRequest(title, "Campaign description",
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), new BigDecimal("100.00"), false),
                        authHeaders(businessAuth.accessToken())),
                CampaignResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long campaignId = createResponse.getBody().id();

        var activateResponse = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId, HttpMethod.PUT,
                new HttpEntity<>(new CampaignUpdateRequest(title, "Campaign description",
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), new BigDecimal("100.00"), CampaignStatus.ACTIVE, false),
                        authHeaders(businessAuth.accessToken())),
                CampaignResponse.class);
        assertThat(activateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return campaignId;
    }

    @Test
    void fullLifecycle_fromDirectSubmissionToApprovedContent_withChangeRequestRoundTrip() {
        AuthResponse creatorAuth = registerAndLogin("creator1@test.com", UserRole.CREATOR, "Jamie Creator");
        AuthResponse businessAuth = registerAndLogin("business1@test.com", UserRole.BUSINESS, "Acme Co");
        Long campaignId = createActiveCampaign(businessAuth, "Acme Summer Launch");

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
        Long campaignId = createActiveCampaign(businessAuth, "Beta Launch");

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
                        LocalDate.now().plusDays(10), LocalDate.now().plusDays(30), new BigDecimal("100.00"), false),
                        authHeaders(businessAuth.accessToken())),
                CampaignResponse.class);
        Long campaignId = createResponse.getBody().id();
        // Left as DRAFT (not activated), so it should not accept submissions.

        var submitResponse = rest.exchange(baseUrl() + "/api/campaigns/" + campaignId + "/content", HttpMethod.POST,
                new HttpEntity<>(new ContentCreateRequest(campaignId, "caption", "http://media/1.png", MediaType.IMAGE),
                        authHeaders(creatorAuth.accessToken())),
                String.class);

        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
