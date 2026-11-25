package vn.clinicmanagement.domain.common.exceptions;

public class ThucTheKhongTimThayNgoaiLe extends MienNghiepVuNgoaiLe {
    public ThucTheKhongTimThayNgoaiLe(String entityName, Object entityId) {
        super("Không tìm thấy thực thể " + entityName + " với ID: " + entityId);
    }
}
