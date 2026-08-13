package club.escobar.controller;

import club.escobar.dto.creator.CreatorProfileResponse;
import club.escobar.dto.creator.CreatorProfileUpdateRequest;
import club.escobar.entity.User;
import club.escobar.entity.enums.UserRole;
import club.escobar.security.JwtAuthenticationFilter;
import club.escobar.security.SecurityUser;
import club.escobar.service.CreatorProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreatorController.class)
@AutoConfigureMockMvc(addFilters = false)
class CreatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatorProfileService creatorProfileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUpSecurityContext() {
        User user = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).active(true).build();
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CreatorProfileUpdateRequest requestWithPortfolioLink(String link) {
        return new CreatorProfileUpdateRequest(
                "Jamie Rivera", "bio", "", "Beauty", false,
                "https://instagram.com/jamie", 1000L,
                link == null ? List.of() : List.of(link));
    }

    private CreatorProfileResponse sampleResponse() {
        return new CreatorProfileResponse(1L, 1L, "creator@test.com", "Jamie Rivera", "bio", "",
                "Beauty", false, "https://instagram.com/jamie", 1000L, List.of(), Instant.now());
    }

    private void expectAccepted(String link) throws Exception {
        when(creatorProfileService.updateOwnProfile(any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(put("/api/creators/me")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithPortfolioLink(link))))
                .andExpect(status().isOk());
    }

    private void expectRejected(String link) throws Exception {
        mockMvc.perform(put("/api/creators/me")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithPortfolioLink(link))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_acceptsHttpsPortfolioLink() throws Exception {
        expectAccepted("https://example.com/portfolio");
    }

    @Test
    void updateProfile_acceptsHttpPortfolioLink() throws Exception {
        expectAccepted("http://example.com/portfolio");
    }

    @Test
    void updateProfile_rejectsJavascriptScheme() throws Exception {
        expectRejected("javascript:alert(1)");
    }

    @Test
    void updateProfile_rejectsDataScheme() throws Exception {
        expectRejected("data:text/html,<script>alert(1)</script>");
    }

    @Test
    void updateProfile_rejectsVbscriptScheme() throws Exception {
        expectRejected("vbscript:msgbox(1)");
    }

    @Test
    void updateProfile_rejectsProtocolRelativeUrl() throws Exception {
        expectRejected("//evil.com");
    }

    @Test
    void updateProfile_rejectsBlankPortfolioLink() throws Exception {
        expectRejected("");
    }
}
