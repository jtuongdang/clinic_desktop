package vn.clinicmanagement.desktop.core.docx;

/**
 * Định nghĩa các loại tài liệu biểu mẫu y tế xuất DOCX theo quy chuẩn Bộ Y Tế.
 */
public enum DocxMedicalDocumentType {
    DON_THUOC("Đơn Thuốc Ngoại Trú", "DONTHUOC", "Mẫu đơn thuốc điện tử theo Thông tư 04/2022/TT-BYT"),
    BENH_AN_NGOAI_TRU("Bệnh Án Khám Bệnh Ngoại Trú", "BENHAN", "Mẫu bệnh án ngoại trú số 01/BV Bộ Y Tế"),
    PHIEU_CHI_DINH_CLS("Phiếu Chỉ Định Cận Lâm Sàng", "CHIDINH", "Phiếu chỉ định xét nghiệm, X-quang, CĐHA"),
    GIAY_HEN_KHAM("Giấy Hẹn Tái Khám", "TAIKHAM", "Phiếu dặn dò hẹn ngày khám lại"),
    GIAY_CHUYEN_TUYEN("Giấy Chuyển Tuyến KCB", "CHUYENTUYEN", "Mẫu giấy chuyển viện BHYT số 4210/BYT"),
    BANG_KE_VIEN_PHI("Bảng Kê Chi Phí Khám Chữa Bệnh", "VIENPHI", "Bảng kê chi phí khám, thuốc, dịch vụ kỹ thuật"),
    GIAY_NGHI_OM_BHXH("Giấy Chứng Nhận Nghỉ Việc Hưởng BHXH", "NGHIBHXH", "Mẫu cấp giấy nghỉ ốm hưởng trợ cấp BHXH");

    private final String displayName;
    private final String filePrefix;
    private final String description;

    DocxMedicalDocumentType(String displayName, String filePrefix, String description) {
        this.displayName = displayName;
        this.filePrefix = filePrefix;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getFilePrefix() { return filePrefix; }
    public String getDescription() { return description; }
}
