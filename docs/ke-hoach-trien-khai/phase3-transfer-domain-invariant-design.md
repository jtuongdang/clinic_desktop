# VIS-TRANSFER-DOMAIN-01 — Phase 3 Visit Transfer Domain & Authorization Invariants Review

**Tài liệu:** Báo cáo rà soát và thiết kế bất biến miền (Domain & Authorization Invariants Design) cho luồng điều chuyển lượt khám (`LuotKham`)<br>
**Mã task:** `VIS-TRANSFER-DOMAIN-01`<br>
**Hợp đồng cơ sở:** `VIS-TRANSFER-SOURCE-RECON-01` (Approved Contract v1)<br>
**Trạng thái:** Báo cáo phân tích kỹ thuật và thiết kế khế ước bất biến miền. Tài liệu tài liệu hóa thuần túy (Documentation only); không sửa đổi mã nguồn, không sửa đổi schema CSDL, không nới lỏng chính sách an ninh.<br>
**Cổng an ninh (Security Gate):** Mọi luồng điều chuyển tại runtime tiếp tục bị từ chối (`DENY`) theo cổng an ninh SEC-01 ([`phase2-implementation-contract.md:32-50`](phase2-implementation-contract.md#L32-L50)) cho đến khi toàn bộ guard, handler, audit, persistence và bộ test nghiệm thu được triển khai đầy đủ.

---

## 1. Tóm tắt điều hành & Mục tiêu tài liệu

Báo cáo này thực hiện rà soát chuyên sâu mã nguồn hiện hành và thiết kế các **bất biến miền (Domain Invariants)** và **bất biến phân quyền tầng ứng dụng (Application Authorization Invariants)** cho use case điều chuyển lượt khám (`Transfer Visit`).

Nhiệm vụ tuân thủ các nguyên tắc cốt lõi:
1. **Bảo toàn nguyên vẹn toàn bộ quyết định của Chủ dự án (Owner):** Tái hiện chính xác các quy tắc đã được phê duyệt về vai trò (`RECEPTIONIST`, `DOCTOR`), trạng thái hợp lệ, phạm vi cơ sở/khoa, bắt buộc chọn phòng hoạt động, khóa EMR, số thứ tự hàng đợi, bảo lưu cờ ưu tiên, bảo toàn chỉ định/thu phí và tính nguyên tử giao tác. Tuyệt đối không mở lại (reopen) bất kỳ quyết định chính sách nào.
2. **Phân định minh bạch các cấp độ thông tin:**
   - **Thực tế mã nguồn (Source Facts):** Hiện trạng code, schema, interface và test hiện hữu với đường dẫn và line anchor đã đối soát.
   - **Chính sách Owner đã duyệt (Approved Policy):** Các bất biến nghiệp vụ và an ninh có hiệu lực bắt buộc.
   - **Khoảng trống & Đề xuất kỹ thuật chưa ngã ngũ (Unresolved Choices & Technical Proposals):** Phân định rạch ròi các phương án kỹ thuật hoặc lựa chọn tác nghiệp còn mở (chuyển tiếp trạng thái, ranh giới ngày, cùng phòng, phạm vi số thứ tự, mã lý do, bảng lịch sử) — không tự ý gán nhãn là quyết định đã phê duyệt.
3. **Chỉ ra các khoảng trống và hành vi vắng mặt (Absent Behavior):** Liệt kê chi tiết những thành phần chưa có trong mã nguồn (absence claims) để làm tiền đề cho pha hiện thực hóa mã sau này.

---

## 2. Đối chiếu hiện trạng mã nguồn (Source Facts & Line Anchors)

Dưới đây là bảng đối soát chi tiết hiện trạng các thành phần mã nguồn trong checkout hiện tại liên quan trực tiếp đến luồng điều chuyển:

| Phân hệ / Thành phần | Vị trí mã nguồn (File & Line Anchor) | Hiện trạng thực tế (Source Fact) | Hành vi còn thiếu / Vắng mặt (Absent Behavior) |
|---|---|---|---|
| **Domain Aggregate `LuotKham`** | [`LuotKham.java:29-55`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L29-L55)<br>[`LuotKham.java:88-124`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L88-L124)<br>[`LuotKham.java:126-221`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L126-L221) | Thực thể lưu giữ `maCoSo`, `maKhoa`, `maPhongKham` (nullable), `trangThai`, `soThuTu`, `soThuTuDisplay`, `dnVnTien`, và cờ `emrLocked`. Có các phương thức `startExam()` (chỉ từ `CHO_KHAM`), `completeExam()` (chuyển sang `HOAN_THANH`), `lockEmr()`. Toàn bộ setter trạng thái/vị trí là public không có guard. | Không có phương thức `transferTo(...)`; không có sự kiện miền `LuotKhamTransferredEvent`; không có kiểm tra guard hợp lệ cho điều chuyển; không lưu vết lịch sử chuyển phòng/khoa. |
| **Hạ tầng Base Entity** | [`CoSoThucThe.java:15-35`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java#L15-L35)<br>[`CoSoThucThe.java:62-93`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java#L62-L93) | Cung cấp trường khóa lạc quan `version` (kiểu `Long`), cơ chế soft delete (`isDeleted`, `deletedAt`, `deletedBy`), audit trail (`createdAt`, `updatedAt`), và danh sách `domainEvents`. | Chưa có cơ chế compare-and-swap (CAS) chuyên biệt hay phương thức snapshot vị trí phục vụ điều chuyển. |
| **Enum Trạng thái Lượt khám** | [`TrangThaiLuotKham.java:3-10`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L3-L10)<br>[`TrangThaiLuotKham.java:23-28`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L23-L28) | Định nghĩa 7 trạng thái: `CHO_KHAM(1)`, `DANG_KHAM(2)`, `DA_KHAM(3)`, `KET_LUAN(4)`, `HOAN_THANH(5)`, `DA_HUY(6)`, `CHO_CLS(7)`. | Chỉ là danh mục enum lưu trữ thuần túy, không có state machine, không định nghĩa ma trận chuyển tiếp trạng thái (transition rules). |
| **Mô hình Cơ sở & Khoa phòng** | [`PhongKham.java:4-22`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/PhongKham.java#L4-L22)<br>[`Khoa.java:6-27`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/Khoa.java#L6-L27) | `PhongKham` liên kết tới `Khoa` qua `maKhoa`, có cờ `hoatDong`. `Khoa` liên kết tới `CoSo` qua `maCoSo`, có cờ `hoatDong`. | Chưa có query/validator kiểm tra phân cấp hợp lệ giữa phòng khám - khoa - cơ sở trong use case điều chuyển. |
| **Repository Port Domain** | [`ILuotKhamRepository.java:6-9`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILuotKhamRepository.java#L6-L9) | Chỉ khai báo hai phương thức: `getVisitsByDoctor(maBacSi, date)` và `getNextQueueNumber(maCoSo, maKhoa, date)`. | Không có phương thức cập nhật điều chuyển (`transferVisit`), không có phương thức cấp số hàng đợi theo phòng, không tìm thấy class triển khai port này trong repository. |
| **Ngữ cảnh Phân quyền** | [`PhanQuyenNguCanh.java:5-17`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationContext.java#L5-L17) | Đối tượng `record` bất biến chứa đúng 1 phiên: `employeeId`, `roleId`, `roleCode`, `facilityId`, `departmentId`, `sessionId`. | Đại diện cho phiên đơn; không chứa thông tin về phòng khám hiện tại hoặc phòng khám đích. |
| **Bộ thẩm định Phân quyền** | [`PhanQuyenDichVu.java:34-55`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationService.java#L34-L55)<br>[`PhanQuyenDichVu.java:57-115`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationService.java#L57-L115) | Kiểm tra nghiêm ngặt: yêu cầu `targetFacilityId == context.facilityId()` và `targetDepartmentId == context.departmentId()`. Nếu khác, trả về `TARGET_MISMATCH` (dòng 51-54). | Evaluator dùng chung này không thể cho phép `RECEPTIONIST` điều chuyển liên khoa cùng cơ sở. Luồng điều chuyển bắt buộc phải có guard phân quyền chuyên biệt (`TransferAuthorizationGuard`). |
| **Cổng Tiếp đón & Hàng đợi** | [`TiepDonHangDoiCongKetNoi.java:9-15`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/ReceptionQueuePort.java#L9-L15)<br>[`TiepDonHangDoiRepository.java:37-81`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java#L37-L81)<br>[`TiepDonHangDoiRepository.java:125-155`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java#L125-L155) | Cung cấp đăng ký mới và truy vấn hàng đợi theo cặp `(maCoSo, maKhoa)` của ngày hiện tại, sắp xếp theo `ORDER BY l.dnVnTien DESC, l.soThuTu, l.id`. | Không có phương thức cập nhật điều chuyển hàng đợi hoặc chuyển vị trí lượt khám trong persistence adapter. |
| **Cơ sở dữ liệu & Schema SQL** | [`V1__Initial_Clinic_Schema.sql:329-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380) | Bảng `LuotKham` lưu `maCoSo`, `maKhoa`, `maPhongKham` (nullable), `trangThai`, `version`, `soThuTu`, `soThuTuDisplay`, `dnVnTien`, `emrLocked`. Khóa ngoại độc lập. | Không có bảng lưu vết lịch sử điều chuyển (`LichSuDieuChuyenLuotKham`); không có ràng buộc toàn vẹn phức hợp (composite foreign key) giữa `maPhongKham` và `maKhoa`. |
| **Khế ước Kiểm toán (Audit)** | [`KiemToanHanhDongMa.java:23-34`](../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L23-L34)<br>[`KiemToanWriteCongKetNoi.java:3-10`](../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditWritePort.java#L3-L10) | Hỗ trợ ghi audit trong transaction hoặc độc lập. Đã có mã `VISIT_REGISTERED`. | Chưa có mã hành động `VISIT_TRANSFERRED`; chưa có định nghĩa payload metadata chuẩn cho điều chuyển. |
| **Giao diện Desktop Prototype** | [`BenhNhanHangDoiNguCanhMenu.java:71-75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75)<br>[`KhamBenhManHinh.java:1080-1091`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091)<br>[`TransferPhongSelectHopThoai.java:15-34`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34) | Menu chuột phải "Chuyển Bệnh Nhân Sang Phòng Khác" mở dialog chọn tên phòng từ danh sách chuỗi tĩnh và mở `TransferPhieuPrintPreviewHopThoai`. | Chỉ là giao diện mô phỏng (UI prototype/preview); hoàn toàn không gọi Application service, không truyền `visitId`, không kiểm tra quyền, không cập nhật CSDL. |
| **Màn hình Tiếp đón Desktop** | [`TiepDonManHinh.java:77-115`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L77-L115) | Tải phòng khám và hàng đợi theo session; xử lý đăng ký bệnh nhân mới. | Hoàn toàn chưa có nút bấm, menu hoặc chức năng điều chuyển lượt khám. |

---

## 3. Các quyết định của Owner đã phê duyệt (Approved Policies - Cố định)

Theo hợp đồng đã duyệt `VIS-TRANSFER-SOURCE-RECON-01` và các văn bản nền [`phase2-role-permission-matrix-approval.md:1-27`](phase2-role-permission-matrix-approval.md#L1-L27), [`phase3-visit-transfer-decision-gaps.md:21-37`](phase3-visit-transfer-decision-gaps.md#L21-L37), các quyết định sau đây là **tiền đề cố định tuyệt đối, không được mở lại hay thay đổi**:

1. **Ma trận trạng thái đủ điều kiện theo vai trò (Role-Status Eligibility):**
   - Vai trò `RECEPTIONIST`: Chỉ được phép điều chuyển các lượt khám đang ở trạng thái `CHO_KHAM`. Mọi trạng thái khác đều bị từ chối (`DENY`).
   - Vai trò `DOCTOR`: Được phép điều chuyển các lượt khám ở trạng thái `CHO_KHAM`, `DANG_KHAM`, hoặc `CHO_CLS`.
   - Các vai trò khác (`ADMIN`, `PHARMACIST`, `CASHIER`, `LAB_TECH`): Hoàn toàn bị cấm (`DENY`) thực hiện điều chuyển.
   - *Lưu ý về chuyển tiếp trạng thái / mốc thời gian (Unresolved):* Việc quyết định lượt khám ở trạng thái `DANG_KHAM` hoặc `CHO_CLS` khi chuyển sang phòng mới sẽ bảo toàn nguyên trạng (`DANG_KHAM`/`CHO_CLS`) hay chuyển tiếp bàn giao (reset về `CHO_KHAM` và xóa/ghi nhận lại mốc bắt đầu khám tại phòng đích) là lựa chọn khế ước tác nghiệp chưa ngã ngũ; không có quy tắc reset tự động nào được phê duyệt.
2. **Các trạng thái cấm tuyệt đối (Hard Ineligible Statuses):**
   - Lượt khám ở các trạng thái kết thúc/hoàn tất: `DA_KHAM`, `KET_LUAN`, `HOAN_THANH` luôn bị từ chối.
   - Lượt khám đã bị hủy (`trangThai == DA_HUY`) hoặc đã bị xóa mềm (`isDeleted == true`) luôn bị từ chối.
   - Lượt khám đã bị khóa bệnh án điện tử (`emrLocked == true`) luôn bị từ chối trong mọi trường hợp.
3. **Quy tắc phạm vi cơ sở, khoa và yêu cầu phòng khám đích:**
   - **Bắt buộc chọn phòng khám đích hoạt động:** Phòng khám đích (`destinationRoomId`) là bắt buộc (không được null), phải có `hoatDong == true`, và phải thuộc đúng khoa đích và cơ sở phiên làm việc.
   - **Phạm vi của `RECEPTIONIST`:** Được phép chuyển liên khoa nhưng bắt buộc phải trong cùng một cơ sở y tế (`source.facilityId == destination.facilityId == session.facilityId`). Nghiêm cấm chuyển xuyên cơ sở.
   - **Phạm vi của `DOCTOR`:** Chỉ được phép điều chuyển trong nội bộ khoa của phiên làm việc (`source.facilityId == destination.facilityId == session.facilityId` VÀ `source.departmentId == destination.departmentId == session.departmentId`). Nghiêm cấm chuyển sang khoa khác.
   - **Nguyên tắc cô lập dữ liệu (Zero Target Patient Read):** Thao tác điều chuyển không cấp quyền đọc hồ sơ bệnh nhân, lịch sử khám, hay chi tiết hàng đợi của khoa/phòng đích cho người thực hiện. Biên nhận điều chuyển chỉ trả về kết quả tối giản.
4. **Quy tắc cấp số thứ tự và bảo toàn cờ ưu tiên:**
   - Cấp số thứ tự mới (`soThuTu`, `soThuTuDisplay`) tại phòng khám/khoa đích. Phạm vi bộ đếm số thứ tự (theo phòng khám hay theo khoa) và ngày áp dụng bộ đếm hàng đợi (queue date) là đề xuất kỹ thuật chưa ngã ngũ (unresolved proposal), chưa có quyết định chính sách chốt.
   - Bảo lưu nguyên vẹn cờ ưu tiên hiện tại của lượt khám (`dnVnTien`); tuyệt đối không tự ý bật cờ ưu tiên và không tự ý xóa cờ ưu tiên khi chuyển.
5. **Ràng buộc với chỉ định dịch vụ, đơn thuốc và thanh toán (Linked Work & Billing):**
   - Toàn bộ chỉ định dịch vụ (`ChiDinhDichVu`), đơn thuốc (`DonThuoc`), kết quả CLS và hóa đơn (`HoaDon`) đã phát sinh vẫn gắn liền với lượt khám.
   - Quá trình điều chuyển tuyệt đối không được tự động hủy, không tự sửa đổi, không làm thay đổi trạng thái thanh toán, và không tạo các khoản thu trùng lặp.
   - Các công việc đang chờ thực hiện (pending work) chỉ được chuyển nếu bảo đảm an toàn lâm sàng, giữ nguyên đơn vị thực hiện và trách nhiệm chuyên môn; nếu không an toàn, hệ thống phải chặn điều chuyển hoặc yêu cầu hoàn tất/bàn giao trước.
   - Đơn thuốc đã duyệt/cấp phát, kết quả CLS đã xác nhận và giao dịch tài chính đã ghi nhận là bất biến (immutable); mọi điều chỉnh phải thông qua quy trình riêng có thẩm quyền, đối soát và audit.
6. **Kiểm soát đồng thời, giao tác và thời gian:**
   - Kiểm soát đồng thời bằng khóa lạc quan thông qua `LuotKham.version` (CAS update).
   - Đảm bảo tính nguyên tử trong một `GiaoDichRanhGioi`: cập nhật lượt khám, cấp số hàng đợi và ghi audit. Thất bại ở bất kỳ khâu nào (đặc biệt là audit) phải rollback toàn bộ (fail-closed). (Việc ghi bảng lịch sử chuyên biệt là đề xuất kỹ thuật chưa ngã ngũ).
   - Thời gian kỹ thuật và audit metadata lưu bằng UTC (Instant); thời gian nghiệp vụ phòng khám tính theo múi giờ `Asia/Ho_Chi_Minh`. Ranh giới ngày điều chuyển so với ngày khám (`ngayKham`) vẫn là lựa chọn khế ước chưa ngã ngũ (unresolved contract choice); việc phê duyệt quy chuẩn thời gian UTC/local time không tự động đồng nghĩa với quy tắc chỉ cho phép điều chuyển trong ngày hiện tại.

---

## 4. Thiết kế ánh xạ bất biến miền và bất biến phân quyền (Domain & Application Invariants)

Để bảo đảm tính toàn vẹn hệ thống theo kiến trúc Clean Architecture / DDD, các quy tắc trên được phân rã và chuẩn hóa thành 9 bất biến kỹ thuật:

```
[UI Component: Transfer Dialog]
               |  (TransferLuotKhamCommand)
               v
+-----------------------------------------------------------------------+
| APPLICATION SERVICE LAYER: TransferLuotKhamHandler                       |
|                                                                       |
|  1. Invariant INV-TX-02 (Scope & Actor Guard)                         |
|     - Check session active, role assignment, work scope               |
|     - Verify RECEPTIONIST (same facility) or DOCTOR (same department) |
|     - Fail-closed: deny if target mismatch or scope violated          |
|                                                                       |
|  2. GiaoDichRanhGioi:                                              |
|     a. Load LuotKham with pessimistic/optimistic concurrency lock     |
|     b. Validate INV-TX-03 (EMR Lock & Soft Delete)                    |
|     c. Validate INV-TX-01 (Actor-Status Compatibility)                |
|     d. Validate INV-TX-08 (UTC Metadata & Local Effective Time)       |
|     e. Validate INV-TX-02 (Target Room Active & Belongs to Dept)      |
|     f. Validate INV-TX-05 (Linked Orders/Billing Safety Check)        |
|     g. Invoke Domain Method: visit.transferTo(destDept, destRoom,...) |
|     h. Allocate Atomic Queue Number (INV-TX-04)                       |
|     i. Persist Visit Update (CAS expectedVersion) (INV-TX-06)         |
|     j. Append Audit Log: VISIT_TRANSFERRED (INV-TX-07)                |
|     k. Return Minimal TransferReceipt (INV-TX-09: No PHI/Queue reads) |
+-----------------------------------------------------------------------+
```

### 4.1. Chi tiết từng bất biến kỹ thuật

#### `INV-TX-01`: Bất biến tương thích Vai trò - Trạng thái (Actor-Status Compatibility Invariant)
* **Tầng sở hữu:** Application Guard kết hợp Domain Entity (`LuotKham`).
* **Nội dung:**
  - Một yêu cầu điều chuyển do `RECEPTIONIST` khởi tạo chỉ hợp lệ khi và chỉ khi `visit.trangThai == TrangThaiLuotKham.CHO_KHAM`.
  - Một yêu cầu điều chuyển do `DOCTOR` khởi tạo chỉ hợp lệ khi và chỉ khi `visit.trangThai ∈ {CHO_KHAM, DANG_KHAM, CHO_CLS}`.
  - **Lưu ý về chuyển tiếp trạng thái / mốc thời gian (Unresolved Proposal):** Không có quy tắc chuyển đổi trạng thái tự động (reset) hay xóa/đặt lại `thoiGianBatDauKham` nào được phê duyệt. Việc bảo toàn nguyên vẹn trạng thái (State Preservation: giữ nguyên `DANG_KHAM`/`CHO_CLS` khi bàn giao chuyên môn) hay chuyển tiếp bàn giao (Handoff Transition: reset về `CHO_KHAM` tại phòng đích để bắt đầu phiên khám mới và ghi vết mốc bắt đầu cũ) là một lựa chọn khế ước tác nghiệp còn mở (unresolved choice), tuyệt đối không được coi là quyết định đã duyệt.

#### `INV-TX-02`: Bất biến phân cấp phạm vi và Phòng khám hoạt động (Scope & Room Hierarchy Invariant)
* **Tầng sở hữu:** Application Authorization Guard kết hợp Persistence Validation.
* **Nội dung:**
  - Phòng khám đích `destinationRoomId` không được null, phải tồn tại trong CSDL và có cờ `hoatDong == true`.
  - Phòng khám đích phải thuộc về khoa đích: `PhongKham.maKhoa == destinationDepartmentId`.
  - Khoa đích phải thuộc về cơ sở đích: `Khoa.maCoSo == destinationFacilityId`.
  - Cơ sở đích bắt buộc phải trùng với cơ sở của phiên và cơ sở hiện tại của lượt khám: `destinationFacilityId == context.facilityId() == visit.maCoSo`.
  - Đối với `DOCTOR`: Bắt buộc `destinationDepartmentId == context.departmentId() == visit.maKhoa`.
  - Đối với `RECEPTIONIST`: Cho phép `destinationDepartmentId != visit.maKhoa` nhưng bắt buộc cùng `facilityId`.
  - **Lưu ý về điều chuyển cùng phòng / cấp lại số thứ tự (Unresolved Proposal):** Việc cấm hay cho phép điều chuyển lại vào cùng một phòng khám (same-room requeue / đổi số thứ tự trong cùng phòng: `destinationRoomId == visit.maPhongKham`) là một đề xuất tác nghiệp còn mở (unresolved choice), chưa được Owner phê duyệt thành bất biến bắt buộc.

#### `INV-TX-03`: Bất biến đóng băng Bệnh án & Tính hoàn tất lâm sàng (EMR Lock & Clinical Finality Invariant)
* **Tầng sở hữu:** Domain Aggregate Root (`LuotKham`).
* **Nội dung:**
  - Nếu `visit.isEmrLocked() == true`, mọi nỗ lực điều chuyển phải ném ra ngoại lệ miền `EMRDaKhoaNgoaiLe`, chặn ngay lập tức mà không phụ thuộc vào vai trò hay lý do.
  - Nếu `visit.isDeleted() == true` hoặc `visit.getTrangThai() == TrangThaiLuotKham.DA_HUY`, từ chối với lỗi `VISIT_INACTIVE_OR_CANCELLED`.
  - Nếu `visit.getTrangThai() ∈ {DA_KHAM, KET_LUAN, HOAN_THANH}`, từ chối với lỗi `VISIT_ALREADY_FINALIZED`. Không cho phép điều chuyển các ca khám đã hoàn tất quy trình chuyên môn.

#### `INV-TX-04`: Bất biến cấp số hàng đợi và Bảo lưu độ ưu tiên (Queue Allocation & Priority Preservation Invariant)
* **Tầng sở hữu:** Domain Aggregate Root (`LuotKham`) kết hợp Repository/Persistence Port.
* **Nội dung:**
  - **Chính sách đã phê duyệt:** Lượt khám khi chuyển đến phòng đích phải được cấp một số thứ tự mới (`soThuTu`, `soThuTuDisplay`). Cờ ưu tiên `dnVnTien` được giữ nguyên giá trị ban đầu (`newVisit.dnVnTien == oldVisit.dnVnTien`); hệ thống nghiêm cấm việc tự động nâng cờ ưu tiên (auto-promote) hoặc tự động hạ/xóa cờ ưu tiên khi chuyển.
  - **Khoảng trống đề xuất kỹ thuật (Unresolved Proposal):** Phạm vi sinh chuỗi số thứ tự (queue sequence scope: sinh số theo phòng khám `destinationRoomId` hay theo khoa `destinationDepartmentId`) và ngày dùng để xác định bộ đếm hàng đợi (queue date: theo ngày tiếp nhận ban đầu hay ngày chuyển thực tế) là các đề xuất kỹ thuật chưa ngã ngũ, chưa có quyết định chính sách phê duyệt cuối cùng.

#### `INV-TX-05`: Bất biến an toàn công việc liên kết và Bất biến tài chính (Linked Work Safety & Financial Immutability Invariant)
* **Tầng sở hữu:** Application Domain Service / Dependency Validator.
* **Nội dung:**
  - Toàn bộ danh sách `chiDinhDichVus`, `donThuocs`, `hoaDons` gắn liền với `visitId` được bảo toàn toàn vẹn. Khóa ngoại `maLuotKham` không thay đổi.
  - Không tự động hủy (`void`/`cancel`), không sửa đổi số lượng/đơn giá, không phát sinh dòng chi phí mới và không hoàn phí tự động trong giao tác điều chuyển.
  - Nếu tồn tại dịch vụ CLS đang thực hiện dở dang hoặc đơn thuốc đang trong quy trình phát thuốc mà việc chuyển phòng/khoa gây rủi ro mất an toàn lâm sàng (clinical risk), hệ thống phải từ chối điều chuyển với mã `LINKED_WORK_BLOCKING` và yêu cầu nhân viên xử lý dứt điểm công việc tại nguồn.

#### `INV-TX-06`: Bất biến kiểm soát đồng thời lạc quan (Optimistic Concurrency & CAS Invariant)
* **Tầng sở hữu:** Domain Entity (`CoSoThucThe`) và Infrastructure SQL Adapter.
* **Nội dung:**
  - Lệnh điều chuyển bắt buộc phải mang theo `expectedVersion` của `LuotKham`.
  - Thao tác cập nhật vị trí trong CSDL phải sử dụng cơ chế Compare-And-Swap (CAS):
    ```sql
    UPDATE LuotKham 
    SET maKhoa = ?, maPhongKham = ?, soThuTu = ?, soThuTuDisplay = ?, 
        trangThai = ?, version = version + 1, updatedAt = ?, updatedBy = ?
    WHERE id = ? AND version = ? AND isDeleted = 0
    ```
  - Nếu số dòng ảnh hưởng bằng 0 (`rowsAffected == 0`), hệ thống xác định có xung đột ghi đè đồng thời (Concurrent Modification) và ném lỗi `CONCURRENCY_CONFLICT`, hủy bỏ toàn bộ giao tác.

#### `INV-TX-07`: Bất biến giao tác nguyên tử & Thất bại đóng an toàn (Atomic Transaction & Fail-Closed Audit Invariant)
* **Tầng sở hữu:** Application Service (`GiaoDichRanhGioi`).
* **Nội dung:**
  - Toàn bộ các bước: (1) Kiểm tra điều kiện, (2) Cập nhật `LuotKham`, (3) Cấp số hàng đợi, và (4) Ghi bản ghi kiểm toán `VISIT_TRANSFERRED` (cùng lưu bản ghi lịch sử nếu đề xuất bảng lịch sử chuyên biệt được thông qua) phải nằm trong cùng một giao tác CSDL (`GiaoDichRanhGioi.inTransaction(...)`).
  - Nếu khâu ghi audit thất bại (do lỗi CSDL, vi phạm constraint, hoặc timeout), toàn bộ cập nhật vị trí và hàng đợi phải bị rollback 100%. Không bao giờ chấp nhận tình trạng chuyển vị trí thành công mà không có bản ghi audit đi kèm (Fail-closed principle).

#### `INV-TX-08`: Bất biến chuẩn hóa thời gian kỹ thuật UTC và thời gian nghiệp vụ địa phương (Technical UTC Metadata & Local Business Time Invariant)
* **Tầng sở hữu:** Application Handler / Audit & Domain Event Boundary.
* **Nội dung:**
  - **Phân định rõ thời gian kỹ thuật và thời gian nghiệp vụ:** Toàn bộ mốc thời gian kỹ thuật, audit metadata và thời điểm xảy ra sự kiện điều chuyển (`occurredAtUtc` / `transferredAtUtc`) bắt buộc ghi nhận theo chuẩn UTC Instant (`java.time.Instant`). Toàn bộ mốc thời gian hiển thị hoặc nghiệp vụ khám chữa bệnh tại phòng khám tính theo múi giờ địa phương `Asia/Ho_Chi_Minh` (`LocalDateTime effectiveLocalTime`).
  - **Ranh giới ngày điều chuyển là vấn đề chưa ngã ngũ (Unresolved Choice):** Giới hạn chỉ cho phép điều chuyển trong ngày làm việc hiện tại (`visit.getNgayKham().equals(today)`) hay cho phép điều chuyển cả lượt khám kéo dài qua ngày hoặc lượt khám đặt trước là câu hỏi tác nghiệp còn mở, chưa được phê duyệt. Quyết định của Owner về việc chuẩn hóa metadata UTC và giờ nghiệp vụ `Asia/Ho_Chi_Minh` không mặc định thiết lập hay chốt ranh giới ngày khám. Tuyệt đối không dán nhãn quy tắc chỉ điều chuyển trong ngày là đã duyệt.

#### `INV-TX-09`: Bất biến cô lập dữ liệu & Không tiết lộ hồ sơ đích (Zero Target Patient Read Invariant)
* **Tầng sở hữu:** Application Output Boundary / DTO Receipt.
* **Nội dung:**
  - Luồng điều chuyển tuyệt đối không gọi các phương thức đọc thông tin bệnh nhân của khoa đích, không gọi `getReceptionQueue` hay `searchPatients` của khoa đích.
  - Đối tượng biên nhận trả về (`TransferReceipt`) chỉ chứa các thông tin định danh tối thiểu của giao tác điều chuyển; tuyệt đối không chứa danh sách bệnh nhân chờ, không chứa thông tin bệnh án của bệnh nhân khác tại khoa/phòng đích.

---

### 4.2. Ma trận phân bổ trách nhiệm kiểm soát bất biến (Layer Allocation Matrix)

| Bất biến | Domain Model (`LuotKham`) | Domain Service / Validator | Application Guard / Handler | Persistence / SQL Trigger / Constraint |
|---|:---:|:---:|:---:|:---:|
| **INV-TX-01 (Actor-Status)** | Thẩm định trạng thái hợp lệ trong `transferTo()` | — | Thẩm định role `RECEPTIONIST` vs `DOCTOR` | — |
| **INV-TX-02 (Scope & Room)** | Lưu `maKhoa`, `maPhongKham` | Kiểm tra quan hệ phòng - khoa - cơ sở | Guard kiểm tra `targetFacilityId` & `targetDepartmentId` | FK constraints `maKhoa`, `maPhongKham` |
| **INV-TX-03 (EMR Lock/Final)** | `if (emrLocked) throw EMRDaKhoaNgoaiLe` | — | Chặn trước khi thực hiện logic nặng | Kiểm tra `emrLocked == 0` |
| **INV-TX-04 (Queue & Priority)** | Cập nhật `soThuTu`, giữ nguyên `dnVnTien` | — | Cấp số mới (scope/date là unresolved proposal) | Sequence / Unique index theo quy tắc cấp số |
| **INV-TX-05 (Linked Work)** | Giữ nguyên các danh sách con | Thẩm định an toàn công việc liên kết | Điều phối không thay đổi billing/orders | Ràng buộc toàn vẹn khóa ngoại FK |
| **INV-TX-06 (Concurrency CAS)** | Quản lý `version` (`CoSoThucThe`) | — | Truyền `expectedVersion` vào command | `WHERE id = ? AND version = ?` |
| **INV-TX-07 (Atomic Fail-closed)** | Phát sinh `LuotKhamTransferredEvent` | — | Điều phối trong `GiaoDichRanhGioi` | Database Transaction Rollback |
| **INV-TX-08 (UTC & Local Time)** | Phân định UTC metadata & local time | — | Ghi nhận UTC Instant cho audit; local time cho nghiệp vụ | Audit timestamp UTC; ranh giới ngày khám (unresolved proposal) |
| **INV-TX-09 (Zero Read)** | — | — | DTO Receipt tối giản, không query đọc | Phân quyền truy vấn `WHERE maKhoa = ?` |

---

## 5. Đặc tả Command, Result DTO và Xử lý lỗi an toàn (Command, Result & Safe Failures)

### 5.1. Cấu trúc Lệnh điều chuyển tối giản (`TransferLuotKhamCommand`)

Lệnh điều chuyển được thiết kế là một `record` bất biến, chỉ nhận các trường tối thiểu cần thiết để thẩm định và thực thi:

```java
package vn.clinicmanagement.application.examination.commands;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import java.util.UUID;

/**
 * Minimal immutable command for visit transfer workflow.
 * Conforms to DEC-AUTHZ-04b and SEC-01 gates.
 */
public record TransferLuotKhamCommand(
        // Ngữ cảnh phân quyền & actor từ phiên làm việc (Application Boundary)
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext,

        // Định danh lượt khám và khóa lạc quan
        int visitId,
        long expectedVersion,

        // Đích điều chuyển (bắt buộc phòng khám hoạt động)
        int destinationDepartmentId,
        int destinationRoomId,

        // Đề xuất kỹ thuật (Unresolved Proposal): Mã lý do chuẩn hóa & ghi chú tùy chọn (không chứa PHI)
        String transferReasonCode,
        String optionalNote,

        // Khóa chống lặp giao tác
        UUID idempotencyKey
) {
    public TransferLuotKhamCommand {
        if (authorizationContext == null || !authorizationContext.isValid()) {
            throw new IllegalArgumentException("Ngữ cảnh phân quyền không hợp lệ.");
        }
        if (auditActorContext == null) {
            throw new IllegalArgumentException("Ngữ cảnh kiểm toán không được để trống.");
        }
        if (visitId <= 0) {
            throw new IllegalArgumentException("Mã lượt khám phải là số dương.");
        }
        if (destinationDepartmentId <= 0 || destinationRoomId <= 0) {
            throw new IllegalArgumentException("Khoa và phòng khám đích phải là số dương.");
        }
        // Lưu ý: Yêu cầu bắt buộc transferReasonCode dưới đây là đề xuất kỹ thuật đang chờ chốt:
        if (transferReasonCode == null || transferReasonCode.isBlank()) {
            throw new IllegalArgumentException("Mã lý do điều chuyển không được để trống (theo đề xuất).");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Khóa idempotency không được để trống.");
        }
    }
}
```

### 5.2. Cấu trúc Biên nhận kết quả tối giản (`TransferReceipt`)

Biên nhận điều chuyển chỉ chứa các dữ kiện kỹ thuật của giao tác chuyển phòng; **tuyệt đối không trả về thông tin bệnh nhân hay chi tiết hàng đợi khoa đích**. Metadata thời gian được ghi nhận theo chuẩn UTC Instant, song song với thời gian nghiệp vụ địa phương:

```java
package vn.clinicmanagement.application.examination.dto;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Minimal transfer receipt. Strictly excludes patient identifiers and destination queue lists.
 * Distinguishes UTC metadata instant from local business time.
 */
public record TransferReceipt(
        int visitId,
        int sourceFacilityId,
        int sourceDepartmentId,
        Integer sourceRoomId,
        int destinationFacilityId,
        int destinationDepartmentId,
        int destinationRoomId,
        int oldQueueNumber,
        int newQueueNumber,
        String newQueueDisplay,
        long newVersion,
        Instant transferredAtUtc,         // UTC metadata instant
        LocalDateTime effectiveLocalTime  // Giờ nghiệp vụ phòng khám (Asia/Ho_Chi_Minh)
) {}
```

---

### 5.3. Danh mục kết quả lỗi an toàn (Safe Failure Outcomes & Error Catalog)

Mọi trường hợp vi phạm bất biến miền hoặc bất biến phân quyền phải trả về kết quả thất bại an toàn (`KetQua.failure`), ghi audit an ninh (nếu cần), và bảo đảm không làm biến đổi dữ liệu (Clean Rollback):

| Mã lỗi (Failure Code) | Bất biến vi phạm | Nguyên nhân / Điều kiện kích hoạt | Trạng thái dữ liệu | Thông điệp an toàn cho Client (User-Safe Message) |
|---|---|---|---|---|
| `AUTH_CONTEXT_INVALID` | Session Boundary | Phiên hết hạn, không hợp lệ, hoặc thiếu assignment. | Rollback / No write | "Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại." |
| `TRANSFER_FORBIDDEN_ROLE` | `INV-TX-01` | Vai trò khác `RECEPTIONIST` hoặc `DOCTOR` cố ý gọi điều chuyển. | Rollback / No write | "Bạn không có quyền thực hiện điều chuyển lượt khám." |
| `TRANSFER_SCOPE_MISMATCH` | `INV-TX-02` | `RECEPTIONIST` chuyển khác cơ sở; hoặc `DOCTOR` chuyển khác khoa. | Rollback / No write | "Phạm vi điều chuyển vượt quá quyền hạn của phiên làm việc." |
| `DESTINATION_ROOM_UNAVAILABLE` | `INV-TX-02` | Phòng khám đích không tồn tại, ngừng hoạt động, hoặc không thuộc khoa đích. | Rollback / No write | "Phòng khám đích không khả dụng hoặc không thuộc khoa tiếp nhận." |
| `VISIT_NOT_FOUND` | Persistence | Không tìm thấy `visitId` hoặc lượt khám nằm ngoài cơ sở phiên. | Rollback / No write | "Không tìm thấy thông tin lượt khám trong phạm vi quản lý." |
| `VISIT_STATUS_INELIGIBLE` | `INV-TX-01` | `RECEPTIONIST` chuyển lượt khác `CHO_KHAM`; hoặc `DOCTOR` chuyển lượt đã hoàn thành. | Rollback / No write | "Trạng thái hiện tại của lượt khám không đủ điều kiện để điều chuyển." |
| `EMR_LOCKED_CANNOT_TRANSFER` | `INV-TX-03` | Lượt khám đã bị khóa hồ sơ bệnh án điện tử (`emrLocked == true`). | Rollback / No write | "Bệnh án điện tử đã bị khóa. Không thể thực hiện điều chuyển." |
| `VISIT_INACTIVE_OR_CANCELLED` | `INV-TX-03` | Lượt khám đã bị hủy (`DA_HUY`) hoặc bị xóa mềm (`isDeleted == true`). | Rollback / No write | "Lượt khám đã bị hủy hoặc không còn hiệu lực." |
| `DATE_BOUNDARY_VIOLATION` | `INV-TX-08` (Đề xuất / Unresolved) | Lượt khám ngoài ngày làm việc (nếu áp dụng quy tắc giới hạn ngày khám). | Rollback / No write | "Chỉ được phép điều chuyển các lượt khám trong ngày làm việc hiện tại." (Mã lỗi dự phòng theo đề xuất giới hạn ngày). |
| `LINKED_WORK_BLOCKING` | `INV-TX-05` | Tồn tại chỉ định dịch vụ hoặc đơn thuốc đang xử lý dở dang không thể chuyển. | Rollback / No write | "Lượt khám đang có dịch vụ cận lâm sàng hoặc thuốc chưa hoàn tất xử lý tại nguồn." |
| `CONCURRENCY_CONFLICT` | `INV-TX-06` | Khóa lạc quan không khớp (`expectedVersion != currentVersion`). | Rollback / No write | "Dữ liệu lượt khám đã bị thay đổi bởi thao tác khác. Vui lòng tải lại và thử lại." |
| `AUDIT_APPEND_FAILED` | `INV-TX-07` | Ghi nhật ký kiểm toán thất bại trong giao tác. | **Toàn bộ Rollback** | "Không thể hoàn tất điều chuyển do lỗi ghi vết kiểm toán hệ thống. Thao tác đã được hoàn tác." |
| `IDEMPOTENCY_REPLAY_SUCCESS` | Replay Contract | Gửi lại cùng `idempotencyKey` và cùng nội dung payload. | Giữ nguyên kết quả cũ | Trả về `TransferReceipt` đã lưu thành công trước đó, không sinh side-effect mới. |
| `IDEMPOTENCY_CONFLICT` | Replay Contract | Dùng lại `idempotencyKey` nhưng nội dung payload bị thay đổi. | Rollback / No write | "Khóa yêu cầu bị trùng lặp với nội dung khác. Thao tác bị từ chối." |

---

## 6. Đề xuất kỹ thuật và Nhận diện các lựa chọn tác nghiệp còn mở

Để chuẩn bị cho task triển khai code tiếp theo (sau khi SEC-01 mở), nhóm phân tích đề xuất các phương án kỹ thuật cụ thể cho các điểm chưa có trong mã nguồn hiện hành:

### 6.1. Phương án lưu vết Lịch sử điều chuyển (Transfer History Persistence) (Unresolved Technical Proposal)
* **Trạng thái:** Khoảng trống kỹ thuật chưa ngã ngũ (Unresolved Choice). Hiện tại chỉ có việc ghi nhật ký kiểm toán (Audit Log append) trong giao tác là hành vi bắt buộc đã duyệt. Phương án bổ sung bảng lịch sử chuyên biệt dưới đây là đề xuất kỹ thuật, chưa được phê duyệt thành schema chính thức:
* **Thực tế:** CSDL hiện tại chỉ lưu vị trí hiện hành của `LuotKham` ([`V1__Initial_Clinic_Schema.sql:329-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380)).
* **Lựa chọn 1 (Đề xuất kỹ thuật - Tạo bảng chuyên biệt):** Tạo bảng mới qua Flyway migration: `LichSuDieuChuyenLuotKham` (`id`, `maLuotKham`, `maCoSoNguon`, `maKhoaNguon`, `maPhongNguon`, `maKhoaDich`, `maPhongDich`, `soThuTuCu`, `soThuTuMoi`, `maLyDo`, `ghiChu`, `nguoiChuyen`, `thoiGianChuyen`).
  - *Ưu điểm:* Hỗ trợ nghiệp vụ in phiếu chuyển khoa, thống kê điều phối buồng phòng, và hiển thị lịch sử luân chuyển trên bệnh án mà không cần giải mã Audit Log.
* **Lựa chọn 2 (Tối giản schema - Không tạo bảng mới):** Lưu toàn bộ thông tin nguồn/đích dưới dạng JSON payload trong `AuditLog` với mã `VISIT_TRANSFERRED`.
  - *Nhược điểm:* Khó truy vấn nhanh trên desktop UI; vi phạm nguyên tắc tách biệt giữa Audit Trail (an ninh) và Lịch sử nghiệp vụ (Domain History).

### 6.2. Thiết kế Sự kiện miền (`LuotKhamTransferredEvent`)
* Đề xuất bổ sung sự kiện miền kế thừa `IMienNghiepVuSuKien`, trong đó phân định rõ thời gian kỹ thuật UTC Instant và giờ nghiệp vụ địa phương:
  ```java
  package vn.clinicmanagement.domain.common.events;

  import java.time.Instant;
  import java.time.LocalDateTime;

  /**
   * Đề xuất sự kiện miền khi điều chuyển lượt khám.
   * Phân định rõ thời gian kỹ thuật UTC metadata (occurredAtUtc) và thời gian nghiệp vụ địa phương (effectiveLocalTime).
   */
  public record LuotKhamTransferredEvent(
          int visitId,
          int patientId,
          int sourceFacilityId,
          int sourceDepartmentId,
          Integer sourceRoomId,
          int targetDepartmentId,
          int targetRoomId,
          int transferredBy,
          Instant occurredAtUtc,            // UTC Instant metadata
          LocalDateTime effectiveLocalTime  // Giờ nghiệp vụ phòng khám (Asia/Ho_Chi_Minh)
  ) implements IMienNghiepVuSuKien {}
  ```

### 6.3. Đề xuất Danh mục Mã lý do điều chuyển chuẩn hóa (`TransferLyDoMa`) (Unresolved Technical Proposal)
* **Trạng thái:** Đề xuất kỹ thuật chưa ngã ngũ (Unresolved Proposal). Việc bắt buộc nhập lý do điều chuyển và danh mục mã chuẩn hóa dưới đây chưa phải là quyết định chính sách đã được Owner phê duyệt, mà là khuyến nghị kỹ thuật nhằm ngăn ngừa nhập văn bản tự do chứa thông tin sức khỏe nhạy cảm (PHI) vào audit log:
  1. `CHUYEN_KHOA_CHUYEN_MON`: Chuyển khoa khám chuyên môn sâu hơn.
  2. `YEU_CAU_NGUOI_BENH`: Bệnh nhân hoặc người nhà có nguyện vọng đổi phòng.
  3. `PHONG_KHAM_QUA_TAI`: Điều phối giảm tải cho phòng khám đông bệnh nhân.
  4. `SAI_SOT_TIEP_DON`: Tiếp đón ban đầu phân nhầm chuyên khoa/buồng khám.
  5. `HOI_CHAN_CHUYEN_PHONG`: Chuyển phòng trong nội bộ khoa phục vụ hội chẩn.

---

## 7. Tài liệu hướng dẫn và Căn cứ đã đọc (Guidance & Citations)

Quá trình rà soát và lập báo cáo này đã nghiên cứu và tuân thủ chặt chẽ các tài liệu sau:

1. **Tài liệu quy ước dự án và nền tảng:**
   - `docs/PROJECT_CONVENTIONS.md` (Quy ước nền về phát triển dự án và quy tắc làm việc của Agent).
   - `README.md` (Tổng quan dự án, cấu hình build và thiết lập môi trường phát triển).
   - `DOMAIN_ANALYSIS.md` (Báo cáo phân tích toàn diện kiến trúc package `vn.clinicmanagement.domain`).
   - `ERD_DIAGRAM.md` (Sơ đồ thực thể quan hệ CSDL EHC HIS).
   - `UI_CODING_STANDARDS.md` (Quy chuẩn mã nguồn giao diện Desktop Swing/FlatLaf).
   - `docs/OPERATIONS_RUNBOOK.md` (Sổ tay vận hành, quy trình kiểm thử và giới hạn môi trường).
2. **Kế hoạch triển khai & Ma trận phân quyền:**
   - `docs/ke-hoach-trien-khai/phase2-role-permission-matrix-approval.md` (Phê duyệt ma trận phân quyền Phase 2 và các quyết định DEC-AUTHZ).
   - `docs/ke-hoach-trien-khai/phase2-implementation-contract.md` (Hợp đồng triển khai Phase 2 và ranh giới cổng an ninh SEC-01).
   - `docs/ke-hoach-trien-khai/phase3-visit-transfer-contract-review.md` (Báo cáo review hợp đồng luồng điều chuyển).
   - `docs/ke-hoach-trien-khai/phase3-visit-transfer-decision-gaps.md` (Ghi nhận các khoảng trống quyết định tác nghiệp điều chuyển).
   - `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-owner-decisions.md` (Đề xuất quyết định của Owner cho vòng đời lượt khám).
   - `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-review.md` (Rà soát hiện trạng vòng đời lượt khám).
3. **Mã nguồn và kịch bản cơ sở dữ liệu đối soát trực tiếp:**
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java`
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/CoSoThucThe.java`
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java`
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/{CoSo,Khoa,PhongKham}.java`
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILuotKhamRepository.java`
   - `clinic-application/src/main/java/vn/clinicmanagement/application/authorization/{PhanQuyenNguCanh,PhanQuyenDichVu}.java`
   - `clinic-application/src/main/java/vn/clinicmanagement/application/audit/{KiemToanHanhDongMa,KiemToanWriteCongKetNoi}.java`
   - `clinic-application/src/main/java/vn/clinicmanagement/application/reception/{TiepDonHangDoiCongKetNoi,RegisterBenhNhanHandler}.java`
   - `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
   - `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java`
   - `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/KhamBenhManHinh.java`
   - `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/BenhNhanHangDoiNguCanhMenu.java`
   - `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferPhongSelectHopThoai.java`

### 7.1. Bằng chứng và Quy trình đối soát RAG (RAG Process & Evidence Record)

- **Quy trình RAG điều phối cấp dự án (Coordinator RAG):** Điều phối viên (Orchestrator) đã thực thi thành công script tìm kiếm RAG của dự án (`/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py`) trước khi tiến hành đọc mã nguồn hoặc duyệt tài liệu chi tiết, qua đó cung cấp các đoạn trích dẫn và hợp đồng điều chuyển đã được phê duyệt làm cơ sở cho tài liệu này. Không có sự tắc nghẽn (blocker) ở cấp độ RAG dự án.
- **Trạng thái môi trường agent cục bộ (Agent-Local Environment):** Tại runtime của môi trường agent, công cụ dòng lệnh RAG cục bộ (`rag` hoặc `ClinicOrchestrator`) không thể triệu gọi trực tiếp do hạn chế môi trường thực thi sandbox độc lập; việc này được công bố minh bạch và được bù đắp hoàn toàn bằng việc điều phối viên đã nạp sẵn kết quả RAG chuẩn xác vào ngữ cảnh task.
- **Phạm vi bảo đảm:** Báo cáo này là tài liệu kỹ thuật thuần túy (Documentation-only), không chỉnh sửa mã nguồn Java, không sửa đổi schema CSDL SQL Server, không nới lỏng chính sách an ninh SEC-01. Toàn bộ các trích dẫn và line anchor trong tài liệu đã được đối chiếu và kiểm tra tính hợp lệ trực tiếp trên mã nguồn repository.
