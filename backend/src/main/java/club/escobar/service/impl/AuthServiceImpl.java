package club.escobar.service.impl;

import club.escobar.config.EmailVerificationProperties;
import club.escobar.config.PasswordResetProperties;
import club.escobar.dto.auth.AuthResponse;
import club.escobar.dto.auth.ForgotPasswordRequest;
import club.escobar.dto.auth.GoogleAuthRequest;
import club.escobar.dto.auth.LoginRequest;
import club.escobar.dto.auth.RegisterRequest;
import club.escobar.dto.auth.RegisterResponse;
import club.escobar.dto.auth.ResetPasswordRequest;
import club.escobar.dto.auth.SetPasswordRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.dto.auth.VerifyEmailRequest;
import club.escobar.entity.BusinessProfile;
import club.escobar.entity.CreatorProfile;
import club.escobar.entity.EmailVerificationToken;
import club.escobar.entity.PasswordResetToken;
import club.escobar.entity.RefreshToken;
import club.escobar.entity.User;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ApiException;
import club.escobar.exception.DuplicateResourceException;
import club.escobar.exception.InvalidCredentialsException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.notification.AuthEmailSender;
import club.escobar.repository.BusinessProfileRepository;
import club.escobar.repository.CreatorProfileRepository;
import club.escobar.repository.EmailVerificationTokenRepository;
import club.escobar.repository.PasswordResetTokenRepository;
import club.escobar.repository.RefreshTokenRepository;
import club.escobar.repository.UserRepository;
import club.escobar.security.GoogleIdTokenVerifierService;
import club.escobar.security.JwtService;
import club.escobar.security.SecurityUser;
import club.escobar.security.TokenHasher;
import club.escobar.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;
    private final AuthEmailSender authEmailSender;
    private final PasswordResetProperties passwordResetProperties;
    private final EmailVerificationProperties emailVerificationProperties;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot self-register as ADMIN");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (request.role() == UserRole.BUSINESS) {
            validateBusinessFields(request.gstNumber(), request.contactPersonName(), request.mobileNumber());
        }

        User user = userRepository.save(User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .emailVerified(false)
                .build());

        createProfileForNewUser(user, request.role(), request.displayName(), request.gstNumber(),
                request.contactPersonName(), request.mobileNumber());

        issueVerificationEmail(user);

        log.info("Registered new {} user id={}, pending email verification", user.getRole(), user.getId());
        return new RegisterResponse("Registration successful. Please check your email to verify your account.");
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
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your email before signing in");
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

    @Override
    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleIdTokenVerifierService.verify(request.idToken());
        String googleId = payload.getSubject();
        String email = payload.getEmail();

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElse(null);

        if (user != null) {
            if (!user.isActive()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This account has been deactivated");
            }
            boolean needsSave = false;
            if (!StringUtils.hasText(user.getGoogleId())) {
                user.setGoogleId(googleId);
                needsSave = true;
            }
            if (!user.isEmailVerified()) {
                // Google has already authenticated ownership of this email address.
                user.setEmailVerified(true);
                needsSave = true;
            }
            if (needsSave) {
                user = userRepository.save(user);
            }
            log.info("User id={} logged in via Google", user.getId());
            return issueTokens(user);
        }

        if (request.role() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No account found for this Google email. Please sign up first.");
        }
        if (request.role() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot self-register as ADMIN");
        }
        if (request.role() == UserRole.BUSINESS) {
            validateBusinessFields(request.gstNumber(), request.contactPersonName(), request.mobileNumber());
        }

        String displayName = StringUtils.hasText(request.displayName())
                ? request.displayName()
                : (String) payload.get("name");
        if (!StringUtils.hasText(displayName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Display name is required");
        }

        User newUser = userRepository.save(User.builder()
                .email(email)
                .googleId(googleId)
                .role(request.role())
                .active(true)
                .emailVerified(true)
                .build());

        createProfileForNewUser(newUser, request.role(), displayName, request.gstNumber(),
                request.contactPersonName(), request.mobileNumber());

        log.info("Registered new {} user id={} via Google", newUser.getRole(), newUser.getId());
        return issueTokens(newUser);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String rawToken = generateOpaqueToken();

            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.sha256(rawToken))
                    .expiresAt(Instant.now().plusSeconds(passwordResetProperties.tokenTtlMinutes() * 60))
                    .used(false)
                    .build());

            String link = passwordResetProperties.frontendResetUrlBase() + "?token=" + rawToken;
            authEmailSender.sendPasswordResetEmail(user, link);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = TokenHasher.sha256(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset completed for user id={}", user.getId());
    }

    @Override
    @Transactional
    public UserSummaryResponse setPassword(Long userId, SetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (StringUtils.hasText(user.getPasswordHash())) {
            if (!StringUtils.hasText(request.currentPassword())
                    || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user = userRepository.save(user);
        log.info("Password set for user id={}", user.getId());

        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole(), true);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String hash = TokenHasher.sha256(request.token());
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This verification link is invalid or has expired"));

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This verification link is invalid or has expired");
        }

        User user = verificationToken.getUser();
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been deactivated");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified for user id={}", user.getId());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void resendVerification(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueVerificationEmail);
    }

    private void issueVerificationEmail(User user) {
        String rawToken = generateOpaqueToken();

        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(rawToken))
                .expiresAt(Instant.now().plusSeconds(emailVerificationProperties.tokenTtlHours() * 3600))
                .used(false)
                .build());

        String link = emailVerificationProperties.frontendVerifyUrlBase() + "?token=" + rawToken;
        authEmailSender.sendVerificationEmail(user, link);
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void createProfileForNewUser(User user, UserRole role, String displayName, String gstNumber,
                                          String contactPersonName, String mobileNumber) {
        if (role == UserRole.CREATOR) {
            creatorProfileRepository.save(CreatorProfile.builder()
                    .user(user)
                    .displayName(displayName)
                    .followerCount(0L)
                    .build());
        } else {
            businessProfileRepository.save(BusinessProfile.builder()
                    .user(user)
                    .companyName(displayName)
                    .gstNumber(gstNumber)
                    .contactPersonName(contactPersonName)
                    .mobileNumber(mobileNumber)
                    .build());
        }
    }

    private void validateBusinessFields(String gstNumber, String contactPersonName, String mobileNumber) {
        if (!StringUtils.hasText(gstNumber)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GST Number is required");
        }
        if (!StringUtils.hasText(contactPersonName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Contact Person Name is required");
        }
        if (!StringUtils.hasText(mobileNumber)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mobile Number is required");
        }
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
                new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole(), StringUtils.hasText(user.getPasswordHash()))
        );
    }
}
