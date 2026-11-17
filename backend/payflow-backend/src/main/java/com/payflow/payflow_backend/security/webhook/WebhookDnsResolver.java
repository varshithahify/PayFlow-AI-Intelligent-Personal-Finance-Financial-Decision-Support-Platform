package com.payflow.payflow_backend.security.webhook;

import org.apache.hc.client5.http.DnsResolver;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class WebhookDnsResolver implements DnsResolver {

    private final WebhookUrlValidator validator;

    public WebhookDnsResolver(WebhookUrlValidator validator) {
        this.validator = validator;
    }

    @Override
    public InetAddress[] resolve(String host)
            throws UnknownHostException {
        return validator.resolveAndValidate(host);
    }

    @Override
    public String resolveCanonicalHostname(String host)
            throws UnknownHostException {
        return host;
    }
}