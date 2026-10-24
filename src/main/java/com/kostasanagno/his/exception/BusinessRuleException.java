package com.kostasanagno.his.exception;

/** A well-formed request that violates a domain rule (mapped to 400). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
