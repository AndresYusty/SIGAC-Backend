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
    /** Public base of the API (used in email download links). Example: http://localhost:8080/api/v1 */
    private String publicApiUrl = "http://localhost:8080/api/v1";
    /** Only true behind a trusted reverse proxy: trusts X-Forwarded-For for the signer's IP. */
    private boolean trustProxyHeaders = false;
    private Storage storage = new Storage();
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Mail mail = new Mail();
    private Seed seed = new Seed();

    @Getter @Setter
    public static class Storage {
        /** "local" (base-path directory) or "s3" (bucket). */
        private String type = "local";
        private String basePath = "./storage";
        private S3 s3 = new S3();
    }

    /** Credentials come from the AWS default chain (EC2 instance role or AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY). */
    @Getter @Setter
    public static class S3 {
        private String bucket;
        private String region;
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
        private boolean demoData = false;
    }
}
