package vn.clinicmanagement.application.cls.get;

import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

public record GetDichVuTheoLuotKhamQuery(
        int visitId,
        PhanQuyenNguCanh sessionContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Query<List<DichVuYeuCauDuLieu>> {}
