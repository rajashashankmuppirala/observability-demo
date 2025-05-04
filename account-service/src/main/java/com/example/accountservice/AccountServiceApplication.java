package com.example.accountservice;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner initializeApplication(OpenTelemetry openTelemetry) {
        return args -> {
            // Log service startup with OpenTelemetry info
            String serviceName = openTelemetry.getSdkContextStorage()
                    .toString();
            
            log.info("Account Service started successfully");
            log.info("OpenTelemetry initialized with context propagators: {}", 
                     openTelemetry.getPropagators().getClass().getSimpleName());
            log.info("Service is ready to accept requests");
        };
    }
}
