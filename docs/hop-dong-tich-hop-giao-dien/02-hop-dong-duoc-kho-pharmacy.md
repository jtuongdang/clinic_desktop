# HỢP ĐỒNG 02: PHÂN HỆ QUẢN LÝ DƯỢC & KHO TỦ TRỰC (PHARMACY & INVENTORY MODULE)
**Mã hợp đồng:** HD-INTEG-02 | **Trạng thái:** Sẵn sàng triển khai

---

## 1. MỤC TIÊU TRIỂN KHAI
Chuyển đổi phân hệ Quản lý Nhà thuốc / Tủ trực vật tư (`PharmacyDispensePanel`) từ trạng thái hiển thị dữ liệu tĩnh sang hệ thống vận hành thực tế:
- Nạp danh mục kho và loại phiếu từ CSDL (`Kho`, `DanhMucKho`).
- Hiển thị danh sách đơn thuốc chờ phát và phiếu nhập/xuất kho thực tế.
- Thực hiện nghiệp vụ cấp phát thuốc và tự động trừ số lượng tồn kho theo nguyên tắc FIFO/FEFO.

---

## 2. MA TRẬN ĐẤU NỐI MÀN HÌNH - NGHIỆP VỤ - CSDL

### 2.1. Cây Phân Loại Danh Mục Kho (Pharmacy Category Tree)
- **Màn hình:** `PharmacyCategoryTreePanel`
- **Điểm yếu hiện tại:** Chuỗi cây thư mục tĩnh ("Hóa chất khoa sinh hóa", "Tất Cả Phiếu (1)", "Nhập bù (1)").
- **Cam kết hợp đồng:**
  - Nạp động danh sách kho từ bảng `Kho` trong CSDL `clinicDb` (Ví dụ: *Kho Chẵn*, *Kho Lẻ Nhà Thuốc*, *Tủ Trực Cấp Cứu*, *Kho Hóa Chất*).
  - Phân loại theo loại phiếu: Phiếu nhập kho (`PhieuNhapKho`), Phiếu xuất kho (`PhieuXuatKho`), Đơn thuốc chờ cấp phát (`DonThuoc`).
  - Đếm số lượng phiếu thực tế dạng badge: `Nhập bù (3)`, `Chờ cấp phát (5)`.
  - Khi click vào từng nhánh kho trên cây -> lọc dữ liệu tương ứng trên bảng bên phải.

### 2.2. Bảng Dữ Liệu Phiếu & Cấp Phát Thuốc (Pharmacy Ticket Table)
- **Màn hình:** `PharmacyTicketTablePanel`
- **Điểm yếu hiện tại:** Mới chỉ đọc mẫu từ `PhieuNhapKho`, chưa có luồng cấp phát thuốc cho bệnh nhân.
- **Cam kết hợp đồng:**
  - Bổ sung tab/chế độ **"Cấp Phát Đơn Thuốc"**:
    - Hiển thị danh sách các đơn thuốc đã thanh toán viện phí (`HoaDon.trangThai = 'DA_THANH_TOAN'`).
    - Nút `[Duyệt Cấp Phát Thuốc]`: Kích hoạt `DispensePrescriptionHandler`.
    - Kiểm tra số lượng tồn kho qua `GetStockAvailabilityHandler`.
    - Tự động sinh `PhieuXuatKho` và trừ số lượng trong bảng `TonKho` / `LoThuoc`.
    - Cập nhật trạng thái `DonThuoc.trangThai` thành `DA_CAP_PHAT`.
  - Nghiệp vụ **Nhập Kho**:
    - Kết nối với `ReceiveWarehouseStockHandler` để duyệt nhập kho lô thuốc mới.

---

## 3. TẦNG NGHIỆP VỤ & SERVICES SỬ DỤNG
Xây dựng `PharmacyCompositionRoot`:
```java
public record PharmacyServices(
    DispensePrescriptionHandler dispensePrescription,
    ReceiveWarehouseStockHandler receiveStock,
    GetStockAvailabilityHandler checkStock,
    GetItemCatalogHandler getItemCatalog
) {}
```
- Các bảng CSDL liên quan: `Kho`, `Item`, `LoThuoc`, `TonKho`, `PhieuNhapKho`, `PhieuXuatKho`, `ChiTietXuatKho`, `DonThuoc`.
