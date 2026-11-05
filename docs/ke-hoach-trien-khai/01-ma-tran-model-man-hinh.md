# 01 — Ma trận Domain model → Màn hình Desktop → Phần còn thiếu

**Cập nhật:** 05/11/2026
**Quy ước:** “Có UI/model” chỉ xác nhận source có lớp tương ứng; không đồng nghĩa đã nối dữ liệu, kiểm thử hay nghiệm thu nghiệp vụ.

| Phân hệ | Model chính | UI trong source | Trạng thái / phần còn thiếu | Ưu tiên |
|---|---|---|---|---|
| Xác thực | `NhanVien`, `VaiTro`, `NhanVienVaiTro` | `LoginForm`, `UserSession` | Đã có application/repository wiring đăng nhập. Còn quản lý user, phân quyền tập trung, đổi/reset mật khẩu, audit và cấu hình secret an toàn. | P0/P1 |
| Bệnh nhân & BHYT | `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh` | `ReceptionPanel`, `PatientRegistrationFormPanel`, `MedicalPopupChooser` | Có luồng đăng ký/tìm kiếm trong checkout. Cần kiểm thử mapping/persistence, chống trùng CCCD đồng thời, quyền riêng tư và xử lý lịch sử hồ sơ. | P0 |
| Tiếp đón & hàng đợi | `LuotKham`, `PhongKham`, `CoSo`, `Khoa` | `ReceptionPanel`, `ClinicRoomStatusPanel` | Có đăng ký tạo lượt khám và đọc queue trong checkout. Chưa nghiệm thu tranh chấp/độ tươi dữ liệu, chuyển trạng thái gọi khám, chuyển phòng và queue recovery. | P1 |
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
