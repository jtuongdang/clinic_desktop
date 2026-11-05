# 02 — Các đợt triển khai

Lộ trình đã duyệt gồm **10 giai đoạn (Phase 0–9)**, chia theo vertical slice và dependency nghiệp vụ. Trạng thái dưới đây phản ánh bằng chứng trong checkout; code/model/UI hiện diện không tự được tính là nghiệm thu.

## Phase 0 — Nền tảng, schema, giao dịch và xác thực

**Trạng thái:** `[~]` Có nền tảng trong source; chưa có bằng chứng hoàn tất kiểm thử/phát hành.
**Phụ thuộc:** Không.

**Model/phạm vi:** Migration/schema; `Result`, command/query, handler; transaction boundary; `NhanVien`, `VaiTro`; xác thực.
**Deliverable:** Maven modules; SQL Server JDBC, migration runner V1–V4, transaction manager/unit of work; luồng đăng nhập nối application/repository.

- [ ] Build/review bằng cấu hình dự án và xác nhận migration từ DB thử nghiệm sạch.
- [ ] Xác nhận transaction manager/rollback, cấu hình secret, audit và cơ chế cấp quyền trước khi dùng dữ liệu thật.

**Điều kiện qua phase:** Build và test CI đạt; migrations chạy idempotent trên DB thử nghiệm; xác thực/persistence và rollback có test phù hợp; cấu hình secret và quyền được xác nhận.

## Phase 1 — Hồ sơ bệnh nhân, BHYT và tiếp đón vào hàng đợi

**Trạng thái:** `[~]` Có phần triển khai và test regression cục bộ; chưa nghiệm thu end-to-end.
**Phụ thuộc:** Phase 0.

**Model/phạm vi:** `BenhNhan`, `BaoHiem`, `CoSo`, `Khoa`, `PhongKham`, `LuotKham`; CCCD/điện thoại; số thứ tự hàng đợi.
**UI/use cases:** `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`; đăng ký, tìm kiếm, queue.

- Source hiện có handler đăng ký/tìm kiếm/đọc queue, JDBC repositories và Swing composition root; queue/search chưa có test trên feature branch hiện tại.
- [x] Có migration V4 unique active CCCD; các test hiện tại xác nhận phone validation, nhánh duplicate application và ánh xạ lỗi unique index bằng synthetic data.
- UI source có hướng dẫn khi CCCD trùng, tìm kiếm bằng CCCD và giữ dữ liệu form; chưa có desktop test xác nhận tương tác UI.
- [ ] Xác nhận save/search/queue trên SQL Server thử nghiệm, cập nhật hồ sơ/BHYT và đọc lại đúng sau restart.
- [ ] Chứng minh rollback khi bước ghi lỗi, cấp queue không trùng dưới concurrency và xử lý duplicate CCCD đồng thời qua DB.
- [ ] Chạy kiểm thử UI phù hợp; review và đồng bộ code lên remote trước khi ghi nhận publication (remote feature ref hiện chưa có).

**Điều kiện qua phase:** Luồng đăng ký/chọn-cập nhật/tìm kiếm chạy từ UI đến SQL Server và đọc lại sau restart; BHYT/visit/queue nhất quán; rollback và concurrency được test; UI lỗi không mất dữ liệu. Test hiện tại chưa đáp ứng các tiêu chí này.

## Phase 2 — Lịch hẹn, sinh hiệu và vòng đời lượt khám

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 1.

**Model/phạm vi:** `LichHen`, `NguonLich`, `LuotKham`, `SinhHieu`, trạng thái lượt khám.
**UI/use cases:** Tiếp đón, `ClinicRoomStatusPanel`, `ExaminationPanel`, màn hình/dialog lịch hẹn.

- [ ] Đặt, đổi, hủy, check-in/no-show; chuyển lịch thành lượt khám không tạo bản ghi trùng.
- [ ] Ghi sinh hiệu với thời điểm và nhân viên thực hiện.
- [ ] Chuyển trạng thái theo quy tắc nghiệp vụ, kiểm soát thao tác đồng thời.

**Điều kiện qua phase:** Lịch/lượt khám/sinh hiệu persist và đọc lại đúng; các chuyển trạng thái hợp lệ được test, gồm conflict, hủy và restart.

## Phase 3 — Khám lâm sàng và EMR

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 2.

**Model/phạm vi:** `ChanDoan`, `ICD`, `TienSuBenh`, `SinhHieu`, phần khám của `LuotKham`, kết quả/hướng xử trí, khóa EMR và lịch sử.
**UI/use cases:** `DoctorStationPanel`, `ExaminationPanel`, clinical tabs/dialogs.

- [ ] Tải đúng queue/lượt khám theo phạm vi bác sĩ/cơ sở.
- [ ] Lưu nháp khám, sinh hiệu và chẩn đoán ICD từ danh mục persist.
- [ ] Hoàn tất/khóa/mở theo quyền và trạng thái; ghi lịch sử thay đổi.
- [ ] Xuất tài liệu từ dữ liệu đã persist, xác nhận biểu mẫu với nghiệp vụ.

**Điều kiện qua phase:** Hồ sơ được lưu/đọc lại, quyền và audit được kiểm tra; khóa EMR ngăn sửa trái phép; có test cho workflow và phục hồi sau restart.

## Phase 4 — Kê đơn và an toàn thuốc

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 3.

**Model/phạm vi:** `DonThuoc`, `ChiTietDonThuoc`, `Item`, `ClinicalDrugSafetyService` và cảnh báo đã được nghiệp vụ xác nhận.
**UI/use cases:** `PrescriptionOrderPanel`, `PrescriptionOrderTabPanel`.

