package club.escobar.service.impl;

import club.escobar.config.TwoFactorProperties;
import club.escobar.dto.account.TwoFactorConfirmRequest;
import club.escobar.dto.account.TwoFactorDisableRequest;
import club.escobar.dto.account.TwoFactorSetupResponse;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.entity.User;
import club.escobar.exception.ApiException;
import club.escobar.exception.InvalidOtpException;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.repository.UserRepository;
import club.escobar.security.TwoFactorSecretCipher;
import club.escobar.service.TwoFactorService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {

    private final UserRepository userRepository;
    private final TwoFactorSecretCipher secretCipher;
    private final TwoFactorProperties properties;

    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    @Override
    @Transactional
    public TwoFactorSetupResponse setup(Long userId) {
        User user = findUser(userId);

        String secret = new DefaultSecretGenerator().generate();
        user.setTwoFactorSecret(secretCipher.encrypt(secret));
        userRepository.save(user);

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(properties.issuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        return new TwoFactorSetupResponse(secret, data.getUri());
    }

    @Override
    @Transactional
    public UserSummaryResponse confirm(Long userId, TwoFactorConfirmRequest request) {
        User user = findUser(userId);
        if (!StringUtils.hasText(user.getTwoFactorSecret())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start two-factor setup before confirming a code");
        }
        if (!verifyCode(user, request.code())) {
            throw new InvalidOtpException("Invalid or expired code");
        }

        user.setTwoFactorEnabled(true);
        user = userRepository.save(user);
        return toSummary(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse disable(Long userId, TwoFactorDisableRequest request) {
        User user = findUser(userId);
        if (!user.isTwoFactorEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Two-factor authentication is not enabled");
        }
        if (!verifyCode(user, request.code())) {
            throw new InvalidOtpException("Invalid or expired code");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user = userRepository.save(user);
        return toSummary(user);
    }

    @Override
    public boolean verifyCode(User user, String code) {
        if (!StringUtils.hasText(user.getTwoFactorSecret())) {
            return false;
        }
        String secret = secretCipher.decrypt(user.getTwoFactorSecret());
        return codeVerifier.isValidCode(secret, code);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole(),
                StringUtils.hasText(user.getPasswordHash()), user.isTwoFactorEnabled());
    }
}
