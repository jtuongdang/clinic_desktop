# TIÊU CHUẨN KỸ THUẬT & QUY TRÌNH NGHIỆM THU (TECHNICAL STANDARDS & DoD)
**Mã tài liệu:** SPEC-QA-DoD-01 | **Dự án:** EHC Hospital Desktop System

---

## 1. NGUYÊN TẮC KIẾN TRÚC & MẪU THIẾT KẾ (DESIGN PATTERNS)

### 1.1. Kiến Trúc Clean Architecture
- **Desktop UI (`clinic-desktop`)** không được gọi SQL trực tiếp. Tất cả các luồng ghi/xử lý nghiệp vụ phải thông qua các **Command / Query Handlers** tại tầng `clinic-application`.
- Các màn hình chỉ nhận phụ thuộc (Dependency Injection) thông qua `CompositionRoot` (ví dụ `BacSiGhepNoiGoc`, `KhamBenhGhepNoiGoc`, `VienPhiGhepNoiGoc`).

### 1.2. Quy Tắc Luồng Swing (Threading & SwingWorker)
- **Quy tắc vàng:** Không thực hiện bất kỳ lệnh mạng, I/O hoặc truy vấn CSDL nào trên luồng giao diện chính (**Event Dispatch Thread - EDT**).
- **Mẫu thực thi:**
  ```java
  SwingWorker<KetQua<OutputDto>, Void> worker = new SwingWorker<>() {
      @Override
      protected KetQua<OutputDto> doInBackground() {
          return handler.handle(command); // Chạy trên Worker Thread
      }

      @Override
      protected void done() {
          // Trở lại EDT an toàn để cập nhật UI
          try {
              KetQua<OutputDto> res = get();
              if (res.isSuccess()) {
                  updateUiSuccess(res.getValue().get());
              } else {
                  showUiError(res.getError());
              }
          } catch (Exception ex) {
              handleWorkerFailure(ex);
          }
      }
  };
  inFlightWorkers.add(worker);
  worker.execute();
  ```
- **Hủy an toàn khi chuyển View:** Mỗi Panel phải có cơ chế `cancelInFlightWorkers()` trong `onHide()` hoặc khi hủy component để tránh rò rỉ bộ nhớ và xung đột phiên làm việc.

### 1.3. Quản Lý Giao Dịch & Kiểm Toán (Transaction & Audit)
- Mọi thao tác ghi nhiều bảng (VD: Kê đơn = Insert `DonThuoc` + Insert N bản ghi `ChiTietDonThuoc`) phải được bao bọc trong giao dịch `JDBCDonViCuaLamViec`.
- Mọi hành vi cập nhật bệnh án, chỉ định thuốc/CLS đều phải sinh `KiemToanTacNhanNguCanh` với đầy đủ `userId`, `username`, `timestamp`, `ipAddress` ghi vào bảng `NhatKyHeThong`.

---

## 2. TIÊU CHÍ NGHIỆM THU (DEFINITION OF DONE - DoD)

Mỗi hợp đồng thành phần chỉ được coi là **HOÀN THÀNH (DONE)** khi thỏa mãn đầy đủ 6 điều kiện tiên quyết:

- [ ] **DoD-1: 0% Mock Data & 0% Fake Action:** Không còn bất kỳ nhánh rẽ `if ("TEST1"...)`, không còn popup "Đã lưu thành công" mà dữ liệu không thực sự ghi xuống `clinicDb`.
- [ ] **DoD-2: Bảo tồn giao diện 100% (Pixel-Perfect):** Giữ nguyên tỷ lệ, màu sắc, font chữ (`UngDungPhongChu`), icon vector theo thiết kế EHC đã được phê duyệt.
- [ ] **DoD-3: Phản hồi tương tác mượt mà:** Trong quá trình ghi CSDL, các nút bấm phải hiển thị trạng thái xử lý (loading indicator hoặc disable nút tạm thời) để tránh người dùng click đúp (Double-submission).
- [ ] **DoD-4: Xử lý lỗi thân thiện (Graceful Degradation):** Khi mất kết nối CSDL hoặc vi phạm nghiệp vụ (VD: thuốc hết tồn kho, bệnh nhân chưa thanh toán), hệ thống phải hiển thị thông báo lỗi rõ ràng bằng tiếng Việt, không để văng `NullPointerException` hoặc treo ứng dụng.
- [ ] **DoD-5: Bộ kiểm thử tự động (Automated Testing):**
  - Có bài test đơn vị (Unit Test) cho Handler nghiệp vụ mới bổ sung.
  - Có bài test kiểm tra giao diện không giao diện (Headless Swing Test) kiểm tra sự kiện nút bấm và gọi Worker đúng chuẩn.
- [ ] **DoD-6: Reactor Build Success:** Toàn bộ lệnh kiểm thử tổng thể `mvn clean test` trên cả 5 module phải đạt **BUILD SUCCESS** 100%.
