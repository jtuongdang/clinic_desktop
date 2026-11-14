package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TonKhoLoDuLieu(
        /** Trường `tonKhoId`: `ton` kho mã. */
        int tonKhoId,
        String soLo,
        LocalDate hanSuDung,
        BigDecimal soLuongTon,
        BigDecimal giaNhap,
        BigDecimal giaBan,
        boolean expired) {
}
