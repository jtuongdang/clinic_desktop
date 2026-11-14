# HỢP ĐỒNG 01: PHÂN HỆ KHÁM CHỮA BỆNH & BÁC SĨ (EXAMINATION & DOCTOR MODULE)
**Mã hợp đồng:** HD-INTEG-01 | **Trạng thái:** Sẵn sàng triển khai

---

## 1. MỤC TIÊU TRIỂN KHAI
Đấu nối toàn bộ quy trình nghiệp vụ của Bác sĩ từ lúc tiếp nhận bệnh nhân từ hàng đợi -> Bắt đầu khám -> Kê đơn thuốc -> Chỉ định cận lâm sàng -> Ghi tóm tắt bệnh án EMR -> Xử trí / Hẹn tái khám / Chuyển phòng / Kết thúc khám.

---

## 2. MA TRẬN ĐẤU NỐI MÀN HÌNH - NGHIỆP VỤ - CSDL

### 2.1. Tiếp Nhận & Điều Khiển Bàn Khám Bác Sĩ
- **Màn hình:** `BacSiTramManHinh` & `BacSiHanhDongThanh`
- **Tác vụ nghiệp vụ:**
  - Bấm vào bệnh nhân trên hàng đợi -> kích hoạt `StartKhamCommand` (nếu trạng thái là `CHO_KHAM`).
  - Chuyển trạng thái lượt khám trong bảng `LuotKham` từ `CHO_KHAM` -> `DANG_KHAM`.
  - Các nút `[In]`, `[Làm mới]`, `[Tìm kiếm]` trên `BacSiHanhDongThanh` kết nối tới các dialog nghiệp vụ thực tế.
- **Tầng nghiệp vụ sử dụng:**
  - `vn.clinicmanagement.application.examination.StartKhamHandler`
  - `vn.clinicmanagement.application.examination.CancelLuotKhamHandler`

### 2.2. Kê Đơn Thuốc (Prescription Order)
- **Màn hình:** `DonThuocYeuCauManHinh` (Form kê đơn) và `DonThuocYeuCauTheManHinh` (Tab Thuốc)
- **Điểm yếu hiện tại:** Nút `[Lưu (Ctrl + S)]` chỉ hiện `JOptionPane.showMessageDialog` giả lập.
- **Cam kết hợp đồng:**
  - Khi bác sĩ ấn `[Lưu]` hoặc `[Lưu + Đóng]`:
    1. Đọc danh sách thuốc đã kê từ bảng giao diện `medicineTable` (Mã thuốc, tên thuốc, số lượng, liều dùng, cách dùng).
    2. Đóng gói thành `PrescribeThuocCommand`.
    3. Gọi `PrescribeThuocHandler` qua background `SwingWorker`.
    4. Ghi nhận giao dịch vào bảng `DonThuoc` và `ChiTietDonThuoc` trong CSDL `clinicDb`.
    5. Cập nhật lại danh sách đơn thuốc trên `DonThuocYeuCauTheManHinh` hiển thị đúng mã phiếu vừa sinh (VD: `DT-20261108-0001`).
    6. Nút `[Hủy Gửi Đơn Thuốc]` trên tab Thuốc gọi `CancelDonThuocHandler` để hủy đơn trong CSDL (thay vì chỉ xóa row trên RAM).
- **Tầng nghiệp vụ sử dụng:**
  - `vn.clinicmanagement.application.prescription.PrescribeThuocHandler`
  - `vn.clinicmanagement.application.prescription.GetDonThuocTheoLuotKhamHandler`
  - `vn.clinicmanagement.application.prescription.CancelDonThuocHandler`

### 2.3. Chỉ Định Cận Lâm Sàng & Dịch Vụ Kỹ Thuật (Service Order)
- **Màn hình:** `DichVuYeuCauManHinh` (Form chỉ định) và `XetNghiemYeuCauTheManHinh` (Tab Xét Nghiệm / CĐHA)
- **Điểm yếu hiện tại:** Nút `[Lưu]` chỉ đóng form mà không tạo bản ghi chỉ định nào.
- **Cam kết hợp đồng:**
  - Khi bác sĩ tick chọn dịch vụ và bấm `[Lưu]`:
    1. Đóng gói danh sách dịch vụ kỹ thuật đã chọn thành `YeuCauDichVuCommand`.
    2. Gọi `YeuCauDichVuHandler` qua background `SwingWorker`.
    3. Thêm mới bản ghi vào bảng `ChiDinhDichVu` với trạng thái `CHO_THUC_HIEN`.
    4. Cập nhật tab `XetNghiemYeuCauTheManHinh` hiển thị danh sách các xét nghiệm/CĐHA thực tế vừa chỉ định.
    5. Nút `[Hủy Gửi Phiếu]` gọi `CancelDichVuYeuCauHandler` để hủy chỉ định trong CSDL.
