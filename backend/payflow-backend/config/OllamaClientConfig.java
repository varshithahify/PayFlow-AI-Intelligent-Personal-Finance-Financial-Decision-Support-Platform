package com.payflow.payflow_backend.config;

import java.time.Duration;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaClientConfig {

    @Bean
    public OllamaApi ollamaApi(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
            String baseUrl) {

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory();

        requestFactory.setReadTimeout(Duration.ofMinutes(5));

        RestClient.Builder restClientBuilder =
                RestClient.builder()
                        .requestFactory(requestFactory);

        return OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(restClientBuilder)
                .build();
    }
}

