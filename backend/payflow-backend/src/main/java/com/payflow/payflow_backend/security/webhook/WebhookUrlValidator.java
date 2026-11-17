package com.payflow.payflow_backend.security.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

@Component
public class WebhookUrlValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "metadata.google.internal",
            "metadata"
    );

    private final boolean allowDockerHost;

    public WebhookUrlValidator(
            @Value("${app.webhook.allow-host-docker-internal:false}")
            boolean allowDockerHost) {
        this.allowDockerHost = allowDockerHost;
    }

    public URI validate(String endpoint) {
        try {
            URI uri = URI.create(endpoint);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                throw new IllegalArgumentException("Invalid webhook URL");
            }

            if (uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException("Invalid webhook URL");
            }

            boolean dockerHost = isDockerDevelopmentEndpoint(uri);

            if (!"https".equalsIgnoreCase(scheme)
                    && !(dockerHost && "http".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(
                        "Webhook endpoints must use HTTPS");
            }

            if (BLOCKED_HOSTS.contains(host.toLowerCase())) {
                throw new IllegalArgumentException(
                        "Webhook destination is not allowed");
            }

            if (dockerHost) {
                return uri;
            }

            if (uri.getPort() != -1
                    && uri.getPort() != 443) {
                throw new IllegalArgumentException(
                        "Webhook port is not allowed");
            }

            validateAddresses(host);

            return uri;

        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid webhook destination", exception);
        }
    }

    public InetAddress[] resolveAndValidate(String host)
            throws UnknownHostException {

        if (allowDockerHost
                && "host.docker.internal".equalsIgnoreCase(host)) {
            return InetAddress.getAllByName(host);
        }

        InetAddress[] addresses = InetAddress.getAllByName(host);

        if (addresses.length == 0) {
            throw new UnknownHostException("No DNS addresses found");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new UnknownHostException(
                        "Webhook destination resolves to a restricted address");
            }
        }

        return addresses;
    }

    private void validateAddresses(String host)
            throws UnknownHostException {

        for (InetAddress address : resolveAndValidate(host)) {
            if (isBlockedAddress(address)) {
                throw new UnknownHostException(
                        "Restricted webhook destination");
            }
        }
    }

    private boolean isDockerDevelopmentEndpoint(URI uri) {
        return allowDockerHost
                && "host.docker.internal".equalsIgnoreCase(uri.getHost())
                && "http".equalsIgnoreCase(uri.getScheme())
                && uri.getPort() == 8765;
    }

    private boolean isBlockedAddress(InetAddress address) {

        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;

            return first == 0
                    || first == 10
                    || first == 127
                    || first >= 224
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 192 && second == 0)
                    || (first == 198 && (second == 18 || second == 19));
        }

        if (bytes.length == 16) {
            int first = bytes[0] & 0xff;

            return (first & 0xfe) == 0xfc
                    || first == 0xff;
        }

        return true;
    }
}