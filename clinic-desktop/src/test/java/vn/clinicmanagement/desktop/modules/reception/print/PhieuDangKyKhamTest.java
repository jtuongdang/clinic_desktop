package vn.clinicmanagement.desktop.modules.print.reception;

import vn.clinicmanagement.desktop.modules.print.reception.PhieuDangKyKhamManHinh;
import vn.clinicmanagement.desktop.modules.print.reception.PhieuDangKyKhamPdf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu;
import java.math.BigDecimal;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhieuDangKyKhamTest {
    static PhieuDangKyKhamDuLieu sample(BigDecimal price) {
        return new PhieuDangKyKhamDuLieu(1,"BN-TEST","PHÒNG KHÁM KIỂM THỬ","Địa chỉ kiểm thử","0123456789",
                "NGƯỜI BỆNH KIỂM THỬ",LocalDate.of(1999,1,1),"Nam","Địa chỉ thử nghiệm có dấu tiếng Việt","PK 1",
                LocalDateTime.of(2026,11,18,9,0),null,null,List.of(new PhieuDangKyKhamDuLieu.DongDichVu("Khám Nội chung",BigDecimal.ONE,price)));
    }
    @Test void soTienBangChuVaGiaChuaChot() {
        assertEquals("Tám mươi nghìn đồng",SoTienBangChu.read(new BigDecimal("80000")));
        assertEquals("Một triệu không trăm lẻ một nghìn đồng",SoTienBangChu.read(new BigDecimal("1001000")));
        assertEquals("Không đồng",SoTienBangChu.read(BigDecimal.ZERO));
        assertNull(sample(null).total()); assertFalse(sample(null).hasConfirmedPrice());
        assertEquals(new BigDecimal("80000"),sample(new BigDecimal("80000")).total());
    }
    @Test void phamViTrangPhaiHopLe() {
        assertArrayEquals(new int[]{0,2,3,4},PhieuDangKyKhamManHinh.parsePages("1,3-5,3",5));
        for(String invalid:List.of("0","2-1","1-9","a","", "1--2"))
            assertThrows(IllegalArgumentException.class,()->PhieuDangKyKhamManHinh.parsePages(invalid,5));
    }
    @Test void nhieuDichVuTachTrangVaGiayA4VanGiuKhoNoiDungA5(@TempDir Path directory) throws Exception {
        var basic=sample(new BigDecimal("80000"));
        var data=new PhieuDangKyKhamDuLieu(basic.visitId(),basic.patientCode(),basic.facilityName(),basic.facilityAddress(),basic.facilityPhone(),
                basic.patientName(),basic.birthDate(),basic.gender(),basic.address(),basic.roomName(),basic.registeredAt(),null,null,
                java.util.Collections.nCopies(7,basic.services().get(0)));
        var document=new PhieuDangKyKhamTrang(data);
        assertEquals(2,document.pageCount());assertEquals(new BigDecimal("560000"),data.total());
        Path pdf=directory.resolve("nhieu-trang.pdf");
        PhieuDangKyKhamPdf.write(document,pdf,new int[]{0,1},210/25.4*72,297/25.4*72);
        String content=Files.readString(pdf,java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("/Count 2"));assertTrue(content.contains("/MediaBox [0 0 595.2756 841.8898]"));
    }

    @Test void pdfDungKhoA5VaKhongDeFileHongKhiLoi(@TempDir Path directory) throws Exception {
        var document = new PhieuDangKyKhamTrang(sample(new BigDecimal("80000")));
        Path file=directory.resolve("phieu.pdf");
        PhieuDangKyKhamPdf.write(document,file,new int[]{0},148/25.4*72,210/25.4*72);
        String pdf=Files.readString(file,java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(pdf.startsWith("%PDF-1.4")); assertTrue(pdf.contains("/Count 1")); assertTrue(pdf.contains("/MediaBox [0 0 419.5276 595.2756]"));
        byte[] previous=Files.readAllBytes(file);
        assertThrows(IllegalArgumentException.class,()->PhieuDangKyKhamPdf.write(document,file,new int[]{2},420,595));
        assertArrayEquals(previous,Files.readAllBytes(file));
        try(var files=Files.list(directory)) { assertEquals(1,files.count()); }
    }
}
