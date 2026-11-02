package com.mosbach.tipprunde.graphql.core;

// Prüft IDs aus GraphQL-Argumenten, bevor sie in einen Pfad des Core eingesetzt werden
public final class Ids {

    private Ids() {
    }

    // IDs sind Zahlen, alles andere wird abgewiesen
    public static long requireNumber(String id, String what) {
        if (id == null || !id.matches("\\d{1,18}")) {
            throw new CoreApiException(400, "VALIDATION_ERROR", what + " muss eine Zahl sein.");
        }
        return Long.parseLong(id);
    }
}
