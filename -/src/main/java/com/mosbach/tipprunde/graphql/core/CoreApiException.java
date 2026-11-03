package com.mosbach.tipprunde.graphql.core;

// Fehler des Core-Service im Format {"error":{"code","message"}}, mit dem HTTP-Status der Antwort
public class CoreApiException extends RuntimeException {

    private final int status;
    private final String code;

    public CoreApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
