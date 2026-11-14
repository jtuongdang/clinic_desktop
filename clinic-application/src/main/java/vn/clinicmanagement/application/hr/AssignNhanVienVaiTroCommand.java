package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Trường `actorId`: tacnhan mã. */
/** Trường `employeeId`: nhân viên mã. */
/** Trường `roleIds`: vai trò `Ids`. */
public record AssignNhanVienVaiTroCommand(int actorId, int employeeId, List<Integer> roleIds)
        implements Command<AssignNhanVienVaiTroBienNhan> {
    public AssignNhanVienVaiTroCommand {
        roleIds = roleIds == null ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(roleIds));
    }
}
