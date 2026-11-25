package vn.clinicmanagement.desktop.modules.billing.examination;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.*;
import vn.clinicmanagement.application.billing.get.GetHoaDonTheoLuotKhamHandler;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.domain.billing.*;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ThanhToanKhamBenhNguonDuLieuTest {
    static AdminPatientItem patient(){return new AdminPatientItem("1","KIỂM THỬ","TEST-001","Viện phí","26",false,"Nam","2000","26 tuổi","26 tuổi","2000-01-01","Địa chỉ","Địa chỉ","","BA1","","Khám bệnh","","","VP-1");}
    static PhanQuyenNguCanh auth(String role){return new PhanQuyenNguCanh(1,1,role,1,1,UUID.randomUUID());}
    static VienPhiManHinhReadCongKetNoi.ThongTin info(){return new VienPhiManHinhReadCongKetNoi.ThongTin("TEST-001","KIỂM THỬ",null,"M","Địa chỉ","","Khám bệnh","Phòng 1","THU_PHI","DANG_KHAM",null,null,Map.of());}
    static VienPhiGhepNoiGoc.Services services(HoaDon invoice){
        IHoaDonRepository repo=(IHoaDonRepository)Proxy.newProxyInstance(IHoaDonRepository.class.getClassLoader(),new Class[]{IHoaDonRepository.class},(p,m,a)->switch(m.getName()){case "getByLuotKham"->Optional.ofNullable(invoice);case "getPhieuThuByHoaDon","getPhieuHoanTraByHoaDon"->List.of();default->throw new UnsupportedOperationException(m.getName());});
        var audit=(KiemToanWriteCongKetNoi)Proxy.newProxyInstance(KiemToanWriteCongKetNoi.class.getClassLoader(),new Class[]{KiemToanWriteCongKetNoi.class},(p,m,a)->null);
        var defaults=VienPhiGhepNoiGoc.createDefault();return new VienPhiGhepNoiGoc.Services(defaults.createInvoice(),new GetHoaDonTheoLuotKhamHandler(repo,audit),defaults.recordPayment(),defaults.patientDebt());
    }
    static HoaDon invoice(int facility){HoaDon invoice=new HoaDon();invoice.setId(1);invoice.setSoHoaDon("VP-1");invoice.setMaLuotKham(1);invoice.setMaBenhNhan(2);invoice.setMaCoSo(facility);invoice.setNgayLap(LocalDateTime.now());ChiTietHoaDon row=new ChiTietHoaDon();row.setId(10);row.setLoaiDoiTuong("DICH_VU_KHAM");row.setMaDoiTuong(2);row.setMoTa("Khám bệnh kiểm thử");row.setSoLuong(BigDecimal.ONE);row.setDonGia(new BigDecimal("120000"));invoice.addChiTiet(row);invoice.recalculateCharges();return invoice;}
    @Test void loadsOnlyRequestedInvoiceAndFacility(){
        var result=ThanhToanKhamBenhNguonDuLieu.load(patient(),auth("DOCTOR"),services(invoice(1)),(invoice,visit,facility)->{assertEquals(1,invoice);assertEquals(1,visit);assertEquals(1,facility);return Map.of();},info(),null);
        assertEquals(1,result.rows().size());assertEquals("Khám bệnh kiểm thử",result.rows().get(0).description());assertEquals("Thu phí",result.rows().get(0).subject());assertEquals(0,new BigDecimal("120000").compareTo(result.invoice().totalAmount()));
    }
    @Test void rejectsOtherFacilityAndUnauthorizedRoleBeforeReadingDetails(){
        ThanhToanDichVuReadCongKetNoi noRead=(i,v,f)->{fail("Không được đọc chi tiết ngoài phạm vi.");return Map.of();};
        assertThrows(IllegalStateException.class,()->ThanhToanKhamBenhNguonDuLieu.load(patient(),auth("DOCTOR"),services(invoice(2)),noRead,info(),null));
        assertThrows(IllegalStateException.class,()->ThanhToanKhamBenhNguonDuLieu.load(patient(),auth("NURSE"),services(invoice(1)),noRead,info(),null));
    }
    @Test void missingInvoiceDoesNotCreateOrFabricateCharges(){
        var result=ThanhToanKhamBenhNguonDuLieu.load(patient(),auth("DOCTOR"),services(null),(i,v,f)->{fail("Chưa có hóa đơn để đọc.");return Map.of();},info(),null);
        assertNull(result.invoice());assertTrue(result.rows().isEmpty());assertEquals("Lượt khám chưa có hóa đơn.",result.message());
    }
}
