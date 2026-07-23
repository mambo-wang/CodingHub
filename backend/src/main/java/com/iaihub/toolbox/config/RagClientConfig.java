package com.iaihub.toolbox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RagClientConfig {

    @Bean
    public HttpClient ragHttpClient() {
        // Force HTTP/1.1. The RAG service (uvicorn/Starlette) only speaks
        // HTTP/1.1; the JDK HttpClient defaults to HTTP/2 and, when talking to
        // an HTTP/1.1-only server, the request body gets mangled (RAG receives
        // an empty body and answers 400 "Invalid JSON body"). Pinning HTTP/1.1
        // keeps the request/response framing correct.
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
