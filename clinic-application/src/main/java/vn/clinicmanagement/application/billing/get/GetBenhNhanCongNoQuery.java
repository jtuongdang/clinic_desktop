package vn.clinicmanagement.application.billing.get;

import vn.clinicmanagement.application.billing.BenhNhanCongNoDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

public record GetBenhNhanCongNoQuery(
        int patientId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Query<List<BenhNhanCongNoDuLieu>> {}
