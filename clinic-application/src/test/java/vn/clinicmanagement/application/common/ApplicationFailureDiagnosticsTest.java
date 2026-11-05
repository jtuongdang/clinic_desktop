package vn.clinicmanagement.application.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationFailureDiagnosticsTest {
    private static final String CORRELATION_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void formattedRecordOmitsSyntheticSecretsFromMessagesAndCauses() {
        RuntimeException cause = new IllegalArgumentException(
                "SECRET_SQL_STATE SECRET_BIND_VALUE SECRET_PASSWORD secret.patient@example.invalid");
        RuntimeException failure = new IllegalStateException(
                "SYNTHETIC_PATIENT_NAME SYNTHETIC_PHONE SYNTHETIC_CCCD clinical-note", cause);

        String formatted = ApplicationFailureDiagnostics.format(
                "RECEPTION_SEARCH_PATIENTS", "PATIENT_SEARCH_FAILED", CORRELATION_ID, failure);

        assertTrue(formatted.contains("operation=RECEPTION_SEARCH_PATIENTS"));
        assertTrue(formatted.contains("result=PATIENT_SEARCH_FAILED"));
        assertTrue(formatted.contains("correlationId=" + CORRELATION_ID));
        assertTrue(formatted.contains("java.lang.IllegalStateException"));
        assertTrue(formatted.contains("java.lang.IllegalArgumentException"));
        assertFalse(formatted.contains("SYNTHETIC_PATIENT_NAME"));
        assertFalse(formatted.contains("SYNTHETIC_PHONE"));
        assertFalse(formatted.contains("SYNTHETIC_CCCD"));
        assertFalse(formatted.contains("SECRET_SQL_STATE"));
        assertFalse(formatted.contains("SECRET_BIND_VALUE"));
        assertFalse(formatted.contains("SECRET_PASSWORD"));
        assertFalse(formatted.contains("clinical-note"));
        assertFalse(formatted.contains("secret.patient@example.invalid"));
    }

    @Test
    void invalidCodesUseStableFallbacksAndInvalidCorrelationIsOmitted() {
        String formatted = ApplicationFailureDiagnostics.format(
                "bad operation; SYNTHETIC_NAME", "result=SECRET", "not-a-uuid-SYNTHETIC_PHONE",
                new IllegalStateException("synthetic message"));

        assertTrue(formatted.contains("operation=UNKNOWN_OPERATION"));
        assertTrue(formatted.contains("result=UNKNOWN_RESULT"));
        assertFalse(formatted.contains("correlationId="));
        assertFalse(formatted.contains("bad operation"));
        assertFalse(formatted.contains("SECRET"));
        assertFalse(formatted.contains("SYNTHETIC"));
    }

    @Test
    void formattedFramesAreSanitizedAndBoundedAcrossCauseChain() {
        RuntimeException cause = new IllegalArgumentException("synthetic cause");
        cause.setStackTrace(stackFrames(ApplicationFailureDiagnostics.MAX_STACK_FRAMES));
        RuntimeException failure = new IllegalStateException("synthetic failure", cause);
        failure.setStackTrace(stackFrames(ApplicationFailureDiagnostics.MAX_STACK_FRAMES + 3));

        String formatted = ApplicationFailureDiagnostics.format(
                "RECEPTION_GET_QUEUE", "RECEPTION_QUEUE_FAILED", CORRELATION_ID, failure);
        String frameList = formatted.substring(formatted.indexOf("frames=[") + "frames=[".length(),
                formatted.length() - 1);
        List<String> frames = frameList.isEmpty() ? List.of() : List.of(frameList.split(","));

        assertEquals(ApplicationFailureDiagnostics.MAX_STACK_FRAMES, frames.size());
        assertTrue(frames.get(0).startsWith("synthetic.SafeFrame#method0:"));
        assertTrue(frames.stream().allMatch(frame -> frame.matches(
                "synthetic\\.SafeFrame#method[0-9]+:-?[0-9]+")));
    }

    @Test
    void unsafeFrameNamesAreOmitted() {
        RuntimeException failure = new IllegalStateException("synthetic message");
        failure.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("synthetic.SafeFrame", "safeMethod", "SafeFrame.java", 12),
                new StackTraceElement("synthetic.SECRET_NAME;DROP", "phone:555-123-4567", null, 13)
        });

        String formatted = ApplicationFailureDiagnostics.format(
                "RECEPTION_GET_ACTIVE_ROOMS", "ROOM_CATALOG_FAILED", CORRELATION_ID, failure);

        assertTrue(formatted.contains("synthetic.SafeFrame#safeMethod:12"));
        assertFalse(formatted.contains("SECRET_NAME;DROP"));
        assertFalse(formatted.contains("phone:555-123-4567"));
    }

    @Test
    void formatterNeverCallsThrowableMessageOrToString() {
        assertDoesNotThrow(() -> ApplicationFailureDiagnostics.format(
                "RECEPTION_SEARCH_PATIENTS", "PATIENT_SEARCH_FAILED", CORRELATION_ID,
                new MessageAccessGuard()));
    }

    @Test
    void loggerFailureDoesNotEscapeAndLoggerReceivesOnlyFormattedText() {
        AtomicReference<String> loggedRecord = new AtomicReference<>();
        ApplicationFailureDiagnostics.logUnexpectedFailure("RECEPTION_REGISTER_PATIENT",
                "PATIENT_REGISTRATION_FAILED", CORRELATION_ID,
                new IllegalStateException("SYNTHETIC_PATIENT_NAME"), loggedRecord::set);

        assertNotNull(loggedRecord.get());
        assertFalse(loggedRecord.get().contains("SYNTHETIC_PATIENT_NAME"));

        assertDoesNotThrow(() -> ApplicationFailureDiagnostics.logUnexpectedFailure(
                "RECEPTION_REGISTER_PATIENT", "PATIENT_REGISTRATION_FAILED", CORRELATION_ID,
                new IllegalStateException("synthetic"), record -> {
                    throw new IllegalStateException("synthetic logger failure");
                }));
    }

    private static StackTraceElement[] stackFrames(int count) {
        List<StackTraceElement> frames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            frames.add(new StackTraceElement("synthetic.SafeFrame", "method" + i, "SafeFrame.java", 20 + i));
        }
        return frames.toArray(StackTraceElement[]::new);
    }

    private static final class MessageAccessGuard extends RuntimeException {
        @Override public String getMessage() { throw new AssertionError("message must not be read"); }
        @Override public String toString() { throw new AssertionError("throwable must not be stringified"); }
    }
}
