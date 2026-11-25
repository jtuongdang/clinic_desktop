package vn.clinicmanagement.application.cls.request;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record YeuCauDichVuCommand(
        int visitId,
        int serviceId,
        BigDecimal quantity,
        Integer roomId,
        String doctorNotes,
        PhanQuyenNguCanh sessionContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<YeuCauDichVuBienNhan> {}
