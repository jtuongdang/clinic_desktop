package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.util.List;

/** Protected active doctor team-queue request for one selected work scope. */
public record GetBacSiHangDoiQuery(LocalDate date, PhanQuyenNguCanh authorizationContext,
                                  /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
                                  KiemToanTacNhanNguCanh auditActorContext) implements Query<List<TiepDonHangDoiMatHang>> {}
