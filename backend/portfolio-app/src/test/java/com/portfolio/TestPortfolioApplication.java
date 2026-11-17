package com.portfolio;

import org.springframework.boot.SpringApplication;

/**
 * Runs the real application against an in-memory H2 database, for trying the app without MySQL:
 * <pre>./mvnw install -DskipTests && ./mvnw -pl portfolio-app spring-boot:test-run</pre>
 * Data is lost on restart. Normal runs (spring-boot:run / java -jar) always use MySQL.
 */
public class TestPortfolioApplication {

    public static void main(String[] args) {
        SpringApplication.from(PortfolioApplication::main).withAdditionalProfiles("demo").run(args);
    }
}
