package vn.clinicmanagement.application.common;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Emits bounded diagnostics without exposing exception messages or business data. */
public final class UngDungThatBaiChanDoanLoi {
    static final int MAX_STACK_FRAMES = 8;
    private static final int MAX_EXCEPTION_TYPES = 5;
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Pattern UUID_TEXT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern CLASS_NAME =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final Pattern METHOD_NAME = Pattern.compile("<init>|<clinit>|[A-Za-z_$][A-Za-z0-9_$]*");
    private static final String UNKNOWN_OPERATION = "UNKNOWN_OPERATION";
    private static final String UNKNOWN_RESULT = "UNKNOWN_RESULT";
    private static final Logger LOGGER = Logger.getLogger(UngDungThatBaiChanDoanLoi.class.getName());

    private UngDungThatBaiChanDoanLoi() {}

    /** Hàm `logUnexpectedFailure`: nhatky `Unexpected` thất bại. */
    public static void logUnexpectedFailure(String operationCode, String resultCode,
                                            String correlationId, Throwable failure) {
        logUnexpectedFailure(operationCode, resultCode, correlationId, failure,
                record -> LOGGER.warning(record));
    }

    /** Hàm `logUnexpectedFailure`: nhatky `Unexpected` thất bại. */
    static void logUnexpectedFailure(String operationCode, String resultCode,
                                     String correlationId, Throwable failure, Consumer<String> logger) {
        try {
            logger.accept(format(operationCode, resultCode, correlationId, failure));
        } catch (RuntimeException ignored) {
            // Diagnostics must never alter the application result.
        }
    }

    static String format(String operationCode, String resultCode, String correlationId, Throwable failure) {
        StringBuilder record = new StringBuilder()
                .append("operation=").append(sanitizeCode(operationCode, UNKNOWN_OPERATION))
                .append(" result=").append(sanitizeCode(resultCode, UNKNOWN_RESULT));
        String validCorrelationId = sanitizeCorrelationId(correlationId);
        if (validCorrelationId != null) record.append(" correlationId=").append(validCorrelationId);

        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        StringBuilder exceptionTypes = new StringBuilder("[");
        StringBuilder frames = new StringBuilder("[");
        int exceptionCount = 0;
        int frameCount = 0;
        Throwable current = failure;
        while (current != null && visited.add(current) && exceptionCount < MAX_EXCEPTION_TYPES) {
            if (exceptionCount > 0) exceptionTypes.append(',');
            exceptionTypes.append(sanitizeClassName(current.getClass().getName()));
            exceptionCount++;

            if (frameCount < MAX_STACK_FRAMES) {
                StackTraceElement[] stack = current.getStackTrace();
                if (stack != null) {
                    for (StackTraceElement frame : stack) {
                        if (frameCount >= MAX_STACK_FRAMES) break;
                        String safeFrame = formatFrame(frame);
                        if (safeFrame == null) continue;
                        if (frameCount > 0) frames.append(',');
                        frames.append(safeFrame);
                        frameCount++;
                    }
                }
            }
            current = current.getCause();
        }
        record.append(" exceptions=").append(exceptionTypes.append(']'))
                .append(" frames=").append(frames.append(']'));
        return record.toString();
    }

    /** Hàm `sanitizeCode`: `sanitize` mã. */
    private static String sanitizeCode(String value, String fallback) {
        return value != null && CODE.matcher(value).matches() ? value : fallback;
    }

    /** Hàm `sanitizeCorrelationId`: `sanitize` `Correlation` mã. */
    private static String sanitizeCorrelationId(String value) {
        if (value == null || !UUID_TEXT.matcher(value).matches()) return null;
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Hàm `sanitizeClassName`: `sanitize` `Class` tên. */
    private static String sanitizeClassName(String value) {
        return value != null && value.length() <= 160 && CLASS_NAME.matcher(value).matches()
                ? value : "unknown";
    }

    /** Hàm `formatFrame`: `format` cuaso. */
    private static String formatFrame(StackTraceElement frame) {
        if (frame == null) return null;
        String className = frame.getClassName();
        String methodName = frame.getMethodName();
        if (className == null || className.length() > 160 || !CLASS_NAME.matcher(className).matches()
                || methodName == null || methodName.length() > 160
                || !METHOD_NAME.matcher(methodName).matches()) {
            return null;
        }
        return className + "#" + methodName + ":" + frame.getLineNumber();
    }
}
