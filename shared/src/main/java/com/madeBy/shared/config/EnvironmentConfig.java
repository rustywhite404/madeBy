package com.madeBy.shared.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class EnvironmentConfig {
    private final Dotenv dotenv;

    public EnvironmentConfig() {
        String envFile = Files.exists(Paths.get("prod.env")) ? "prod.env" : ".env";
        this.dotenv = Dotenv.configure()
                .filename(envFile)
                .load();
    }

    public String getJwtSecretKey() {
        return dotenv.get("JWT_SECRET_KEY");
    }

    public String getAdminToken() {
        return dotenv.get("ADMIN_TOKEN");
    }
}