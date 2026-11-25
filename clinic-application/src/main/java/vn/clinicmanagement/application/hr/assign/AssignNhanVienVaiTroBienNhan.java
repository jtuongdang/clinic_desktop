package vn.clinicmanagement.application.hr.assign;

import java.util.List;

public record AssignNhanVienVaiTroBienNhan(int employeeId, List<Integer> roleIds) {
    public AssignNhanVienVaiTroBienNhan {
        roleIds = List.copyOf(roleIds);
    }
}
