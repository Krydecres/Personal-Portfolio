package com.portfolio.identity.api;

import com.portfolio.common.security.Caller;
import com.portfolio.identity.api.AuthService.AuthResult;
import com.portfolio.identity.api.dto.IdentityDtos.ChangePasswordRequest;
import com.portfolio.identity.api.dto.IdentityDtos.UpdateAccountRequest;
import com.portfolio.identity.api.dto.IdentityDtos.UserResponse;

/** Service contract: the signed-in user's own account. */
public interface AccountService {

    UserResponse me(Caller caller);

    UserResponse update(Caller caller, UpdateAccountRequest request);

    /** Changes the password, revokes every existing session and returns a fresh one for the caller. */
    AuthResult changePassword(Caller caller, ChangePasswordRequest request);
}
