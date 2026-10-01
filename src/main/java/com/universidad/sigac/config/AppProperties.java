package com.universidad.sigac.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades propias del sistema (prefijo "sigac" en application.yml). */
@Getter
@Setter
@ConfigurationProperties(prefix = "sigac")
public class AppProperties {

    private String zone = "America/Bogota";
    private String frontendUrl = "http://localhost:4200";
    /** Solo true detrás de un proxy inverso propio: confía en X-Forwarded-For para la IP del firmante. */
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

    /** El servidor SMTP se configura con spring.mail.*; aquí solo el interruptor y el remitente. */
    @Getter @Setter
    public static class Mail {
        private boolean enabled = false;
        private String from = "sigac@universidad.edu";
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
