package com.payflow.payflow_backend.gateway;

public class GatewayResult {

    private final boolean success;
    private final String gatewayName;
    private final String message;

    private GatewayResult(
            boolean success,
            String gatewayName,
            String message
    ) {
        this.success = success;
        this.gatewayName = gatewayName;
        this.message = message;
    }

    public static GatewayResult success(
            String gatewayName,
            String message
    ) {
        return new GatewayResult(
                true,
                gatewayName,
                message
        );
    }

    public static GatewayResult failure(
            String gatewayName,
            String message
    ) {
        return new GatewayResult(
                false,
                gatewayName,
                message
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public String getMessage() {
        return message;
    }
}