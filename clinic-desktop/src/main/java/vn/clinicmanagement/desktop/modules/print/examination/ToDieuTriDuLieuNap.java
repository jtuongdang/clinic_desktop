package vn.clinicmanagement.desktop.modules.print.examination;


import vn.clinicmanagement.desktop.modules.print.billing.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamQuery;
import vn.clinicmanagement.application.prescription.get.GetDonThuocTheoLuotKhamQuery;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.print.examination.ToDieuTriPrintManHinh.TreatmentSheetData;
import java.util.*;

/** Compose the document from persisted visit and existing protected order queries. */
public final class ToDieuTriDuLieuNap {
    private ToDieuTriDuLieuNap() {}
    public static TreatmentSheetData load(KhamBenhGhepNoiGoc.Services services,AdminPatientItem patient,PhanQuyenNguCanh auth) {
        if(auth==null||!auth.isValid())throw new IllegalStateException("Phiên không hợp lệ.");
        var visit=services.examQueue().getVisitDetail(patient.visitId).orElseThrow();
        if(visit.getMaCoSo()!=auth.facilityId()||visit.getMaKhoa()!=auth.departmentId())throw new IllegalStateException("Lượt khám ngoài phạm vi.");
        TreatmentSheetData base=ToDieuTriPrintManHinh.fromVisit(visit,patient);
        var prescriptions=services.getPrescriptionByVisit().handle(new GetDonThuocTheoLuotKhamQuery(patient.visitId,auth));
        KiemToanTacNhanNguCanh actor=new KiemToanTacNhanNguCanh(auth.employeeId(),auth.roleId(),auth.roleCode(),auth.facilityId(),auth.departmentId(),"DESKTOP",UUID.randomUUID());
        var orders=services.getServicesByVisit().handle(new GetDichVuTheoLuotKhamQuery(patient.visitId,auth,actor));
        if(prescriptions==null||!prescriptions.isSuccess()||orders==null||!orders.isSuccess())throw new IllegalStateException("Không đọc được đầy đủ chỉ định.");
        List<String> meds=new ArrayList<>(),supplies=new ArrayList<>(),cls=new ArrayList<>();
        for(var order:prescriptions.getValue().orElse(List.of())) {
            if(order.trangThai()==TrangThaiDonThuoc.HUY||order.items()==null)continue;
            for(var item:order.items()) {
                boolean supply=order.isSupplyOrder()||item.loaiItem()==LoaiMatHang.VAT_TU;
                String line="- "+safe(item.itemName())+" x "+(item.soLuongKe()==null?"":item.soLuongKe().stripTrailingZeros().toPlainString())+" "+safe(item.donViTinh());
                if(!supply) {
                    if(!safe(item.duongDung()).isBlank())line+=", "+item.duongDung();
                    if(!safe(item.lieuDung()).isBlank())line+=", "+item.lieuDung();
                    if(!safe(item.huongDan()).isBlank())line+=", "+item.huongDan();
                }
                (supply?supplies:meds).add(line);
            }
        }
        for(var order:orders.getValue().orElse(List.of()))
            if(!"DA_HUY".equals(order.status())&&!"KHAM".equals(order.serviceType()))cls.add(safe(order.serviceName()));
        return new TreatmentSheetData(base.sheetNumber(),base.visitId(),base.patientCode(),base.patientName(),base.ageText(),base.gender(),base.subjectType(),base.address(),
                base.departmentName(),base.roomName(),base.bedName(),base.diagnosisText(),base.examDate(),base.reasonText(),base.diseaseProgress(),base.personalHistory(),base.familyHistory(),
                base.treatmentMethod(),meds,supplies,cls,base.doctorName(),base.birthYear(),base.insuranceNumber(),base.insuranceRate(),base.insuranceRemaining());
    }
    private static String safe(String text){return text==null?"":text.trim();}
}
