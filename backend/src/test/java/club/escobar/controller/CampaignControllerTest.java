package club.escobar.controller;

import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.common.PageResponse;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.CampaignStatus;
import club.escobar.security.JwtAuthenticationFilter;
import club.escobar.service.CampaignService;
import club.escobar.service.CreatorMatchingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Discovery/browse must stay open to any creator regardless of KYC status (NOT_STARTED, PENDING,
// REJECTED, or VERIFIED) - these tests prove the point at its root: the endpoints require no
// authentication at all (no SecurityContext is ever set up here), so no creator KYC state can
// possibly affect them. addFilters=false only skips the JWT filter itself; @PreAuthorize would
// still reject an unauthenticated call if one were mistakenly added to these methods.
@WebMvcTest(controllers = CampaignController.class)
@AutoConfigureMockMvc(addFilters = false)
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignService campaignService;

    @MockBean
    private CreatorMatchingService creatorMatchingService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CampaignResponse sampleResponse() {
        return new CampaignResponse(3L, 2L, "Acme Co", null, "Summer Launch", "desc",
                LocalDate.now(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), LocalDate.now().plusDays(20),
                new BigDecimal("100.00"), CampaignStatus.PUBLISHED, true, false, false,
                ApprovalStatus.APPROVED, null, null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void browse_succeedsWithoutAnyAuthentication() throws Exception {
        when(campaignService.listPublic(any(), any(), any()))
                .thenReturn(new PageResponse<>(java.util.List.of(sampleResponse()), 0, 12, 1, 1, true));

        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Summer Launch"));
    }

    @Test
    void getById_succeedsWithoutAnyAuthentication() throws Exception {
        when(campaignService.getById(3L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/campaigns/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Summer Launch"));
    }
}
