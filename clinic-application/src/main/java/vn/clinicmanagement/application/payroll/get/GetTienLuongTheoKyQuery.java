package vn.clinicmanagement.application.payroll.get;

import vn.clinicmanagement.application.payroll.TienLuongTongHopDuLieu;

import vn.clinicmanagement.application.common.Query;

public record GetTienLuongTheoKyQuery(Integer periodId, Integer month, Integer year)
        implements Query<TienLuongTongHopDuLieu> {}
