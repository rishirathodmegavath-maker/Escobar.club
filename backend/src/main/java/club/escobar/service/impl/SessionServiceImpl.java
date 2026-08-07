package club.escobar.service.impl;

import club.escobar.dto.account.SessionResponse;
import club.escobar.exception.ResourceNotFoundException;
import club.escobar.repository.RefreshTokenRepository;
import club.escobar.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> list(Long userId) {
        return refreshTokenRepository
                .findAllByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now())
                .stream()
                .map(token -> new SessionResponse(
                        token.getId(),
                        token.getIpAddress(),
                        token.getUserAgent(),
                        token.getCreatedAt(),
                        token.getLastUsedAt(),
                        token.getExpiresAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(Long userId, Long sessionId) {
        int updated = refreshTokenRepository.revokeForUser(sessionId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Session not found");
        }
    }
}
