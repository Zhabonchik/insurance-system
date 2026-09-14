package org.insurance.insuranceservice.registry.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "registry.emulator")
public record RegistryEmulatorProperties(
    String baseUrl, String registerPath, Duration connectTimeout, Duration readTimeout) {}