- **Tầng nghiệp vụ sử dụng:**
  - `vn.clinicmanagement.application.cls.YeuCauDichVuHandler`
  - `vn.clinicmanagement.application.cls.GetDichVuTheoLuotKhamHandler`
  - `vn.clinicmanagement.application.cls.CancelDichVuYeuCauHandler`

### 2.4. Hồ Sơ Bệnh Án Điện Tử (EMR Clinical Record)
- **Màn hình:** `HoSoBenhAnTheManHinh` (Tab Bệnh Án)
- **Điểm yếu hiện tại:** Nút `[Lưu]` hiển thị popup thành công tĩnh nhưng không lưu CSDL.
- **Cam kết hợp đồng:**
  - Đọc nội dung 5 khối tóm tắt: Quá trình bệnh lý, tóm tắt CLS, phương pháp điều trị, tình trạng ra viện, hướng điều trị tiếp theo.
  - Đóng gói thành `UpdateEMRLamSangRecordCommand`.
  - Gọi `UpdateEMRLamSangRecordHandler` để lưu thông tin vào bảng `NhatKyHoSoBenhAn` / `HoSoBenhAn`.
  - Ghi nhận Audit Log hành động sửa bệnh án y khoa.
- **Tầng nghiệp vụ sử dụng:**
  - `vn.clinicmanagement.application.examination.UpdateEMRLamSangRecordHandler`
  - `vn.clinicmanagement.application.examination.GetLuotKhamChiTietHandler`

### 2.5. Xử Trí, Chẩn Đoán ICD-10 & Kết Thúc Khám (Treatment Resolution)
- **Màn hình:** `DieuTriXuTriTheManHinh` (Tab Xử Trí)
- **Điểm yếu hiện tại:** Nút `[💾 Lưu]` góc dưới phải chỉ nạp ngày giờ hiện tại và mở dialog in phiếu hẹn, chưa ghi nhận kết quả điều trị.
- **Cam kết hợp đồng:**
  - Lưu mã bệnh ICD-10 chính (`txtMaIcdChinh`) và các bệnh kèm theo vào bảng `ChanDoan` thông qua `AddChanDoanHandler`.
  - Lưu hình thức xử trí, kết quả điều trị, lời dặn bác sĩ.
  - Nếu kết luận hoàn thành: Kích hoạt `CompleteKhamHandler` chuyển trạng thái lượt khám thành `DA_KHAM`.
  - Nếu chuyển viện/chuyển khoa: Kích hoạt `TransferLuotKhamHandler` qua `TransferPhongSelectHopThoai`.
- **Tầng nghiệp vụ sử dụng:**
  - `vn.clinicmanagement.application.examination.AddChanDoanHandler`
  - `vn.clinicmanagement.application.examination.CompleteKhamHandler`
  - `vn.clinicmanagement.application.examination.LockEMRHandler`
  - `vn.clinicmanagement.application.examination.TransferLuotKhamHandler`

---

## 3. LỘ TRÌNH ĐẤU NỐI KỸ THUẬT (COMPOSITION ROOT)
Xây dựng và hoàn thiện lớp `KhamBenhGhepNoiGoc` (tương tự như `VienPhiGhepNoiGoc` và `TiepDonGhepNoiGoc`):
```java
public record ExaminationServices(
    StartKhamHandler startExam,
    CompleteKhamHandler completeExam,
    AddChanDoanHandler addDiagnosis,
    PrescribeThuocHandler prescribeMedication,
    GetDonThuocTheoLuotKhamHandler getPrescriptions,
    CancelDonThuocHandler cancelPrescription,
    YeuCauDichVuHandler orderService,
    GetDichVuTheoLuotKhamHandler getServices,
    CancelDichVuYeuCauHandler cancelService,
    UpdateEMRLamSangRecordHandler updateEmr,
    TransferLuotKhamHandler transferVisit
) {}
```
Mọi màn hình con của `KhamBenhManHinh` nhận `ExaminationServices` qua Dependency Injection và thực thi qua SwingWorker không chặn luồng EDT.
