package vn.clinicmanagement.domain.examination;

import java.util.Set;

/**
 * Danh mục nghiệp vụ đã duyệt cho việc chuyển lượt khám ngoại trú (`VIS-TRANSFER-OWNER-DECISION-RECON-05`). <p> Enum này chỉ cung cấp dữ liệu phân loại và ma trận vai trò đủ điều kiện. Các lớp bảo vệ của ứng dụng và ranh giới phân quyền vẫn bắt buộc.
 */
public enum TransferLyDoMa {

    /** Nhân viên tiếp đón chuyển nhầm bệnh nhân; lỗi được sửa trước khi bắt đầu khám. Vai trò đủ điều kiện: chỉ `RECEPTIONIST`. */
    RECEPTION_TRIAGE_CORRECTION(Set.of("RECEPTIONIST")),

    /** Bệnh nhân yêu cầu đổi phòng khi đang chờ khám (`CHO_KHAM`). Vai trò đủ điều kiện: `RECEPTIONIST` và `DOCTOR`. */
    PATIENT_SPECIALTY_PREFERENCE(Set.of("RECEPTIONIST", "DOCTOR")),

    /**
     * Chuyển bệnh nhân giữa các phòng song song trong cùng khoa để cân bằng thời gian chờ. Vai trò đủ điều kiện: `RECEPTIONIST` và `DOCTOR`.
     */
    CLINIC_LOAD_BALANCING(Set.of("RECEPTIONIST", "DOCTOR")),

    /**
     * Bác sĩ đang khám yêu cầu phòng chuyên khoa phù hợp khác trong khoa đã chọn cho phiên. Vai trò đủ điều kiện: chỉ `DOCTOR`.
     */
    CLINICAL_SPECIALTY_REFERRAL(Set.of("DOCTOR")),

    /** Phòng hiện tại thiếu thiết bị chẩn đoán cần thiết, thiết bị có ở phòng đích. Vai trò đủ điều kiện: chỉ `DOCTOR`. */
    DIAGNOSTIC_EQUIPMENT_NEED(Set.of("DOCTOR")),

    /**
     * Bác sĩ được điều động xử lý cấp cứu, đi buồng hoặc vắng đột xuất; cần bàn giao hàng đợi đang hoạt động. Vai trò đủ điều kiện: chỉ `DOCTOR`.
     */
    CLINICIAN_EMERGENCY_HANDOFF(Set.of("DOCTOR"));

    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    public static final String ROLE_DOCTOR = "DOCTOR";

    private final Set<String> allowedRoleCodes;

    TransferLyDoMa(Set<String> allowedRoleCodes) {
        this.allowedRoleCodes = allowedRoleCodes;
    }

    /**
     * Trả về tập mã vai trò được phép cho lý do chuyển này; tập hợp không thể sửa đổi. @return tập mã vai trò bất biến được phép.
     */
    public Set<String> getAllowedRoleCodes() {
        return allowedRoleCodes;
    }

    /**
     * Tên gọi rút gọn theo kiểu fluent của {@link #getAllowedRoleCodes()}. @return tập mã vai trò bất biến được phép.
     */
    public Set<String> allowedRoleCodes() {
        return allowedRoleCodes;
    }

    /**
     * Xác định mã vai trò có được dùng lý do chuyển này hay không. Phải khớp chính xác chuỗi; mã `null`, rỗng, có khoảng trắng thừa hoặc không xác định đều bị từ chối. @param roleCode mã vai trò cần kiểm tra (ví dụ: `RECEPTIONIST`, `DOCTOR`) @return {@code true} nếu đủ điều kiện; ngược lại là {@code false}.
     */
    public boolean isEligibleRole(String roleCode) {
        if (roleCode == null) {
            return false;
        }
        return allowedRoleCodes.contains(roleCode);
    }

    /**
     * Kiểm tra lý do chuyển này có được phép cho mã vai trò đã chỉ định hay không. @param roleCode mã vai trò cần kiểm tra @return {@code true} nếu được phép; ngược lại là {@code false}.
     */
    public boolean isAllowedForRole(String roleCode) {
        return isEligibleRole(roleCode);
    }

    /**
     * Kiểm tra mã vai trò đã chỉ định có được phép dùng lý do chuyển này hay không. @param roleCode mã vai trò cần kiểm tra @return {@code true} nếu được phép; ngược lại là {@code false}.
     */
    public boolean isAllowedRole(String roleCode) {
        return isEligibleRole(roleCode);
    }

    /**
     * Kiểm tra lý do chuyển này có cho phép mã vai trò đã chỉ định hay không. @param roleCode mã vai trò cần kiểm tra @return {@code true} nếu được phép; ngược lại là {@code false}.
     */
    public boolean allowsRole(String roleCode) {
        return isEligibleRole(roleCode);
    }
}
