package club.escobar.controller;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.entity.enums.UserRole;
import club.escobar.security.JwtAuthenticationFilter;
import club.escobar.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_returns201WithTokens() throws Exception {
        var response = new AuthResponse("access-token", "refresh-token",
                new UserSummaryResponse(1L, "creator@test.com", UserRole.CREATOR));
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "creator@test.com", "password123", UserRole.CREATOR, "Jamie Creator", null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("creator@test.com"));
    }

    @Test
    void register_rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "not-an-email", "password123", UserRole.CREATOR, "Jamie Creator", null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void login_returns200WithTokens() throws Exception {
        var response = new AuthResponse("access-token", "refresh-token",
                new UserSummaryResponse(2L, "biz@test.com", UserRole.BUSINESS));
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("biz@test.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("BUSINESS"));
    }
}
