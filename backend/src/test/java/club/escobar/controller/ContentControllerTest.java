package club.escobar.controller;

import club.escobar.dto.content.ContentPublishRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.entity.User;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.MediaType;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.security.JwtAuthenticationFilter;
import club.escobar.security.SecurityUser;
import club.escobar.service.ContentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContentService contentService;

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

    private ContentResponse sampleResponse(ContentStatus status) {
        return new ContentResponse(20L, 1L, "Jamie", null, 3L, "Summer Launch", 2L, "Acme", null,
                "caption", "media.png", MediaType.IMAGE, "https://www.instagram.com/p/Cabc123/",
                status, 1, List.of(), Instant.now(), Instant.now(), Instant.now(),
                status == ContentStatus.PUBLISHED ? Instant.now() : null);
    }

    @Test
    void publish_returns200_withPublishedStatus() throws Exception {
        when(contentService.publish(any(), any(), any())).thenReturn(sampleResponse(ContentStatus.PUBLISHED));

        mockMvc.perform(patch("/api/content/20/publish")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContentPublishRequest("https://www.instagram.com/p/Cabc123/"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.postUrl").value("https://www.instagram.com/p/Cabc123/"));
    }

    @Test
    void publish_rejectsMalformedInstagramUrl_returns400() throws Exception {
        mockMvc.perform(patch("/api/content/20/publish")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContentPublishRequest("https://example.com/not-instagram"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publish_propagates409_whenServiceThrowsInvalidStateTransition() throws Exception {
        when(contentService.publish(any(), any(), any()))
                .thenThrow(new InvalidStateTransitionException("Content can only be published once approved"));

        mockMvc.perform(patch("/api/content/20/publish")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContentPublishRequest("https://www.instagram.com/p/Cabc123/"))))
                .andExpect(status().isConflict());
    }
}
