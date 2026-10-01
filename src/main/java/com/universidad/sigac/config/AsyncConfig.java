package com.universidad.sigac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async (el envío de correos no bloquea la petición del usuario). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
