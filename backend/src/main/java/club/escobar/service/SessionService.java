package club.escobar.service;

import club.escobar.dto.account.SessionResponse;

import java.util.List;

public interface SessionService {

    List<SessionResponse> list(Long userId);

    void revoke(Long userId, Long sessionId);
}
