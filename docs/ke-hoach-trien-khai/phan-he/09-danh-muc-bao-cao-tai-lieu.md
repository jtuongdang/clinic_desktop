# Phân hệ 09 — Danh mục, báo cáo và tài liệu

**Trạng thái:** `[~] Đang làm` (CSDL Danh mục nền đã nạp seed data; Core DOCX export engine đã hoàn thành 100%)  
**Đợt:** Danh mục nền ở Đợt 1; In ấn/Tài liệu ở Đợt 3 & 6; Báo cáo phân tích ở Đợt 7.  
**Domain:** `service`, `inventory`, `examination`, `system`, các aggregate nguồn.

---

## 1. Danh mục nền tảng (Catalogs)

### Hiện có
- **Models:** `ICD`, `DanhMucDichVu`, `Item`, `DonViTinhQuyDoi`, `NhaCungCap`, `DinhMucVatTuDichVu`.
- **CSDL SQL Server:** Đã có sẵn dữ liệu seed data chuẩn trong bảng `ICD`, `DanhMucDichVu`, `Item`, `PhongKham`, `Khoa`.
- **UI:** `CatalogPanel` và `SystemConfigPanel`.

### Backlog kỹ thuật
- [ ] Xây dựng `ICDRepositoryImpl`, `DanhMucDichVuRepositoryImpl`, `ItemRepositoryImpl` trong `clinic-infrastructure`.
- [ ] Tạo cơ chế Cache bộ nhớ trong ứng dụng cho danh mục ICD-10 và Bảng giá dịch vụ để tăng tốc độ phản hồi bàn khám và tiếp đón.
- [ ] Chức năng tìm kiếm nhanh mã bệnh ICD-10 với auto-complete theo mã và tên bệnh tiếng Việt.

---

## 2. In và Xuất Tài Liệu Y Tế (DOCX Export Engine)

### Hiện có (ĐÃ HOÀN TẤT CORE 100%)
Đã xây dựng trọn vẹn package **`vn.clinicmanagement.desktop.core.docx`** bằng Java thuần (OpenXML Zip generation, zero-dependency, chạy siêu nhẹ):
- **`DocxDocument.java`**: Trình tạo cấu trúc tài liệu WordprocessingML, đoạn văn, định dạng in đậm, in nghiêng, cỡ chữ, căn lề.
- **`DocxTable.java`**: Trình tạo bảng biểu y tế có header xám, border, căn lề theo cột (chuẩn bảng kê đơn thuốc và chi phí).
- **`DocxMedicalDocumentType.java`**: Định nghĩa các loại biểu mẫu chuẩn Bộ Y Tế:
  + Đơn thuốc ngoại trú (Mẫu số 04/BV)
  + Bệnh án khám bệnh ngoại trú
  + Giấy hẹn khám lại / Tái khám
  + Bảng kê chi phí khám chữa bệnh
- **`DocxExportService.java`**: Dịch vụ ánh xạ trực tiếp từ các Domain Entities (`BenhNhan`, `LuotKham`, `DonThuoc`, `ChiTietDonThuoc`) sang tài liệu Word `.docx` hoàn chỉnh.
- **`DocxManager.java`**: Lớp Facade tích hợp sẵn Swing `JFileChooser`, kiểm tra ghi đè file, và tự động mở tài liệu sau khi xuất.

### Backlog cần nối vào UI
- [ ] Gắn sự kiện nút **"In đơn thuốc (.docx)"** trên `PrescriptionOrderPanel` và preview dialog gọi `DocxManager.exportPrescription()`.
- [ ] Gắn sự kiện nút **"Xuất bệnh án (.docx)"** trên `DoctorStationPanel` gọi `DocxManager.exportMedicalRecord()`.
- [ ] Gắn sự kiện nút **"In giấy hẹn (.docx)"** trên `ReceptionPanel` gọi `DocxManager.exportAppointmentSlip()`.

---

## 3. Báo cáo & Thống kê

### Hiện có
- **UI:** `ClinicReportPanel` có filter ngày/loại và bảng ví dụ; hiện đang dùng dữ liệu tĩnh.
- Model báo cáo: Sử dụng DTO/Read Models thông qua các câu truy vấn SQL trực tiếp tối ưu (không tải toàn bộ entity graph).

### Backlog cần bổ sung
1. Báo cáo lượt tiếp đón và khám bệnh theo ngày/tháng/bác sĩ.
2. Báo cáo doanh thu viện phí và thanh toán BHYT.
3. Báo cáo kho dược: Thẻ kho, Báo cáo Nhập - Xuất - Tồn, danh sách thuốc cận hạn dùng (< 6 tháng).
4. Drill-down từ số tổng hợp đến chứng từ chi tiết; xuất báo cáo ra file Excel (.xlsx) hoặc PDF.

---

## 4. Nghiệm thu

- [ ] Xuất file đơn thuốc `.docx` thành công, mở trực tiếp bằng Microsoft Word / LibreOffice với định dạng chuẩn đẹp, căn lề đúng chuẩn y tế.
- [ ] Danh mục ICD-10 và Bảng giá dịch vụ được tải trực tiếp từ SQL Server.
- [ ] Báo cáo phản ánh đúng 100% dữ liệu thực tế phát sinh trong ca khám và viện phí.
