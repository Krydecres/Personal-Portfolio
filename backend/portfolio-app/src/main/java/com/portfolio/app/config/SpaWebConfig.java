package com.portfolio.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.time.Duration;

/**
 * Serves the React build (copied to classpath:/static/ by the Dockerfile) and makes client-side routes work:
 * a browser that opens /admin or /register directly gets index.html, React Router then shows the page.
 *
 * <p>Controllers are matched first, so /api/** never reaches this. Paths under api/ or with a file
 * extension that do not exist stay a real 404. In development static/ is empty and nothing changes.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String LOCATION = "classpath:/static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite puts a content hash in every file name under /assets: safe to cache for a year.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(LOCATION + "assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // index.html and the rest: always revalidate, so a new deploy is picked up at once.
        registry.addResourceHandler("/**")
                .addResourceLocations(LOCATION)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    private static final class SpaFallbackResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            boolean apiPath = resourcePath.startsWith("api/");
            boolean looksLikeFile = resourcePath.substring(resourcePath.lastIndexOf('/') + 1).contains(".");
            if (apiPath || looksLikeFile) {
                return null; // genuine 404
            }
            Resource index = location.createRelative("index.html");
            return index.exists() && index.isReadable() ? index : null;
        }
    }
}
