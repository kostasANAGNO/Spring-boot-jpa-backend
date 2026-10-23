package com.kostasanagno.his.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException of(String resource, String field, Object value) {
        return new DuplicateResourceException("%s with %s '%s' already exists".formatted(resource, field, value));
    }
}
