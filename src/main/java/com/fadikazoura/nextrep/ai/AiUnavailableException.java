package com.fadikazoura.nextrep.ai;

/** The AI could not produce a usable answer (not configured, overloaded, timed out or invalid output). */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
