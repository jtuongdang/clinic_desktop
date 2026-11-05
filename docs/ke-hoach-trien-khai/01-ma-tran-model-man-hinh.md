# 01 — Ma trận Domain model → Màn hình Desktop → Phần còn thiếu

**Cập nhật:** 05/11/2026
**Quy ước:** “Có UI/model” chỉ xác nhận source có lớp tương ứng; không đồng nghĩa đã nối dữ liệu, kiểm thử hay nghiệm thu nghiệp vụ.

| Phân hệ | Model chính | UI trong source | Trạng thái / phần còn thiếu | Ưu tiên |
|---|---|---|---|---|
| Xác thực và access baseline (Phase 2) | `NhanVien`, `VaiTro`, `NhanVienVaiTro`, `PhanQuyenCoSo`, `MaTranPhanQuyen`, audit | `LoginForm`, `UserSession`, `SystemConfigPanel` | Authentication handler/PBKDF2 unit tests có tại `9ff7ddd`; persistence, session/role/scope enforcement, kiểm tra quyền tại use case và audit vẫn chưa xác minh. Đây là gate bắt buộc trước EMR (Phase 4), prescription/pharmacy (Phase 6), billing (Phase 7). | P0, gate |
| Bệnh nhân & BHYT (Phase 1) | `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh` | `ReceptionPanel`, `PatientRegistrationFormPanel`, `MedicalPopupChooser` | Tại `99d709b`: 14 test source/66 discovered (1 opt-in skipped mặc định). SQL Server synthetic E2E save/search/update/reload pass; multi-table rollback và concurrency cũng đã kiểm tra. Còn thiếu process restart, privacy/audit và UI end-to-end. | P1 |
| Tiếp đón & hàng đợi (Phase 1) | `LuotKham`, `PhongKham`, `CoSo`, `Khoa` | `ReceptionPanel`, `ClinicRoomStatusPanel` | Handler ghi lượt khám và queue repository đọc/ghi đã nối; UI duplicate CCCD hướng tìm hồ sơ, giữ form, queue/search/save dùng `SwingWorker`. `PatientRegistrationFormPanelTest` bổ sung 5 headless tests, gồm room selection theo giả định deterministic đã sửa. Chưa nghiệm thu SQL Server queue, restart/recovery, queue/CCCD concurrency, độ tươi dữ liệu, trạng thái gọi khám hoặc chuyển phòng. | P1 |
| Lịch hẹn & vòng đời lượt khám (Phase 3) | `LichHen`, `NguonLich`, `LuotKham`, `SinhHieu` | Chưa xác nhận màn hình lịch hẹn chuyên biệt | Đặt/đổi/hủy, chống trùng, check-in/no-show, sinh hiệu và chuyển thành lượt khám; phụ thuộc Phase 1 và security baseline Phase 2. | P1 |
| Khám bệnh & EMR (Phase 4) | `ChanDoan`, `ICD`, `SinhHieu`, `TienSuBenh`, `KetQuaDieuTri`, `HuongXuTri`, audit | `DoctorStationPanel`, `ExaminationPanel`, clinical tabs/dialogs | Cần nối use cases, quyền theo actor/scope, lưu nháp, ký/khóa và audit. Không mở workflow trước Phase 2 gate. | P1 |
| Catalogs, dịch vụ & CLS (Phase 5) | `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu`, `ICD` | `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel` | Cần nối danh mục, trạng thái thực hiện, nhập/duyệt kết quả, audit/version và thiết kế lưu kết quả hình ảnh. | P1 |
| Kê đơn, kho & nhà thuốc (Phase 6) | `DonThuoc`, `ChiTietDonThuoc`, `Item`, `Kho`, `TonKho`, phiếu nhập/xuất, `InventoryTransaction` | `PrescriptionOrderPanel`, `PrescriptionOrderTabPanel`, `PharmacyDispensePanel` | Cần nạp thuốc/tồn kho thật, kiểm tra an toàn, duyệt/cấp phát, FEFO persistence, kiểm kê, audit và transaction ngăn âm kho. Bị chặn bởi Phase 2 gate. | P1 |
| Viện phí (Phase 7) | `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan` | `BillingInvoicePanel`, print panels | Cần chốt quy tắc giá/BHYT/đồng chi trả; thu/hoàn/điều chỉnh; đối soát giao dịch. Bị chặn bởi Phase 2 gate. | P1 |
| HR/payroll và extended administration (Phase 8) | Nhân sự, lịch ca, chấm công, nghỉ phép, payroll, `CoSo`/`Khoa`/`PhongKham`, cấu hình | `SystemConfigPanel` | Panel hệ thống không thay thế CRUD/admin/payroll; quản trị mở rộng sau security baseline Phase 2. | P1/P2 |
| Báo cáo, tài liệu, tích hợp và release hardening (Phase 9) | Giao dịch từ khám, CLS, kho, viện phí, HR; DOCX/integrations | `ClinicReportPanel`, core `desktop/core/docx`, print preview dialogs | Có engine DOCX và một số preview. Cần xác nhận truy vấn theo quyền, đối soát, nội dung/mẫu, encoding, dữ liệu persist và hardening release. | P2 |

