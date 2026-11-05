# 02 — Các đợt triển khai

Lộ trình theo vertical slice và phụ thuộc nghiệp vụ. Trạng thái được cập nhật theo mã nguồn đã kiểm tra trong checkout; việc có code local chưa thay thế review, test và nghiệm thu.

---

## Đợt 0 — Nền tảng, CSDL, giao dịch và xác thực

**Trạng thái:** `[x] Hoàn tất theo phạm vi nền tảng đã thống nhất`
**Phụ thuộc:** Không.

- [x] Maven multi-module và các module Clean Architecture.
- [x] SQL Server JDBC connection và migration runner; migrations hiện có V1–V3.
- [x] Application primitives: `Result<T>`, `Command`, `Query`, handlers và transaction boundary.
- [x] JDBC transaction manager / unit of work.
- [x] Đăng nhập nối application với repository nhân viên từ SQL Server.

**Lưu ý vận hành:** trước khi sử dụng dữ liệu thật vẫn cần hoàn thiện bảo mật, quyền hạn, cấu hình secret và audit ở Đợt 7/Phân hệ 08. Hoàn tất Đợt 0 không đồng nghĩa ứng dụng đạt điều kiện production.

---

## Đợt 1 — Hồ sơ bệnh nhân, BHYT và tiếp nhận vào hàng đợi

**Trạng thái:** `[~] Luồng đã có trong checkout; chờ commit, kiểm thử và nghiệm thu`
**Phụ thuộc:** Đợt 0.