- [ ] Nạp thuốc/danh mục và lưu đơn gắn đúng lượt khám/bác sĩ.
- [ ] Kiểm tra an toàn, liều dùng, trạng thái duyệt/hủy/sửa theo yêu cầu nghiệp vụ.
- [ ] Nối đơn đã duyệt với cấp phát ở Phase 6.

**Điều kiện qua phase:** Đơn và chi tiết persist, đọc lại chính xác; các cảnh báo/trạng thái có test và được xác nhận bởi nghiệp vụ.

## Phase 5 — Chỉ định dịch vụ và cận lâm sàng

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 3.

**Model/phạm vi:** `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu`.
**UI/use cases:** `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel`.

- [ ] Tạo/hủy chỉ định có người thao tác, lý do và trạng thái.
- [ ] Tiếp nhận, nhập, duyệt, trả kết quả; giữ lịch sử sau duyệt.
- [ ] Chốt cách lưu file/hình ảnh trước khi triển khai quy trình liên quan.

**Điều kiện qua phase:** Chỉ định/kết quả nối lượt khám, persist và truy vết được; test chuyển trạng thái, quyền và audit.

## Phase 6 — Kho, nhà thuốc và cấp phát

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 4; kết nối chi phí với Phase 7.

**Model/phạm vi:** `Item`, `Kho`, `TonKho`, lô, phiếu nhập/xuất, nhà cung cấp, `InventoryTransaction`, `FefoAllocationService`.
**UI/use cases:** `PharmacyDispensePanel` và pharmacy components.

- [ ] Nhập hàng, quản lý lô/hạn dùng và nhà cung cấp.
- [ ] Cấp phát đơn đã duyệt theo FEFO, trong transaction nguyên tử.
- [ ] Kiểm kê/điều chỉnh có lý do và audit; ngăn tồn âm.
- [ ] Cảnh báo hết hạn/cận hạn và dưới định mức.

**Điều kiện qua phase:** Sổ tồn và giao dịch khớp sau nhập/xuất/điều chỉnh; test concurrency, rollback và không âm kho.

## Phase 7 — Viện phí, thu ngân và payroll

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 3; nhận nguồn phí từ Phase 5–6; payroll phụ thuộc dữ liệu nhân sự ở Phase 8.

**Model/phạm vi:** `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan`; payroll/ca và thù lao nhân viên theo domain hiện có.
**UI/use cases:** `BillingInvoicePanel`, phiếu thu, màn hình payroll cần hoàn thiện.

- [ ] Tổng hợp chi phí dịch vụ đã thực hiện và thuốc đã cấp.
- [ ] Chốt giá, BHYT, đồng chi trả, giảm trừ, làm tròn, tạm ứng và payroll với nghiệp vụ.
- [ ] Thu nhiều lần, hoàn/điều chỉnh có chứng từ; giữ nguyên audit hóa đơn đã thu.
- [ ] Đối soát báo cáo với giao dịch nguồn.

**Điều kiện qua phase:** Tính tiền/payroll tái lập được từ dữ liệu nguồn, có chứng từ và audit; test điều chỉnh/hoàn/đối soát.

## Phase 8 — Cơ sở, nhân sự, bảo mật và cấu hình

**Trạng thái:** `[ ]` Chưa có bằng chứng quản trị hoàn chỉnh.
**Phụ thuộc:** Phase 0; các phase nghiệp vụ cần phân quyền phải được tích hợp trước phát hành.

**Model/phạm vi:** `CoSo`, `Khoa`, `PhongKham`, `NhanVien`, `VaiTro`, `PhanQuyenCoSo`, `MaTranPhanQuyen`, `CaTruc`, cấu hình/audit.
**UI/use cases:** quản trị cơ sở/phòng, quản lý tài khoản/role và `SystemConfigPanel`.

- [ ] CRUD nhân viên/cơ sở/khoa/phòng và hiệu lực danh mục.
- [ ] Quản lý vai trò/phạm vi; áp quyền ở application/use case, không chỉ ẩn UI.
- [ ] Audit thao tác, che dữ liệu nhạy cảm, bảo vệ secret và cấu hình DB.
- [ ] Quy trình backup/restore và cấu hình triển khai được xác nhận.

**Điều kiện qua phase:** Quyền được kiểm tra trên use case và persistence; audit/cấu hình được rà soát; backup/restore được diễn tập ở môi trường thử nghiệm.

## Phase 9 — Danh mục, báo cáo, tài liệu và phát hành

**Trạng thái:** `[ ]` Chưa có bằng chứng phát hành được nghiệm thu.
**Phụ thuộc:** Các phase cung cấp dữ liệu cho danh mục/báo cáo; Phase 8 cho quyền và cấu hình.

**Model/phạm vi:** danh mục dùng chung; giao dịch khám, CLS, kho, viện phí, nhân sự; DOCX/in ấn/xuất dữ liệu.
**UI/use cases:** catalog CRUD, `ClinicReportPanel`, print preview và tích hợp phát hành.

- [ ] CRUD danh mục có validation, hiệu lực và quyền.
- [ ] Định nghĩa chỉ số báo cáo, truy vấn và đối soát với nguồn; áp phạm vi dữ liệu.
- [ ] Kiểm tra mẫu in/xuất, encoding và tính nhất quán với dữ liệu đã persist.
- [ ] Build/test/release trên CI và môi trường thử nghiệm; ghi nhận review, artifact và hướng dẫn vận hành.

**Điều kiện qua phase:** Báo cáo/tài liệu đối soát được, quyền đúng; build/test/release có bằng chứng tái lập và checklist vận hành được duyệt.
