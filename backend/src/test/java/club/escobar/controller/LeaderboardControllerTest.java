package club.escobar.controller;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.metrics.LeaderboardEntryResponse;
import club.escobar.entity.User;
import club.escobar.entity.enums.UserRole;
import club.escobar.security.JwtAuthenticationFilter;
import club.escobar.security.SecurityUser;
import club.escobar.service.ContentMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentMetricsService contentMetricsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UserRole role) {
        User user = User.builder().id(2L).email("business@test.com").role(role).active(true).build();
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void businessLeaderboard_returns200_withRankedEntries() throws Exception {
        authenticateAs(UserRole.BUSINESS);
        var entry = new LeaderboardEntryResponse(1, 1L, "Jamie", null, 500L, 2L);
        when(contentMetricsService.businessLeaderboard(any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(entry), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/businesses/2/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rank").value(1))
                .andExpect(jsonPath("$.content[0].totalViews").value(500));
    }

    @Test
    void globalLeaderboard_returns200_forAnyAuthenticatedRole() throws Exception {
        authenticateAs(UserRole.CREATOR);
        var entry = new LeaderboardEntryResponse(1, 1L, "Jamie", null, 500L, 2L);
        when(contentMetricsService.globalLeaderboard(any()))
                .thenReturn(new PageResponse<>(List.of(entry), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/leaderboard/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].creatorDisplayName").value("Jamie"));
    }
}
