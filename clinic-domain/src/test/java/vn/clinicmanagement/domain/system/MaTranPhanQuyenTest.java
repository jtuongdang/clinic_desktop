package vn.clinicmanagement.domain.system;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MaTranPhanQuyenTest {

    @Test
    void defaultsToDepartmentScopeAndDeny() {
        MaTranPhanQuyen permission = new MaTranPhanQuyen();

        assertEquals(PhamViDuLieu.KHOA_PHONG, permission.getPhamViDuLieu());
        assertFalse(permission.isDuocPhep());
    }
}
