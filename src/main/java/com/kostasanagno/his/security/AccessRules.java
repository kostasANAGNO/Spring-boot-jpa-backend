package com.kostasanagno.his.security;

/** Reusable {@code @PreAuthorize} expressions so roles are not scattered as string literals. */
public final class AccessRules {

    public static final String ADMIN_ONLY = "hasRole('ADMIN')";
    public static final String ANY_STAFF = "hasAnyRole('ADMIN','DOCTOR','NURSE','RECEPTIONIST')";

    private AccessRules() {
    }
}
