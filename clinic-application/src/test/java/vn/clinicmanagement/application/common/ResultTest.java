package vn.clinicmanagement.application.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void successWithValueReturnsSuccessfulResultWithValue() {
        Result<String> result = Result.success("synthetic-value");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Optional.of("synthetic-value"), result.getValue());
        assertEquals("synthetic-value", result.valueOrThrow());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    void successWithNullValueReturnsSuccessfulResultWithEmptyOptional() {
        Result<String> result = Result.success(null);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertNull(result.valueOrThrow());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    void failureWithCodeAndMessageReturnsFailureResultWithExpectedError() {
        Result<Integer> result = Result.failure("INVALID_INPUT", "Synthetic input error description");

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertTrue(result.getError().isPresent());

        Result.ApplicationError error = result.getError().get();
        assertEquals("INVALID_INPUT", error.code());
        assertEquals("Synthetic input error description", error.message());
    }

    @Test
    void failureMatchesApplicationErrorRecord() {
        Result.ApplicationError expectedError = new Result.ApplicationError("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");
        Result<String> result = Result.failure("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");

        assertTrue(result.isFailure());
        assertEquals(expectedError, result.getError().orElseThrow());
    }

    @Test
    void valueOrThrowOnFailureThrowsIllegalStateExceptionWithCodeAndMessage() {
        Result<String> result = Result.failure("ACCESS_DENIED", "Permission missing");

        IllegalStateException exception = assertThrows(IllegalStateException.class, result::valueOrThrow);
        assertEquals("ACCESS_DENIED: Permission missing", exception.getMessage());
    }

    @Test
    void applicationErrorRecordValidatesNonNullCodeAndMessage() {
        Result.ApplicationError error = new Result.ApplicationError("CODE_01", "Valid message");
        assertEquals("CODE_01", error.code());
        assertEquals("Valid message", error.message());

        assertThrows(NullPointerException.class, () -> new Result.ApplicationError(null, "Valid message"));
        assertThrows(NullPointerException.class, () -> new Result.ApplicationError("CODE_01", null));
    }

    @Test
    void failureMethodValidatesNonNullCodeAndMessage() {
        assertThrows(NullPointerException.class, () -> Result.failure(null, "Valid message"));
        assertThrows(NullPointerException.class, () -> Result.failure("CODE_01", null));
    }

    @Test
    void applicationErrorRecordSupportsEqualityAndHashCode() {
        Result.ApplicationError err1 = new Result.ApplicationError("CODE_01", "Message");
        Result.ApplicationError err2 = new Result.ApplicationError("CODE_01", "Message");
        Result.ApplicationError err3 = new Result.ApplicationError("CODE_02", "Message");
        Result.ApplicationError err4 = new Result.ApplicationError("CODE_01", "Other message");

        assertEquals(err1, err2);
        assertEquals(err1.hashCode(), err2.hashCode());
        assertNotEquals(err1, err3);
        assertNotEquals(err1, err4);
        assertNotEquals(null, err1);
        assertNotNull(err1.toString());
    }
}
