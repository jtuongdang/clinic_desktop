package vn.clinicmanagement.application.common;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KetQuaTest {

    @Test
    void successWithValueReturnsSuccessfulResultWithValue() {
        KetQua<String> result = KetQua.success("synthetic-value");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals(Optional.of("synthetic-value"), result.getValue());
        assertEquals("synthetic-value", result.valueOrThrow());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    void successWithNullValueReturnsSuccessfulResultWithEmptyOptional() {
        KetQua<String> result = KetQua.success(null);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertNull(result.valueOrThrow());
        assertTrue(result.getError().isEmpty());
    }

    @Test
    void failureWithCodeAndMessageReturnsFailureResultWithExpectedError() {
        KetQua<Integer> result = KetQua.failure("INVALID_INPUT", "Synthetic input error description");

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertTrue(result.getValue().isEmpty());
        assertTrue(result.getError().isPresent());

        KetQua.ApplicationError error = result.getError().get();
        assertEquals("INVALID_INPUT", error.code());
        assertEquals("Synthetic input error description", error.message());
    }

    @Test
    void failureMatchesApplicationErrorRecord() {
        KetQua.ApplicationError expectedError = new KetQua.ApplicationError("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");
        KetQua<String> result = KetQua.failure("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");

        assertTrue(result.isFailure());
        assertEquals(expectedError, result.getError().orElseThrow());
    }

    @Test
    void valueOrThrowOnFailureThrowsIllegalStateExceptionWithCodeAndMessage() {
        KetQua<String> result = KetQua.failure("ACCESS_DENIED", "Permission missing");

        IllegalStateException exception = assertThrows(IllegalStateException.class, result::valueOrThrow);
        assertEquals("ACCESS_DENIED: Permission missing", exception.getMessage());
    }

    @Test
    void applicationErrorRecordValidatesNonNullCodeAndMessage() {
        KetQua.ApplicationError error = new KetQua.ApplicationError("CODE_01", "Valid message");
        assertEquals("CODE_01", error.code());
        assertEquals("Valid message", error.message());

        assertThrows(NullPointerException.class, () -> new KetQua.ApplicationError(null, "Valid message"));
        assertThrows(NullPointerException.class, () -> new KetQua.ApplicationError("CODE_01", null));
    }

    @Test
    void failureMethodValidatesNonNullCodeAndMessage() {
        assertThrows(NullPointerException.class, () -> KetQua.failure(null, "Valid message"));
        assertThrows(NullPointerException.class, () -> KetQua.failure("CODE_01", null));
    }

    @Test
    void applicationErrorRecordSupportsEqualityAndHashCode() {
        KetQua.ApplicationError err1 = new KetQua.ApplicationError("CODE_01", "Message");
        KetQua.ApplicationError err2 = new KetQua.ApplicationError("CODE_01", "Message");
        KetQua.ApplicationError err3 = new KetQua.ApplicationError("CODE_02", "Message");
        KetQua.ApplicationError err4 = new KetQua.ApplicationError("CODE_01", "Other message");

        assertEquals(err1, err2);
        assertEquals(err1.hashCode(), err2.hashCode());
        assertNotEquals(err1, err3);
        assertNotEquals(err1, err4);
        assertNotEquals(null, err1);
        assertNotNull(err1.toString());
    }
}
