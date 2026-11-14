package vn.clinicmanagement.application.hr;

import java.util.List;

public interface NhanVienVaiTroPhanCong {
    /** Hàm `replaceRoleIds`: `replace` vai trò `Ids`. */
    void replaceRoleIds(int employeeId, List<Integer> roleIds);
}
