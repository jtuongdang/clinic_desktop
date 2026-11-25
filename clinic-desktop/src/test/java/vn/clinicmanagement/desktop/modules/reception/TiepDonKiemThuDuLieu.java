package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.desktop.modules.reception.components.BenhNhanDangKyBieuMauManHinh;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import java.util.*;
import java.lang.reflect.Proxy;
import javax.swing.*;

/** Dữ liệu hợp lệ tối thiểu để các phép thử phòng/phiên không bị lỗi ở trường khác. */
public final class TiepDonKiemThuDuLieu {
    private TiepDonKiemThuDuLieu() { }
    public static DanhMucDichVu service() {var s=new DanhMucDichVu();s.setId(11);s.setTenDichVu("Khám kiểm thử");s.setLoaiDichVu(LoaiDichVu.KHAM);s.setHoatDong(true);return s;}
    public static void loadService(TiepDonManHinh panel){panel.getFormPanel().setClinicalServices(List.of(service()));}
    public static IChiDinhDichVuRepository serviceRepository(){return (IChiDinhDichVuRepository)Proxy.newProxyInstance(TiepDonKiemThuDuLieu.class.getClassLoader(),new Class[]{IChiDinhDichVuRepository.class},(p,m,a)->switch(m.getName()){case "getDichVuById"->Optional.of(service());case "searchDichVu"->List.of(service());default->throw new UnsupportedOperationException(m.getName());});}
    public static Object value(BenhNhanDangKyBieuMauManHinh panel,String field){try{var f=panel.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(panel);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    public static String text(BenhNhanDangKyBieuMauManHinh panel,String field){return ((JTextField)value(panel,field)).getText();}
    public static void fillRequired(BenhNhanDangKyBieuMauManHinh panel){
        ((JTextField)value(panel,"txtHoTen")).setText("Nguyễn Văn Kiểm Thử");
        ((JTextField)value(panel,"txtDay")).setText("01");((JTextField)value(panel,"txtMonth")).setText("01");((JTextField)value(panel,"txtYear")).setText("1999");
    }
}
