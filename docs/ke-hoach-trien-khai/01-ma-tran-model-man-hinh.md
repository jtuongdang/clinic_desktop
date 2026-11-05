# 01 — Ma trận Domain model → Màn hình Desktop → Phần còn thiếu

**Cập nhật:** 05/11/2026
**Quy ước:** “Có UI/model” chỉ xác nhận source có lớp tương ứng; không đồng nghĩa đã nối dữ liệu, kiểm thử hay nghiệm thu nghiệp vụ.

| Phân hệ | Model chính | UI trong source | Trạng thái / phần còn thiếu | Ưu tiên |
|---|---|---|---|---|
| Xác thực | `NhanVien`, `VaiTro`, `NhanVienVaiTro` | `LoginForm`, `UserSession` | Đã có application/repository wiring đăng nhập. Còn quản lý user, phân quyền tập trung, đổi/reset mật khẩu, audit và cấu hình secret an toàn. | P0/P1 |
| Bệnh nhân & BHYT | `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh` | `ReceptionPanel`, `PatientRegistrationFormPanel`, `MedicalPopupChooser` | Có handler đăng ký/tìm kiếm, JDBC repository và UI wiring trong checkout. Test hiện có cho lỗi duplicate CCCD và ánh xạ unique index; migration V4 đặt unique key cho CCCD hồ sơ active. Chưa chứng minh SQL Server save/search/reload, cập nhật hồ sơ/BHYT, concurrency E2E, quyền riêng tư hay lịch sử hồ sơ. | P0 |
| Tiếp đón & hàng đợi | `LuotKham`, `PhongKham`, `CoSo`, `Khoa` | `ReceptionPanel`, `ClinicRoomStatusPanel` | Handler ghi lượt khám và queue repository đọc/ghi đã nối trong source; UI duplicate CCCD hướng người dùng tìm hồ sơ, giữ dữ liệu form, queue/search/save dùng `SwingWorker`. Chưa nghiệm thu SQL Server queue, restart/recovery, concurrency, độ tươi dữ liệu, chuyển trạng thái gọi khám hoặc chuyển phòng. | P1 |
| Lịch hẹn | `LichHen`, `NguonLich`, `LuotKham` | Chưa xác nhận màn hình lịch hẹn chuyên biệt | Đặt/đổi/hủy, chống trùng, check-in/no-show và chuyển thành lượt khám. | P1 |
| Khám bệnh & EMR | `ChanDoan`, `ICD`, `SinhHieu`, `TienSuBenh`, `KetQuaDieuTri`, `HuongXuTri` | `DoctorStationPanel`, `ExaminationPanel`, clinical tabs/dialogs | Nhiều UI/model hiện có nhưng cần nối use cases, quyền theo bác sĩ, lưu nháp, ký/khóa và lịch sử. | P1 |
| Kê đơn | `DonThuoc`, `ChiTietDonThuoc`, `ClinicalDrugSafetyService`, `Item` | `PrescriptionOrderPanel`, `PrescriptionOrderTabPanel` | Cần nạp thuốc/tồn kho thật, kiểm tra an toàn, duyệt/hủy/sửa theo trạng thái và nối cấp phát. | P1 |
| Dịch vụ & CLS | `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem` | `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel` | Cần nối danh mục, trạng thái thực hiện, nhập/duyệt kết quả và thiết kế lưu kết quả hình ảnh. | P1 |
| Kho & nhà thuốc | `Item`, `Kho`, `TonKho`, phiếu nhập/xuất, `InventoryTransaction` | `PharmacyDispensePanel` và components | Cần nhập hàng, lô/hạn, FEFO persistence, kiểm kê, audit và transaction ngăn âm kho. | P1 |
| Viện phí | `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan` | `BillingInvoicePanel`, print panels | Cần chốt quy tắc nghiệp vụ giá/BHYT/đồng chi trả; thu/hoàn/điều chỉnh; đối soát giao dịch. | P1 |
| Nhân sự & phân quyền | `NhanVien`, `VaiTro`, `PhanQuyenCoSo`, `MaTranPhanQuyen`, `CaTruc` | `SystemConfigPanel` | Panel hệ thống không thay thế màn hình CRUD/permission manager; cần quản lý phạm vi và audit. | P1/P2 |
| Báo cáo | Giao dịch từ khám, CLS, kho, viện phí, nhân sự | `ClinicReportPanel` | Cần định nghĩa chỉ số, phân quyền dữ liệu, truy vấn và đối soát với nguồn. | P2 |
| In/xuất tài liệu | Hồ sơ, lượt khám, đơn thuốc, hóa đơn | Core `desktop/core/docx`, print preview dialogs | Có engine DOCX và một số preview. Cần kiểm tra nội dung mẫu, encoding, dữ liệu persist và xác nhận biểu mẫu trước khi tuyên bố đạt chuẩn pháp quy. | P1 |

## Nguyên tắc triển khai UI

1. Swing UI gọi Application use case; không truy cập JDBC trực tiếp.
2. I/O chạy nền (ví dụ `SwingWorker`); cập nhật thành phần giao diện trên EDT.
3. Với mỗi trường form, ghi rõ nguồn model/DTO/schema, nullable, validation và cách xử lý khi cập nhật hồ sơ cũ.
4. Màn hình cần trạng thái loading, empty, error và retry phù hợp; không để lỗi kỹ thuật lộ nguyên văn cho nhân viên.
5. Mức ưu tiên không phải kết luận tuân thủ y tế/pháp lý. Quy tắc BHYT, thuốc, tài chính và biểu mẫu phải được xác nhận nghiệp vụ.

## Bằng chứng task đã có trong checkout

- **Điện thoại:** `PhoneNumber` chấp nhận prefix di động Việt Nam được hỗ trợ với `0` hoặc `+84`, loại bỏ dấu cách/chấm/gạch nối; `PhoneNumberTest` bao phủ các trường hợp này và các đầu vào sai. Đây là bằng chứng validation domain, không phải nghiệm thu toàn bộ form/persistence.
- **UI trùng bệnh nhân:** `ReceptionPanel` nhận diện lỗi duplicate CCCD, hiển thị hướng dẫn tra cứu và gọi search bằng CCCD nhập; `PatientRegistrationFormPanel` giữ form khi báo lỗi. Chưa có desktop test xác nhận hành vi UI.
- **CCCD:** migration V4 dừng nếu phát hiện CCCD trùng trên hồ sơ active trước khi tạo unique key/index. `BenhNhanRepositoryImpl` chỉ ánh xạ vi phạm `UX_BenhNhan_Active_CCCD`; test infrastructure dùng dữ liệu tổng hợp xác nhận không nhầm index khác và không để lộ giá trị trùng. Application test xác nhận nhánh duplicate xảy ra trước bước lưu BHYT/lượt khám.
- **Publication:** các commit phone/UI/CCCD nêu trên có trong lịch sử local của feature branch; commit bổ sung splitter/test GO `74490b3` chỉ nằm trên ref task local riêng. Remote hiện không có ref feature này; chưa có bằng chứng publication/release. Code đã có không đồng nghĩa E2E hoàn tất.
