package com.university.sigac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables @Async (sending emails does not block the user's request). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
