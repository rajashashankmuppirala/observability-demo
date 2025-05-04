package com.example.userservice.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ObservabilityConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenTelemetry openTelemetry() {
        // Create resource with service information
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, applicationName,
                        ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )));

        // Create console exporter for development (logs spans to console)
        SpanExporter loggingExporter = new LoggingSpanExporter();

        // Create OTLP exporter for production
        SpanExporter otlpExporter = OtlpHttpSpanExporter.builder()
                // Configure endpoint for backend (e.g., Jaeger, Zipkin, etc.)
                .setEndpoint("http://localhost:4318/v1/traces")
                .build();

        // Set up the tracer provider with desired exporters and sampler
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(Sampler.alwaysOn()) // Or use parentBased, traceIdRatioBased, etc.
                .addSpanProcessor(BatchSpanProcessor.builder(loggingExporter).build())
                .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
                .build();

        // Create the OpenTelemetry instance
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    public io.opentelemetry.api.trace.Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(applicationName);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        // Configure RestTemplate with tracing interceptors
        return new RestTemplate();
    }
}
