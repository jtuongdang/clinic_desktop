package vn.clinicmanagement.application.hr;

import java.util.List;

/** Trường `employeeId`: nhân viên mã. */
/** Trường `roleIds`: vai trò `Ids`. */
public record AssignNhanVienVaiTroBienNhan(int employeeId, List<Integer> roleIds) {
    public AssignNhanVienVaiTroBienNhan {
        roleIds = List.copyOf(roleIds);
    }
}
