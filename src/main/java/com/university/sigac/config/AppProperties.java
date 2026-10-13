package com.university.sigac.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** System-specific properties ("sigac" prefix in application.yml). */
@Getter
@Setter
@ConfigurationProperties(prefix = "sigac")
public class AppProperties {

    private String zone = "America/Bogota";
    private String frontendUrl = "http://localhost:4200";
    /** Only true behind a trusted reverse proxy: trusts X-Forwarded-For for the signer's IP. */
    private boolean trustProxyHeaders = false;
    private Storage storage = new Storage();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Mail mail = new Mail();
    private Seed seed = new Seed();

    @Getter @Setter
    public static class Storage {
        private String basePath = "./storage";
    }

    @Getter @Setter
    public static class Jwt {
        private String secret;
        private int expirationHours = 8;
    }

    @Getter @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200", "http://127.0.0.1:4200");
    }

    /** The SMTP server is configured with spring.mail.*; only the switch and the sender live here. */
    @Getter @Setter
    public static class Mail {
        private boolean enabled = false;
        private String from = "sigac@university.edu";
    }

    @Getter @Setter
    public static class Seed {
        private String adminEmail = "admin@sigac.local";
        private String adminPassword = "Admin12345*";
        private String devEmail = "dev@sigac.local";
        private String devPassword = "Dev12345*";
        private boolean demoData = false;
    }
}