**Model chính:** `BenhNhan`, `BaoHiem`, `CoSo`, `Khoa`, `PhongKham`, `LuotKham`.
**UI chính:** `LoginForm`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`.

- [x] Trong checkout có repository bệnh nhân, bảo hiểm, phòng khám và hàng đợi.
- [x] Có use cases đăng ký, tìm bệnh nhân và đọc hàng đợi.
- [x] Form Tiếp đón được nối qua composition root và dùng SwingWorker cho các thao tác được tích hợp.
- [ ] Chạy build/test phù hợp và kiểm tra luồng đăng ký, tìm lại hồ sơ, cập nhật bệnh nhân, bảo hiểm, lượt khám và queue trên DB thử nghiệm.
- [ ] Kiểm thử rollback khi một bước ghi thất bại; kiểm tra lỗi trùng CCCD và tính duy nhất khi ghi đồng thời.
- [ ] Review, commit phần thay đổi hiện còn local; đồng bộ tài liệu phân hệ sau nghiệm thu.

**Qua đợt khi:** form đăng ký/tìm kiếm tạo hoặc chọn đúng hồ sơ; lượt khám và hàng đợi được lưu nhất quán; đọc lại sau restart đúng; kiểm thử xác nhận transaction rollback.

---

## Đợt 2 — Lịch hẹn, sinh hiệu và vòng đời lượt khám

**Trạng thái:** `[ ] Chưa nghiệm thu end-to-end`
**Phụ thuộc:** Đợt 1.

**Model:** `LichHen`, `LuotKham`, `SinhHieu`, trạng thái lượt khám.
**UI:** `ReceptionPanel`, `ClinicRoomStatusPanel`, `ExaminationPanel`, màn hình/dialog lịch hẹn.

- [ ] Đặt, đổi, hủy lịch; đánh dấu đến/không đến; chuyển lịch thành lượt khám mà không tạo trùng.
- [ ] Ghi sinh hiệu có thời điểm và nhân viên thực hiện.
- [ ] Chuyển trạng thái lượt khám theo quy tắc nghiệp vụ, có kiểm soát tranh chấp khi nhiều người thao tác.
- [ ] Tự làm mới queue hoặc cập nhật sau sự kiện; hiển thị lỗi/mất kết nối.

---

## Đợt 3 — Khám lâm sàng, EMR và kê đơn

**Trạng thái:** `[ ] Chưa nghiệm thu end-to-end`
**Phụ thuộc:** Đợt 2.

**Model:** `ChanDoan`, `ICD`, `TienSuBenh`, `DonThuoc`, `ChiTietDonThuoc`, kết quả và hướng xử trí.
**UI:** `DoctorStationPanel`, `ExaminationPanel`, các clinical tabs.

- [ ] Nạp lượt khám đang chờ và đúng phạm vi bác sĩ/cơ sở.
- [ ] Lưu nháp khám và chẩn đoán ICD từ danh mục thật.
- [ ] Kê đơn, kiểm tra dữ liệu thuốc và các cảnh báo domain đã được xác nhận.
- [ ] Hoàn tất/khóa hồ sơ theo trạng thái và quyền; lưu lịch sử thay đổi.
- [ ] Tích hợp xuất DOCX với dữ liệu đã persist; xác nhận mẫu với người phụ trách nghiệp vụ.

---

## Đợt 4 — Chỉ định dịch vụ và cận lâm sàng

**Trạng thái:** `[ ] Chưa nghiệm thu end-to-end`
**Phụ thuộc:** Đợt 3.

**Model:** `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu`.
**UI:** `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel`.

- [ ] Tạo/hủy chỉ định có người thao tác, lý do và trạng thái.
- [ ] Tiếp nhận, nhập, duyệt và trả kết quả; bảo toàn lịch sử sau duyệt.
- [ ] Xác định thiết kế lưu kết quả hình ảnh/file trước khi triển khai UI.

---

## Đợt 5 — Kho, nhà thuốc và cấp phát

**Trạng thái:** `[ ] Chưa nghiệm thu end-to-end`
**Phụ thuộc:** Đợt 3.

**Model:** `Item`, `Kho`, `TonKho`, phiếu nhập/xuất, lô hàng, `InventoryTransaction`, `FefoAllocationService`.
**UI:** `PharmacyDispensePanel` và các pharmacy components.

- [ ] Quản lý nhập kho/lô/hạn dùng và nhà cung cấp.
- [ ] Cấp phát đơn đã duyệt; kiểm tra tồn theo lô và transaction nguyên tử.
- [ ] Kiểm kê, điều chỉnh có lý do và audit; ngăn tồn kho âm.
- [ ] Cảnh báo cận hạn/hết hạn và dưới định mức.

---

## Đợt 6 — Viện phí và thu ngân

**Trạng thái:** `[ ] Chưa nghiệm thu end-to-end`
**Phụ thuộc:** Đợt 3; kết nối Đợt 4–5 theo nguồn chi phí.

**Model:** `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan`.
**UI:** `BillingInvoicePanel`, phiếu thu và các màn hình liên quan.

- [ ] Tổng hợp khoản thu từ dịch vụ đã thực hiện và thuốc đã cấp.
- [ ] Chốt cách tính giá, BHYT, đồng chi trả, giảm trừ, làm tròn và tạm ứng với nghiệp vụ.
- [ ] Hỗ trợ thu một phần/nhiều lần, hoàn/điều chỉnh có chứng từ; không sửa âm thầm hóa đơn đã thu.
- [ ] Đối soát báo cáo với dữ liệu giao dịch.

---

## Đợt 7 — Quản trị, bảo mật, báo cáo và phát hành

**Trạng thái:** `[ ] Chưa nghiệm thu`
**Phụ thuộc:** Các luồng nghiệp vụ cần báo cáo/quản trị.

- [ ] Quản lý nhân viên, vai trò, phạm vi cơ sở/khoa và quyền theo use case.
- [ ] Ghi audit an toàn; che/mask thông tin nhạy cảm trong log.
- [ ] CRUD danh mục có validation và kiểm soát hiệu lực.
- [ ] Báo cáo vận hành, viện phí, BHYT và kho theo định nghĩa nghiệp vụ đã duyệt.
- [ ] Bảo vệ secret/cấu hình DB, hướng dẫn backup/restore và đóng gói ứng dụng.
- [ ] Kiểm tra build/test/release bằng cấu hình CI và môi trường thử nghiệm.
