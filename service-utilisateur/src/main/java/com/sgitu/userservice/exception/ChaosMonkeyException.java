package com.sgitu.userservice.exception;

/**
 * Thrown by ChaosMonkeyService to simulate a dependency failure.
 */
public class ChaosMonkeyException extends RuntimeException {

    public ChaosMonkeyException(String serviceName) {
        super("[CHAOS MONKEY] " + serviceName + " is simulated as DOWN");
    }
}
