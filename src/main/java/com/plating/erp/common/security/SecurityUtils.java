package com.plating.erp.common.security;

import com.plating.erp.common.api.BizException;
import com.plating.erp.common.api.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
