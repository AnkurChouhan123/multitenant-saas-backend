package com.saas.platform.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;


//  RestTemplateConfig - Configuration for RestTemplate
//  Provides proper timeout and connection pooling settings

@Configuration
public class RestTemplateConfig {
    
    
     // Create RestTemplate bean with proper timeouts
     // Used by WebhookService for HTTP requests
     
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .requestFactory(this::clientHttpRequestFactory)
            .build();
    }
    
    
      //  configure HTTP client factory with timeouts
     
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds connection timeout
        factory.setReadTimeout(30000);    // 30 seconds read timeout
        return factory;
    }
}