package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root. Component scanning from {@code com.portfolio} picks up every service module:
 * identity ({@code com.portfolio.identity}), profile ({@code com.portfolio.profile}) and
 * content ({@code com.portfolio.content}).
 */
@SpringBootApplication
public class PortfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApplication.class, args);
    }
}
