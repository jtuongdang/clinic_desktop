package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import javax.swing.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra thông báo gắn với đúng ô, và danh mục chưa tải không tạo được lệnh lưu. */
class DangKyKhamNhapLieuTest {
    private static final UUID SESSION=UUID.randomUUID();
    private static final PhanQuyenNguCanh AUTH=new PhanQuyenNguCanh(1,2,"RECEPTIONIST",3,4,SESSION);
    private static final KiemToanTacNhanNguCanh AUDIT=new KiemToanTacNhanNguCanh(1,2,"RECEPTIONIST",3,4,"TEST",UUID.randomUUID());

    @Test void thieuHoTenNgaySinhVaNgayKhongTonTaiChiDungO() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel=validForm();
            text(panel,"txtHoTen").setText(""); expect(panel,"họ và tên",text(panel,"txtHoTen"));
            text(panel,"txtHoTen").setText("Nguyễn Văn An");
            text(panel,"txtMonth").setText(""); expect(panel,"đủ ngày, tháng và năm",text(panel,"txtMonth"));
            text(panel,"txtDay").setText("31");text(panel,"txtMonth").setText("02");expect(panel,"Ngày sinh không hợp lệ",text(panel,"txtDay"));
        });
    }
    @Test void danhMucChuaTaiDangTaiTrongVaLuaChonSaiDeuChan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel=validForm();
            panel.beginClinicalServiceLoad();expect(panel,"danh mục dịch vụ",panel.getChooserService().getTextField());
            assertEquals("Khám kiểm thử",panel.getChooserService().getText());
            panel.setClinicalServices(List.of(service()));assertEquals(11,panel.toRegisterCommand(AUTH,AUDIT).serviceId());
            panel.setClinicalServices(List.of());expect(panel,"danh mục dịch vụ",panel.getChooserService().getTextField());
            panel.setClinicalServices(List.of(service()));panel.getChooserService().setText("");expect(panel,"chọn dịch vụ",panel.getChooserService().getTextField());
            panel.getChooserService().setText("Dịch vụ không tồn tại");expect(panel,"không có trong danh mục",panel.getChooserService().getTextField());
        });
    }
    @Test void loiDienThoaiNgayGiayToVaPhongKhamDuocChiRo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel=validForm();text(panel,"txtPhone").setText("123");
            var error=expect(panel,"Số điện thoại",text(panel,"txtPhone"));
            error.focusInput();assertEquals("error",error.getInput().getClientProperty("JComponent.outline"));
            text(panel,"txtPhone").setText("0912345678");assertNull(error.getInput().getClientProperty("JComponent.outline"));
            text(panel,"txtCccdD").setText("01");expect(panel,"Ngày cấp CMND/CCCD",text(panel,"txtCccdM"));
            text(panel,"txtCccdD").setText("");panel.setClinicRooms(List.of());
            var missing=assertThrows(DangKyKhamNhapLieuNgoaiLe.class,()->panel.toRegisterCommand(AUTH,AUDIT));
            assertTrue(missing.getMessage().contains("danh sách phòng khám"));
        });
    }
    @Test void bhytThieuTheVaNgayHetHanChiToiOThucTeTrenManHinh() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var panel=validForm();((JComboBox<?>)field(panel,"cbDoiTuong")).setSelectedItem("BHYT");
            JTextField[] number=(JTextField[])field(panel,"insuranceNumberFields");expect(panel,"số thẻ",number[0]);
            number[0].setText("AB123");
            var error=assertThrows(DangKyKhamNhapLieuNgoaiLe.class,()->panel.toRegisterCommand(AUTH,AUDIT));
            assertTrue(error.getMessage().contains("ngày hết hạn thẻ BHYT"));
            assertTrue(SwingUtilities.isDescendingFrom(error.getInput(),panel),"Phải focus ô ngày hiển thị, không focus trường dữ liệu ẩn");
        });
    }
    private static DangKyKhamNhapLieuNgoaiLe expect(BenhNhanDangKyBieuMauManHinh panel,String message,JComponent target) {
        var error=assertThrows(DangKyKhamNhapLieuNgoaiLe.class,()->panel.toRegisterCommand(AUTH,AUDIT));
        assertTrue(error.getMessage().contains(message),error.getMessage());assertSame(target,error.getInput());return error;
    }
    private static BenhNhanDangKyBieuMauManHinh validForm() {
        var panel=new BenhNhanDangKyBieuMauManHinh();var room=new PhongKham();room.setId(7);room.setTenPhong("PK 1");panel.setClinicRooms(List.of(room));
        panel.setClinicalServices(List.of(service()));text(panel,"txtHoTen").setText("Nguyễn Văn An");text(panel,"txtDay").setText("01");text(panel,"txtMonth").setText("01");text(panel,"txtYear").setText("1999");return panel;
    }
    private static DanhMucDichVu service(){var s=new DanhMucDichVu();s.setId(11);s.setTenDichVu("Khám kiểm thử");s.setLoaiDichVu(LoaiDichVu.KHAM);s.setHoatDong(true);return s;}
    private static JTextField text(Object panel,String name){return (JTextField)field(panel,name);}
    private static Object field(Object target,String name){try{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(ReflectiveOperationException error){throw new AssertionError(error);}}
}
