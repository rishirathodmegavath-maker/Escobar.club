package club.escobar.service.impl;

import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.CreatorProfile;
import club.escobar.entity.RefreshToken;
import club.escobar.entity.User;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ApiException;
import club.escobar.exception.DuplicateResourceException;
import club.escobar.exception.InvalidCredentialsException;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CreatorProfileRepository;
import club.escobar.repository.RefreshTokenRepository;
import club.escobar.repository.UserRepository;
import club.escobar.security.JwtService;
import club.escobar.security.SecurityUser;
import club.escobar.security.TokenHasher;
import club.escobar.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot self-register as ADMIN");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (request.role() == UserRole.BUSINESS) {
            if (!StringUtils.hasText(request.gstNumber())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "GST Number is required");
            }
            if (!StringUtils.hasText(request.contactPersonName())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Contact Person Name is required");
            }
            if (!StringUtils.hasText(request.mobileNumber())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Mobile Number is required");
            }
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build());

        if (request.role() == UserRole.CREATOR) {
            creatorProfileRepository.save(CreatorProfile.builder()
                    .user(user)
                    .displayName(request.displayName())
                    .followerCount(0L)
                    .build());
        } else {
            businessProfileRepository.save(BusinessProfile.builder()
                    .user(user)
                    .companyName(request.displayName())
                    .gstNumber(request.gstNumber())
                    .contactPersonName(request.contactPersonName())
                    .mobileNumber(request.mobileNumber())
                    .build());
        }

        log.info("Registered new {} user id={}", user.getRole(), user.getId());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been deactivated");
        }

        log.info("User id={} logged in", user.getId());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (jwtService.isExpired(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token is expired");
        }

        String hash = TokenHasher.sha256(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is invalid"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Refresh token is invalid or has been revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String hash = TokenHasher.sha256(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueTokens(User user) {
        SecurityUser securityUser = new SecurityUser(user);
        String accessToken = jwtService.generateAccessToken(securityUser);
        String refreshTokenValue = jwtService.newRefreshTokenValue();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(refreshTokenValue))
                .expiresAt(jwtService.refreshTokenExpiry())
                .revoked(false)
                .build());

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole())
        );
    }
}
