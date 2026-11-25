package vn.clinicmanagement.application.hr.get;

import vn.clinicmanagement.application.hr.NhanVienTongHopDuLieu;

import vn.clinicmanagement.application.common.Query;

public record GetNhanVienQuery(Boolean active) implements Query<java.util.List<NhanVienTongHopDuLieu>> {}
