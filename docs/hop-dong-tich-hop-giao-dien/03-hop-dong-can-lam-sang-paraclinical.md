# HỢP ĐỒNG 03: PHÂN HỆ CẬN LÂM SÀNG & CHẨN ĐOÁN HÌNH ẢNH (PARACLINICAL MODULE)
**Mã hợp đồng:** HD-INTEG-03 | **Trạng thái:** Sẵn sàng triển khai

---

## 1. MỤC TIÊU TRIỂN KHAI
Nâng cấp phân hệ Cận Lâm Sàng (`ParaclinicalOrderPanel`) từ một bảng danh sách chỉ đọc (Read-only) thành phân hệ điều hành cận lâm sàng hoàn chỉnh:
- Tiếp nhận bệnh nhân đến làm xét nghiệm / siêu âm / X-Quang theo y lệnh bác sĩ.
- Nhập kết quả định lượng, định tính và ghi nhận kết luận của bác sĩ chuyên khoa CĐHA/Xét nghiệm.
- Cập nhật trạng thái phiếu chỉ định từ `CHO_THUC_HIEN` -> `DANG_THUC_HIEN` -> `HOAN_THANH`.

---

## 2. MA TRẬN ĐẤU NỐI MÀN HÌNH - NGHIỆP VỤ - CSDL

### 2.1. Tiếp Nhận Chỉ Định & Quản Lý Hàng Đợi CLS
- **Màn hình:** `ParaclinicalOrderPanel`
- **Cam kết hợp đồng:**
  - Bộ lọc nâng cao: Lọc theo Khoa/Phòng thực hiện (Xét nghiệm huyết học, Sinh hóa, X-Quang, Siêu âm màu), lọc theo ngày và trạng thái phiếu.
  - Khi kỹ thuật viên click vào phiếu chỉ định:
    - Hiển thị chi tiết các chỉ số cần đo.
    - Chuyển trạng thái lượt chỉ định sang `DANG_THUC_HIEN`.

### 2.2. Nhập Kết Quả Cận Lâm Sàng (Record Results Dialog)
- **Màn hình bổ sung:** `ParaclinicalResultEntryDialog` (Hộp thoại nhập kết quả CLS chuẩn EHC)
- **Cam kết hợp đồng:**
  - Form nhập liệu bảng động gồm các cột:
    - *Tên xét nghiệm* (VD: Bạch cầu - WBC, Hồng cầu - RBC, Đường huyết lúc đói)
    - *Kết quả đo* (Trị số thực tế)
    - *Chỉ số bình thường tham chiếu* (VD: 4.0 - 10.0 G/L)
    - *Đơn vị tính* (G/L, mmol/L)
    - *Cảnh báo bất thường* (Tự động tô màu Đỏ nếu vượt ngưỡng, Xanh nếu thấp hơn ngưỡng)
  - Khung nhập: *Mô tả hình ảnh tổn thương* & *Kết luận của Bác sĩ chuyên khoa*.
  - Nút **[Lưu Kết Quả]**:
    - Gọi `RecordServiceResultHandler`.
    - Ghi dữ liệu vào bảng `ChiTietKetQuaXetNghiem`.
    - Đổi trạng thái `ChiDinhDichVu.trangThai = 'HOAN_THANH'`.
    - Đồng bộ kết quả này lập tức hiển thị trên Tab `LabOrderTabPanel` tại bàn khám của bác sĩ điều trị.

---

## 3. TẦNG NGHIỆP VỤ & SERVICES SỬ DỤNG
Xây dựng `ParaclinicalCompositionRoot`:
```java
public record ParaclinicalServices(
    GetServicesByVisitHandler getServices,
    RecordServiceResultHandler recordResult,
    CancelServiceOrderHandler cancelOrder
) {}
```
- Các bảng CSDL liên quan: `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DanhMucDichVu`, `LuotKham`, `BenhNhan`.
