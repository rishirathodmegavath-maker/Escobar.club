package club.escobar.service;

import club.escobar.dto.account.TwoFactorConfirmRequest;
import club.escobar.dto.account.TwoFactorDisableRequest;
import club.escobar.dto.account.TwoFactorSetupResponse;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.entity.User;

public interface TwoFactorService {

    TwoFactorSetupResponse setup(Long userId);

    UserSummaryResponse confirm(Long userId, TwoFactorConfirmRequest request);

    UserSummaryResponse disable(Long userId, TwoFactorDisableRequest request);

    boolean verifyCode(User user, String code);
}
