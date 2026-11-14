package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Query;

/** Trường `active`: hoạt động. */
public record GetNhanVienQuery(Boolean active) implements Query<java.util.List<NhanVienTongHopDuLieu>> {}
