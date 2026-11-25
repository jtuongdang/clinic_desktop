package vn.clinicmanagement.application.billing.get;

import vn.clinicmanagement.application.billing.HoaDonDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

public record GetHoaDonTheoLuotKhamQuery(
        int visitId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Query<HoaDonDuLieu> {}
