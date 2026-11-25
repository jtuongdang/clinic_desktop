package vn.clinicmanagement.domain.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HanhDongQuyenTest {

    @Test
    void preservesExistingValuesAndRoundTripsEveryAction() {
        assertEquals(1, HanhDongQuyen.XEM.getValue());
        assertEquals(2, HanhDongQuyen.TAO.getValue());
        assertEquals(3, HanhDongQuyen.SUA.getValue());
        assertEquals(4, HanhDongQuyen.XOA.getValue());
        assertEquals(5, HanhDongQuyen.DUYET.getValue());
        assertEquals(6, HanhDongQuyen.IN.getValue());
        assertEquals(7, HanhDongQuyen.XUAT_FILE.getValue());
        assertEquals(8, HanhDongQuyen.MO_KHOA_EMR.getValue());
        assertEquals(9, HanhDongQuyen.AUDIT_READ.getValue());

        for (HanhDongQuyen action : HanhDongQuyen.values()) {
            assertEquals(action, HanhDongQuyen.fromValue(action.getValue()));
        }
    }
}
