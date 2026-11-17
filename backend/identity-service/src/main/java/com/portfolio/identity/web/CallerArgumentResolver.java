package com.portfolio.identity.web;

import com.portfolio.common.security.Caller;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lets any controller in any service declare a {@link Caller} parameter. Only the identity service knows
 * the token format, so only it translates a validated JWT into a Caller. Anonymous requests get null.
 * Registered with Spring MVC by {@link IdentityWebConfig}.
 */
@Component
public class CallerArgumentResolver implements HandlerMethodArgumentResolver {

    static final String ROLE_CLAIM = "role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Caller.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            return new Caller(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString(ROLE_CLAIM));
        }
        return null;
    }
}
