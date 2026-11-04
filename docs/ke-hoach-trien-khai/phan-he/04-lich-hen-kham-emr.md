# Phân hệ 04 — Lịch hẹn, khám lâm sàng và EMR

**Trạng thái:** `[ ] Chưa làm end-to-end`  
**Đợt:** 2–3.  
**Domain:** `appointment`, `examination`, một phần `patient`, `prescription`.

## Hiện có

- Models: `LichHen`, `LuotKham`, `ChanDoan`, `ICD`, `SinhHieu`, `DonThuoc`, `ChiTietDonThuoc`; các enums trạng thái; `ClinicalDrugSafetyService`.
- UI: `DoctorStationPanel`, `ExaminationPanel`, clinical tabs, các dialog chẩn đoán/đặt dịch vụ/kê đơn/xử trí, print previews.
- `DoctorStationPanel` có dữ liệu bệnh nhân dựng sẵn và một số tab placeholder; `ExaminationPanel` có nhiều dialog nhưng cần xác định phần nào được nối vào cùng use case.
- Chưa thấy màn hình lịch hẹn độc lập và chưa thấy các use case lưu EMR/đơn thuốc trong Application.

## Backlog theo model

- [ ] **`LichHen`:** calendar theo ngày/tuần, bác sĩ/phòng; đặt/đổi/hủy; chống trùng; đánh dấu đến/không đến; tạo lượt khám.
- [ ] **`LuotKham`:** truy vấn hàng đợi, xem hồ sơ đang khám, lưu nháp, hoàn tất và khóa; lịch sử chuyển trạng thái.
- [ ] **`ChanDoan` + `ICD`:** chọn ICD từ danh mục thật; phân biệt chẩn đoán chính/phụ; kiểm tra mã còn hiệu lực; lưu người chẩn đoán.
- [ ] **`SinhHieu`:** hiển thị thời điểm và người đo; hỗ trợ đo nhiều lần; đánh dấu bất thường theo ngưỡng cấu hình.
- [ ] **`DonThuoc` + `ChiTietDonThuoc`:** tìm item, liều/tần suất/đường dùng/số ngày; kiểm tra dị ứng, liều và trùng thuốc; duyệt/hủy/sửa theo trạng thái.
- [ ] **Khóa EMR:** lệnh ký/khóa, người ký, thời gian, chữ ký/biên nhận nếu có; mở lại phải có quyền và lý do; ghi `NhatKyHoSoBenhAn`.

## Màn hình cần bổ sung hoặc hoàn thiện

1. **Lịch khám:** tháng/tuần/ngày, bộ lọc bác sĩ/phòng/cơ sở, tạo và đổi lịch; có thể hoãn nếu MVP chỉ tiếp nhận walk-in.
2. **Chi tiết lượt khám:** timeline bệnh án, dữ liệu dị ứng/BHYT/sinh hiệu, trạng thái save/sync và lịch sử thao tác.
3. **Tab kết quả/Phản hồi bác sĩ:** hiện tab ở `DoctorStationPanel` là placeholder; cần nối kết quả CLS và trạng thái đã đọc.
4. **Ký/khóa EMR:** xác nhận tóm tắt trước khóa, thông báo dữ liệu còn thiếu; trang tra cứu audit/read-only.
5. **Đơn thuốc:** màn hình soát/duyệt nếu người kê và người duyệt là vai trò khác; xem lịch sử cấp phát sau khi tích hợp kho.

## Nghiệm thu

- [ ] Hồ sơ nháp, đã hoàn tất và đã khóa có trạng thái phân biệt và tải lại chính xác.
- [ ] Người không có quyền không sửa được EMR đã khóa; mở lại để lại lý do và audit.
- [ ] Kê đơn hiển thị cảnh báo an toàn trước khi duyệt; không phụ thuộc data mẫu.
- [ ] Lịch hẹn chuyển thành lượt khám không tạo duplicate khi thao tác lặp.

