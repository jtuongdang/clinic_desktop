# Phân hệ 09 — Danh mục, báo cáo và tài liệu

**Trạng thái:** `[~] Có seed data và DOCX engine trong source; tích hợp và nghiệm thu end-to-end còn thiếu`
**Đợt:** Danh mục nền ở Đợt 1; In ấn/Tài liệu ở Đợt 3 & 6; Báo cáo phân tích ở Đợt 7.
**Domain:** `service`, `inventory`, `examination`, `system`, các aggregate nguồn.

---

## 1. Danh mục nền tảng (Catalogs)

### Hiện có
- **Models:** `ICD`, `DanhMucDichVu`, `MatHang`, `DonViTinhQuyDoi`, `NhaCungCap`, `DinhMucVatTuDichVu`.
- **Migration:** Có script seed dữ liệu ban đầu cho một số danh mục như `ICD`, dịch vụ, item, phòng khám và khoa. Cần xác minh dữ liệu/phiên bản trên DB thử nghiệm; không xem seed là danh mục nghiệp vụ đã được duyệt.
- **UI:** `DanhMucManHinh` và `HeThongCauHinhManHinh`.

### Backlog kỹ thuật
- [ ] Xây dựng `ICDRepository`, `DanhMucDichVuRepository`, `ItemRepository` trong `clinic-infrastructure`.
- [ ] Tạo cơ chế Cache bộ nhớ trong ứng dụng cho danh mục ICD-10 và Bảng giá dịch vụ để tăng tốc độ phản hồi bàn khám và tiếp đón.
- [ ] Chức năng tìm kiếm nhanh mã bệnh ICD-10 với auto-complete theo mã và tên bệnh tiếng Việt.

---

## 2. In và Xuất Tài Liệu Y Tế (DOCX Export Engine)

### Hiện có trong source
Package **`vn.clinicmanagement.desktop.core.docx`** có các lớp tạo/xuất tài liệu OpenXML. Cần kiểm thử file đầu ra, xác nhận nội dung mẫu với nghiệp vụ và nối vào luồng UI trước khi nghiệm thu:
- **`DOCXTaiLieu.java`**: Trình tạo cấu trúc tài liệu WordprocessingML, đoạn văn, định dạng in đậm, in nghiêng, cỡ chữ, căn lề.
- **`DOCXBang.java`**: Trình tạo bảng biểu y tế có header xám, border, căn lề theo cột (chuẩn bảng kê đơn thuốc và chi phí).
- **`DOCXYTeTaiLieuLoai.java`**: Enum khai báo nhiều loại tài liệu. Tên/description trong enum chưa phải bằng chứng mẫu đã được dựng hoặc tuân thủ biểu mẫu hiện hành.
- **`DOCXExportDichVu.java`**: Có các hàm dựng tài liệu từ dữ liệu Domain; cần kiểm tra trường nào được đưa vào và xử lý dữ liệu thiếu.
- **`DOCXQuanLy.java`**: Hiện có `promptAndExportPrescription` và `promptAndExportMedicalRecord`, dùng `JFileChooser` rồi lưu file. Chưa thấy API xuất giấy hẹn trong manager.

### Backlog cần nối vào UI
- [ ] Xác định nút/luồng phù hợp rồi nối đơn thuốc với `DOCXQuanLy.promptAndExportPrescription(...)`.
- [ ] Nối xuất bệnh án với `DOCXQuanLy.promptAndExportMedicalRecord(...)` sau khi có dữ liệu khám đã lưu.
- [ ] Chỉ bổ sung xuất giấy hẹn khi nghiệp vụ lịch hẹn và API tạo tài liệu đã được triển khai.
- [ ] Kiểm thử mở file DOCX bằng parser/Word-compatible reader và review mẫu với nghiệp vụ; không tuyên bố tuân thủ chỉ dựa trên enum mô tả.

---

## 3. Báo cáo & Thống kê

### Hiện có
- **UI:** `PhongKhamBaoCaoManHinh` có filter ngày/loại và bảng ví dụ; hiện đang dùng dữ liệu tĩnh.
- **Định hướng:** dùng query/Read Model trong Application với truy vấn phù hợp ở infrastructure; hiện chưa thấy persistence báo cáo được nghiệm thu.

### Backlog cần bổ sung
1. Báo cáo lượt tiếp đón và khám bệnh theo ngày/tháng/bác sĩ.
2. Báo cáo doanh thu viện phí và thanh toán BHYT.
3. Báo cáo kho dược: Thẻ kho, Báo cáo Nhập - Xuất - Tồn, danh sách thuốc cận hạn dùng (< 6 tháng).
4. Drill-down từ số tổng hợp đến chứng từ chi tiết; xuất báo cáo ra file Excel (.xlsx) hoặc PDF.

---

## 4. Nghiệm thu

- [ ] Xuất file đơn thuốc `.docx` thành công, mở trực tiếp bằng Microsoft Word / LibreOffice với định dạng chuẩn đẹp, căn lề đúng chuẩn y tế.
- [ ] Danh mục ICD-10 và Bảng giá dịch vụ được tải trực tiếp từ SQL Server.
- [ ] Báo cáo đối soát đúng với các giao dịch đã ghi nhận trong phạm vi và kỳ báo cáo được xác định.
