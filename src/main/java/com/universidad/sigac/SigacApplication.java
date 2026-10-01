package com.universidad.sigac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SigacApplication {

    public static void main(String[] args) {
        SpringApplication.run(SigacApplication.class, args);
    }
}