## Nguyên tắc triển khai UI

1. Swing UI gọi Application use case; không truy cập JDBC trực tiếp.
2. I/O chạy nền (ví dụ `SwingWorker`); cập nhật thành phần giao diện trên EDT.
3. Với mỗi trường form, ghi rõ nguồn model/DTO/schema, nullable, validation và cách xử lý khi cập nhật hồ sơ cũ.
4. Màn hình cần trạng thái loading, empty, error và retry phù hợp; không để lỗi kỹ thuật lộ nguyên văn cho nhân viên.
5. Mức ưu tiên không phải kết luận tuân thủ y tế/pháp lý. Quy tắc BHYT, thuốc, tài chính và biểu mẫu phải được xác nhận nghiệp vụ.

## Bằng chứng hiện tại tại code snapshot `99d709b`

- **Điện thoại:** `PhoneNumber` chấp nhận prefix di động Việt Nam được hỗ trợ với `0` hoặc `+84`, loại bỏ dấu cách/chấm/gạch nối; `PhoneNumberTest` bao phủ các trường hợp này và các đầu vào sai. Đây là bằng chứng validation domain, không phải nghiệm thu toàn bộ form/persistence.
- **UI trùng bệnh nhân:** `ReceptionPanel` nhận diện lỗi duplicate CCCD, hiển thị hướng dẫn tra cứu và gọi search bằng CCCD nhập; `PatientRegistrationFormPanel` giữ form khi báo lỗi. `PatientRegistrationFormPanelTest` chạy headless kiểm tra form, validation và chọn phòng theo fixture deterministic; đây không phải UI/database E2E.
- **CCCD:** migration V4 dừng nếu phát hiện CCCD trùng trên hồ sơ active trước khi tạo unique key/index. `BenhNhanRepositoryImpl` chỉ ánh xạ vi phạm `UX_BenhNhan_Active_CCCD`; test infrastructure dùng dữ liệu tổng hợp xác nhận không nhầm index khác và không để lộ giá trị trùng. Application test xác nhận nhánh duplicate xảy ra trước bước lưu BHYT/lượt khám.
- **Branch/test:** Code snapshot `99d709b`; 14 test source/66 discovered: domain 3/16, application 5/26, infrastructure 4/17, desktop 2/7. `mvn clean verify` pass: 0 failures/errors and 1 SQL Server opt-in skip. Opt-in SQL Server synthetic persistence/update E2E pass; separate manual SQL checks covered rollback and queue/CCCD concurrency.
- **Publication/acceptance:** commits remain local. Fresh composition reload is not process restart; UI-to-database end-to-end and Phase 0/1 acceptance are still open.
