package com.portfolio.identity.internal.service;

import com.portfolio.common.error.ServiceException;
import com.portfolio.common.security.Caller;
import com.portfolio.common.util.Slugs;
import com.portfolio.identity.api.AccountService;
import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.api.dto.IdentityDtos.ChangePasswordRequest;
import com.portfolio.identity.api.dto.IdentityDtos.UpdateAccountRequest;
import com.portfolio.identity.api.dto.IdentityDtos.UserResponse;
import com.portfolio.identity.internal.domain.RefreshToken.RevokeReason;
import com.portfolio.identity.internal.domain.UserAccount;
import com.portfolio.identity.internal.domain.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountServiceImpl implements AccountService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenManager refreshTokens;
    private final SessionIssuer sessions;

    AccountServiceImpl(UserAccountRepository users, PasswordEncoder passwordEncoder,
                       RefreshTokenManager refreshTokens, SessionIssuer sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Caller caller) {
        return UserMapper.toResponse(load(caller));
    }

    @Override
    @Transactional
    public UserResponse update(Caller caller, UpdateAccountRequest request) {
        UserAccount user = load(caller);
        user.setDisplayName(request.displayName().trim());
        user.setAvatarUrl(Slugs.clean(request.avatarUrl()));
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public AuthResult changePassword(Caller caller, ChangePasswordRequest request) {
        UserAccount user = load(caller);
        if (user.getPasswordHash() != null) {
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw ServiceException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
            }
            if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
                throw ServiceException.badRequest("SAME_PASSWORD", "Mật khẩu mới phải khác mật khẩu cũ");
            }
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw ServiceException.badRequest("PASSWORD_MISMATCH", "Mật khẩu nhập lại không khớp");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Sign out every other device: a password change is often a reaction to a suspected compromise.
        refreshTokens.revokeAll(user.getId(), RevokeReason.PASSWORD_CHANGED);
        return sessions.newSession(user);
    }

    private UserAccount load(Caller caller) {
        if (caller == null || caller.userId() == null) {
            throw ServiceException.unauthorized("UNAUTHENTICATED", "Bạn cần đăng nhập");
        }
        return users.findById(caller.userId())
                .orElseThrow(() -> ServiceException.unauthorized("UNAUTHENTICATED", "Bạn cần đăng nhập"));
    }
}
