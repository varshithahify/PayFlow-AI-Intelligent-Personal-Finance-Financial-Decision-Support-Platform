package com.payflow.payflow_backend.security;

public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_ORG_ID =
            new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setOrgId(Long orgId) {
        CURRENT_ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return CURRENT_ORG_ID.get();
    }

    public static Long requireOrgId() {
        Long orgId = CURRENT_ORG_ID.get();

        if (orgId == null) {
            throw new IllegalStateException(
                    "Authenticated organization context is missing"
            );
        }

        return orgId;
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
    }
}