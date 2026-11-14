package vn.clinicmanagement.domain.patient;

import vn.clinicmanagement.domain.common.exceptions.MienNghiepVuNgoaiLe;

/** Raised when a patient identity is already assigned to another active record. */
public final class BenhNhanDinhDanhXungDotNgoaiLe extends MienNghiepVuNgoaiLe {
    public BenhNhanDinhDanhXungDotNgoaiLe() {
        super("Định danh người bệnh đã được sử dụng bởi một hồ sơ đang hoạt động.");
    }
}
