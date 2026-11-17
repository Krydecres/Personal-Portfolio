package com.portfolio.identity.web;

import com.portfolio.common.error.ServiceException;
import com.portfolio.identity.internal.config.IdentityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * CSRF defence for the endpoints that authenticate with the refresh cookie (refresh, logout).
 * SameSite=Strict already stops browsers from attaching the cookie cross-site; this check is a second
 * layer: if the browser says the request comes from an origin we do not know, reject it.
 */
@Component
public class OriginGuard {

    private final IdentityProperties props;

    public OriginGuard(IdentityProperties props) {
        this.props = props;
    }

    public void check(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return; // non-browser client, or same-origin GET: no ambient-credential CSRF risk
        }
        boolean allowed = props.getAllowedOrigins().stream().anyMatch(o -> o.equalsIgnoreCase(origin));
        if (!allowed) {
            throw ServiceException.forbidden("Yêu cầu bị từ chối: nguồn gửi không hợp lệ");
        }
    }
}
