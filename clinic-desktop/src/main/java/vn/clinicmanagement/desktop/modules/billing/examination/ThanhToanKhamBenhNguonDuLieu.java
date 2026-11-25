package vn.clinicmanagement.desktop.modules.billing.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.*;
import vn.clinicmanagement.application.billing.get.GetHoaDonTheoLuotKhamQuery;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongBienNhan;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongCommand;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.infrastructure.persistence.billing.*;
import vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import java.math.BigDecimal;
import java.util.*;
import static vn.clinicmanagement.desktop.modules.billing.examination.ThanhToanKhamBenhDuLieu.*;

/** Loads the selected visit and executes per-line payer changes against the database. */
public final class ThanhToanKhamBenhNguonDuLieu {
    private ThanhToanKhamBenhNguonDuLieu() { }
    public static ThanhToanKhamBenhDuLieu load(AdminPatientItem patient,PhanQuyenNguCanh auth) {
        if(patient==null||patient.visitId<=0)throw new IllegalArgumentException("Chọn lượt khám để xem thanh toán.");
        if(auth==null||!auth.isValid())throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
        var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow);var provider=new JdbcKetNoiNhaCungCap(tx);
        var read=new VienPhiManHinhRepository(provider);
        var info=read.getByVisit(patient.visitId,auth.facilityId()).orElseThrow(()->new IllegalStateException("Không tìm thấy lượt khám tại cơ sở hiện tại."));
        var audit=new JdbcKiemToanRepository(provider,uow,CoSoDuLieuKetNoi::getConnection);
        var services=VienPhiGhepNoiGoc.create(new HoaDonRepository(provider,uow),new LuotKhamRepository(provider,uow),audit,tx);
        var actor=new KiemToanTacNhanNguCanh(auth.employeeId(),auth.roleId(),auth.roleCode(),auth.facilityId(),auth.departmentId(),NguoiDungPhien.getInstance().getWorkstationId(),UUID.randomUUID());
        return load(patient,auth,services,new ThanhToanDichVuRepository(provider),info,actor);
    }
    public static KetQua<ChangeChiTietHoaDonDoiTuongBienNhan> changePayer(
            int invoiceId,int lineId,String targetPayerType,
            BigDecimal fallbackServicePrice,BigDecimal fallbackInsurancePrice,Integer overrideCoverageRate,
            PhanQuyenNguCanh auth){
        if(auth==null||!auth.isValid())return KetQua.failure("AUTH_INVALID","Phiên đăng nhập không hợp lệ.");
        var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow);var provider=new JdbcKetNoiNhaCungCap(tx);
        var audit=new JdbcKiemToanRepository(provider,uow,CoSoDuLieuKetNoi::getConnection);
        var handler=new ChangeChiTietHoaDonDoiTuongHandler(
                new HoaDonRepository(provider,uow),
                new ThanhToanDichVuRepository(provider),
                audit,
                tx
        );
        var actor=new KiemToanTacNhanNguCanh(auth.employeeId(),auth.roleId(),auth.roleCode(),auth.facilityId(),auth.departmentId(),NguoiDungPhien.getInstance().getWorkstationId(),UUID.randomUUID());
        return handler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                invoiceId,lineId,targetPayerType,fallbackServicePrice,fallbackInsurancePrice,overrideCoverageRate,auth,actor));
    }
    static ThanhToanKhamBenhDuLieu load(AdminPatientItem patient,PhanQuyenNguCanh auth,VienPhiGhepNoiGoc.Services services,
            ThanhToanDichVuReadCongKetNoi sources,VienPhiManHinhReadCongKetNoi.ThongTin info,KiemToanTacNhanNguCanh actor){
        var response=services.getInvoiceByVisit().handle(new GetHoaDonTheoLuotKhamQuery(patient.visitId,auth,actor));
        HoaDonDuLieu invoice=null;String message="";
        if(response.isSuccess())invoice=response.valueOrThrow();
        else {var error=response.getError().orElse(null);if(error==null||!"INVOICE_NOT_FOUND".equals(error.code()))throw new IllegalStateException(error==null?"Không tải được hóa đơn.":error.message());message="Lượt khám chưa có hóa đơn.";}
        if(invoice!=null&&invoice.facilityId()!=auth.facilityId())throw new IllegalStateException("Hóa đơn không thuộc cơ sở hiện tại.");
        var rows=invoice==null?List.<Dong>of():mapRows(invoice,sources.getByInvoice(invoice.id(),patient.visitId,auth.facilityId()),info.subject());
        var activeCard=invoice==null?Optional.<ThanhToanDichVuReadCongKetNoi.ThongTinBaoHiemBenhNhan>empty():sources.getActiveInsuranceByPatient(invoice.patientId());
        String subject=switch(safe(info.subject()).toUpperCase(Locale.ROOT)){
            case "BHYT"->"BHYT";case "THU_PHI","VIEN_PHI"->"Viện phí";
            case "MIEN_PHI"->"Miễn phí";case "DICH_VU_YEU_CAU"->"Dịch vụ";case "UU_TIEN"->"Ưu tiên";default->safe(info.subject());};
        String bhytNumber=!safe(info.bhytNumber()).isEmpty()?info.bhytNumber():(activeCard.map(ThanhToanDichVuReadCongKetNoi.ThongTinBaoHiemBenhNhan::soBaoHiem).orElse(""));
        int bhytRate=patient.bhytRate>0?patient.bhytRate:activeCard.map(ThanhToanDichVuReadCongKetNoi.ThongTinBaoHiemBenhNhan::mucHuong).orElse(0);
        var bhytExp=info.bhytExpiry()!=null?info.bhytExpiry():activeCard.map(ThanhToanDichVuReadCongKetNoi.ThongTinBaoHiemBenhNhan::ngayHetHan).orElse(null);
        boolean covered="BHYT".equals(subject)||(invoice!=null&&invoice.insurancePaidAmount()!=null&&invoice.insurancePaidAmount().signum()>0);
        return new ThanhToanKhamBenhDuLieu(info.patientName(),info.patientCode(),info.birthDate()==null?safe(patient.birthYear):String.valueOf(info.birthDate().getYear()),
                safe(patient.queueAge),safe(info.gender()).isEmpty()?safe(patient.gender):"NAM".equalsIgnoreCase(info.gender())||"M".equalsIgnoreCase(info.gender())?"Nam":"Nữ",info.address(),bhytNumber,
                bhytRate,bhytExp==null?"":"Hạn thẻ: "+bhytExp,invoice==null?safe(patient.invoiceCode):invoice.invoiceNumber(),invoice,rows,info.collectors(),message,covered?"BHYT":subject);
    }
}
