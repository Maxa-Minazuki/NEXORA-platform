package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NexoraApplication {

    static {
        // All instants are stored as UTC wall-clock values; a UTC JVM default removes any driver ambiguity.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        // User-facing API texts are Kazakh regardless of the host OS locale.
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("kk-KZ"));
    }

    public static void main(String[] args) {
        SpringApplication.run(NexoraApplication.class, args);
    }
}
