# Phân hệ 07 — Viện phí và payroll

**Trạng thái:** `[ ] Chưa làm end-to-end`  
**Đợt:** 6 cho billing; payroll ở Đợt 7.  
**Domain:** `billing`, `payroll`, một phần `appointment` (payment interface).

## A. Viện phí

### Models và UI hiện có

- Models: `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan`, enum trạng thái/phương thức thanh toán, `IPaymentGatewayService`.
- UI: `BillingInvoicePanel`, `MedicalBillingSlipPanel`, `MedicalBillingSlipDialog`.
- Bảng hóa đơn/chi tiết hiện có data mẫu; chưa thấy payment gateway hoặc Application use case.

### Cần triển khai

- [ ] Nguồn phát sinh line item: khám, CLS, thuốc/vật tư, điều chỉnh được phép.
- [ ] Snapshot đơn giá, đối tượng, tỷ lệ BHYT, miễn giảm, tạm ứng và cách làm tròn.
- [ ] Lập/đóng/mở lại hóa đơn theo trạng thái và quyền; cấm sửa âm thầm sau khi đã thu.
- [ ] Thu một phần/nhiều lần qua `PhieuThu`; hoàn/điều chỉnh có chứng từ và người duyệt.
- [ ] Theo dõi `CongNoBenhNhan`, hạn thanh toán và lịch sử nhắc nợ nếu cần.
- [ ] Màn hình đối soát theo ca/ngày/phương thức; gateway và hóa đơn điện tử là tích hợp riêng sau khi chốt vendor.

### Màn hình cần bổ sung hoặc nâng cấp

1. Danh sách hóa đơn có filter bệnh nhân/lượt/trạng thái/ngày.
2. Chi tiết hóa đơn với bảng cấu thành tiền và phần người bệnh/BHYT.
3. Màn hình thu tiền/tạm ứng, xác nhận phương thức và in phiếu.
4. Lịch sử thanh toán, hoàn tiền, điều chỉnh và công nợ.
5. Đối soát cuối ca thu ngân.

## B. Payroll

### Models và UI hiện có

- Models: `KyLuong`, `BangLuong`, `ChiTietBangLuong`, `CauHinhHoaHongLoaiDichVu`; dữ liệu nguồn có thể đến từ lịch công, nhân viên và dịch vụ.
- Chưa thấy màn hình payroll riêng. Danh mục nhân sự trong `SystemConfigPanel` không thay thế payroll.

### Cần triển khai và màn hình cần bổ sung

- [ ] Chốt rule lương cơ bản, phụ cấp, ngày công, tăng ca, nghỉ phép và hoa hồng dịch vụ.
- [ ] Cấu hình hoa hồng theo dịch vụ/chức danh/thời hạn.
- [ ] Tạo kỳ lương, tính thử, cảnh báo thiếu dữ liệu, duyệt/chốt kỳ.
- [ ] Chi tiết lương từng nhân viên; tổng hợp theo khoa; export bảng thanh toán.
- [ ] Khóa kỳ lương sau chốt; mọi điều chỉnh hậu kỳ phải tạo phiên bản/điều chỉnh mới.

## Nghiệm thu

- [ ] Tổng hóa đơn bằng tổng các dòng sau giảm trừ; thu/hoàn tiền cân bằng với trạng thái hóa đơn.
- [ ] Retry thao tác thanh toán không ghi nhận thu trùng.
- [ ] Tổng payroll có thể đối chiếu từ rule và dữ liệu nguồn; kỳ đã chốt không bị sửa trực tiếp.
- [ ] Màn hình không tự khẳng định tuân thủ thuế/kế toán nếu chưa được kế toán nghiệp vụ xác nhận.

