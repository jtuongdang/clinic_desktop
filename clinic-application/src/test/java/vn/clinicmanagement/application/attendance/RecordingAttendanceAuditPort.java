package vn.clinicmanagement.application.attendance;

import java.util.ArrayList;
import java.util.List;

/**
 * Audit port ghi nhận phục vụ kiểm thử in-memory.
 */
public class RecordingAttendanceAuditPort implements AuditWritePort {

    public final List<AttendanceAuditEntry> entries = new ArrayList<>();

    @Override
    public void recordAudit(AttendanceAuditEntry entry) {
        entries.add(entry);
    }
}
