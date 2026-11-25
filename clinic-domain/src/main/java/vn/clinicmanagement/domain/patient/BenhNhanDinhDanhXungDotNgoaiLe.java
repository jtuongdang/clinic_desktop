package vn.clinicmanagement.domain.patient;

import vn.clinicmanagement.domain.common.exceptions.MienNghiepVuNgoaiLe;

/**
 * Được ném khi danh tính bệnh nhân đã được gán cho một hồ sơ khác đang hoạt động.
 */
public final class BenhNhanDinhDanhXungDotNgoaiLe extends MienNghiepVuNgoaiLe {
    public BenhNhanDinhDanhXungDotNgoaiLe() {
        super("Định danh người bệnh đã được sử dụng bởi một hồ sơ đang hoạt động.");
    }
}
