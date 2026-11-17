package com.portfolio.identity.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Spring MVC configuration owned by the identity service: registers the {@link CallerArgumentResolver}. */
@Configuration
public class IdentityWebConfig implements WebMvcConfigurer {

    private final CallerArgumentResolver callerArgumentResolver;

    public IdentityWebConfig(CallerArgumentResolver callerArgumentResolver) {
        this.callerArgumentResolver = callerArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(callerArgumentResolver);
    }
}
