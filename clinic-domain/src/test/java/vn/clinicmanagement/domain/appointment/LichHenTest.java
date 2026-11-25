package vn.clinicmanagement.domain.appointment;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNull;

class LichHenTest {

    @Test
    void constructorPreservesNullCreator() {
        LichHen appointment = new LichHen(
                1, 2, 3, null,
                LocalDateTime.of(2026, 1, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 9, 30),
                "Synthetic appointment", "LE_TAN", null);

        assertNull(appointment.getCreatedBy());
    }
}
