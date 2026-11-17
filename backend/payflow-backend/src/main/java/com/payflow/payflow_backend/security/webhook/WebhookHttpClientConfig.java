package com.payflow.payflow_backend.security.webhook;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class WebhookHttpClientConfig {

    @Bean(destroyMethod = "close")
    public CloseableHttpClient webhookHttpClient(
            WebhookDnsResolver dnsResolver) {

        ConnectionConfig connectionConfig =
                ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(5))
                        .setSocketTimeout(Timeout.ofSeconds(10))
                        .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(dnsResolver)
                        .setDefaultConnectionConfig(connectionConfig)
                        .setMaxConnTotal(50)
                        .setMaxConnPerRoute(10)
                        .build();

        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setResponseTimeout(Timeout.ofSeconds(10))
                        .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .build();
    }
}