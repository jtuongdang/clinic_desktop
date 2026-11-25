package vn.clinicmanagement.application.attendance;

import java.util.ArrayList;
import java.util.List;

/**
 * Audit port ghi nhận phục vụ kiểm thử in-memory.
 */
public class GhiNhanChamCongKiemToanCongKetNoi implements KiemToanWriteCongKetNoi {

    public final List<ChamCongKiemToanNhapLieu> entries = new ArrayList<>();

    @Override
    public void recordAudit(ChamCongKiemToanNhapLieu entry) {
        entries.add(entry);
    }
}
