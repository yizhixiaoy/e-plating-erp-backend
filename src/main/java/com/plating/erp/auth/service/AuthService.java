package com.plating.erp.auth.service;

import com.plating.erp.auth.vo.AuthResponseVo;
import com.plating.erp.auth.vo.AuthVo;

import java.util.List;

public interface AuthService {
    List<AuthResponseVo.TenantSearchResult> searchTenants(String keyword, Integer limit);

    AuthResponseVo.TenantByUsernameResult getTenantByUsername(String username);

    AuthResponseVo.TenantByPhoneResult getTenantByPhone(String phone);

    List<AuthResponseVo.RecentTenantResult> getRecentTenants(String auth);

    AuthResponseVo.LoginResponse login(AuthVo.LoginReq req);

    void sendSmsCode(String phone, String tenantCode, String scene);

    void sendEmailCode(String email, String tenantCode, String scene);

    String refreshToken(String refreshToken);

    void resetPassword(AuthVo.ResetPasswordReq payload);

    void verifySmsCodeForForgotPassword(AuthVo.VerifySmsCodeReq payload);

    AuthResponseVo.UserInfoResult getCurrentUser(Long userId);

    void updateCurrentUser(Long userId, AuthVo.UpdateUserReq payload);

    void changePassword(Long userId, AuthVo.ChangePasswordReq payload);
}
