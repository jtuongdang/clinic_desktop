package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.common.Query;

/** Trường `periodId`: ky mã. */
public record GetTienLuongTheoKyQuery(Integer periodId, Integer month, Integer year)
        implements Query<TienLuongTongHopDuLieu> {}
