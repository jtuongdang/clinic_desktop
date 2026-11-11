package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DatabaseResetToolTest {

    @Test
    void resetTransactionalDataFailsGracefullyWhenConnectionThrows() {
        boolean result = DatabaseResetTool.resetTransactionalData(() -> {
            throw new SQLException("Synthetic connection failure for testing");
        });
        assertFalse(result, "Reset tool should fail gracefully when connection is unavailable");
    }
}
