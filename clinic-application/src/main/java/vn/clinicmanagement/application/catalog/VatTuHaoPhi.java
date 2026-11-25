package vn.clinicmanagement.application.catalog;

import java.math.BigDecimal;

/** Available material in a facility warehouse; absent insurance/hospital tariffs stay null. */
public record VatTuHaoPhi(int id,String code,String name,String unit,String packing,String group,
        int warehouseId,String warehouseName,BigDecimal insurancePrice,BigDecimal hospitalPrice,BigDecimal servicePrice) {
    @Override public String toString(){return name;}
}
