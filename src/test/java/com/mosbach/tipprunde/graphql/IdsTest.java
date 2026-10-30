package com.mosbach.tipprunde.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.mosbach.tipprunde.graphql.core.CoreApiException;
import com.mosbach.tipprunde.graphql.core.Ids;

class IdsTest {

    @Test
    void numbersAreAccepted() {
        assertEquals(3L, Ids.requireNumber("3", "Die Gruppen-ID"));
        assertEquals(123456789L, Ids.requireNumber("123456789", "Die Gruppen-ID"));
    }

    @Test
    void everythingElseIsRejectedWithValidationError() {
        for (String bad : new String[] { null, "", " 3", "3 ", "-1", "1.5", "abc", "3/../4", "3?x=1", "99999999999999999999" }) {
            CoreApiException e = assertThrows(CoreApiException.class, () -> Ids.requireNumber(bad, "Die Gruppen-ID"));
            assertEquals("VALIDATION_ERROR", e.getCode(), String.valueOf(bad));
            assertEquals(400, e.getStatus());
        }
    }
}
