# VIS-TRANSFER-DECISION-GAP-01 — Phase 3 Visit Transfer Decision-Gap Memo

**Tài liệu:** Bản ghi nhận khoảng trống quyết định nghiệp vụ cho luồng điều chuyển lượt khám (Visit Transfer Decision-Gap Memo)<br>
**Mã task:** `VIS-TRANSFER-DECISION-GAP-01`<br>
**Trạng thái:** Báo cáo phân tích kỹ thuật và đề xuất lựa chọn tác nghiệp cho Owner duyệt. Không thay đổi source code, schema migration, ma trận phân quyền, hoặc kích hoạt runtime workflow.<br>
**Gate an ninh:** Mọi workflow điều chuyển tiếp tục bị từ chối (`DENY`) trong runtime theo cổng an ninh SEC-01 ([`phase2-implementation-contract.md:32-50`](phase2-implementation-contract.md#L32-L50)).

---

## 1. Tóm tắt điều hành & Mục tiêu tài liệu

Tài liệu này đối soát hiện trạng mã nguồn, tài liệu Phase 3 và các chính sách đã được Chủ dự án (Owner) phê duyệt nhằm xác định chính xác các **khoảng trống quyết định tác nghiệp (operational decision gaps)** cần Owner chốt trước khi thiết kế contract triển khai cho tính năng điều chuyển lượt khám (`LuotKham`).

Tài liệu tuân thủ nghiêm ngặt 3 nguyên tắc:
1. **Không mở lại các chính sách đã duyệt (settled policies):** Giữ nguyên vai trò, phạm vi (role/scope), ranh giới hủy lượt và ranh giới ghi sinh hiệu làm biên cố định.
2. **Không tự quyết định thay Owner:** Chỉ trình bày các lựa chọn khả thi kèm tác động kỹ thuật/kiến trúc cụ thể.
3. **Phân định minh bạch:** Tách bạch rõ ràng giữa **Thực tế mã nguồn (Fact)**, **Chính sách đã duyệt (Policy)**, và **Suy luận kỹ thuật (Inference)**.

---

## 2. Ranh giới chính sách đã phê duyệt — Cố định, không mở lại

Các ranh giới sau đây đã được Owner phê duyệt chính thức tại [`phase2-role-permission-matrix-approval.md:1-27`](phase2-role-permission-matrix-approval.md#L1-L27) và tái khẳng định tại [`phase3-visit-lifecycle-owner-decisions.md:7-15`](phase3-visit-lifecycle-owner-decisions.md#L7-L15). Các mục này đóng vai trò là tiền đề bất biến (invariants) cho luồng điều chuyển:

* **Chính sách điều chuyển lượt khám (`DEC-AUTHZ-04b`):**
  - `RECEPTIONIST` (Tiếp đón): Được phép điều chuyển lượt khám giữa các khoa trong **cùng một cơ sở** (`source.facilityId == destination.facilityId == session.facilityId`). Nghiêm cấm điều chuyển xuyên cơ sở.
  - `DOCTOR` (Bác sĩ): Chỉ được phép điều chuyển lượt khám **trong nội bộ khoa** mà phiên làm việc đã chọn (`source.facilityId == destination.facilityId == session.facilityId` và `source.departmentId == destination.departmentId == session.departmentId`, ví dụ: chuyển giữa các phòng khám trong cùng khoa).
  - Các vai trò khác (`ADMIN`, `PHARMACIST`, `CASHIER`, `LAB_TECH`): Hoàn toàn bị cấm (`DENY`) thực hiện điều chuyển.
  - **Ranh giới cô lập dữ liệu (`DEC-AUTHZ-03`):** Thao tác điều chuyển là một tác vụ cập nhật vị trí/trạng thái có guard chuyên biệt; thao tác này **tuyệt đối không cấp quyền đọc** hồ sơ bệnh nhân, lịch sử khám hay hàng đợi của khoa/phòng đích cho người thực hiện.
* **Ranh giới hủy lượt khám (`DEC-AUTHZ-04a`):**
  - Chỉ duy nhất `RECEPTIONIST` được hủy lượt khám.
  - Lượt khám chỉ hợp lệ để hủy khi đang ở trạng thái `CHO_KHAM` và **chưa có bất kỳ chỉ định cận lâm sàng/dịch vụ nào cũng như chưa phát sinh bất kỳ khoản thu phí/hóa đơn nào**. Bắt buộc xác nhận, nhập lý do và ghi audit. Ranh giới này độc lập và không bị nới lỏng bởi luồng điều chuyển.
* **Ranh giới ghi nhận sinh hiệu (`DEC-AUTHZ-01`):**
  - Chỉ duy nhất `DOCTOR` được ghi hoặc sửa sinh hiệu (`SinhHieu`) trong phạm vi khoa phòng phiên làm việc (`KHOA_PHONG`); `RECEPTIONIST` và các vai trò khác bị từ chối tuyệt đối. Ranh giới này không bị ảnh hưởng hay gộp chung vào màn hình điều chuyển.
* **Quy tắc phiên làm việc đơn (`Session Scope Boundary`):**
  - Mỗi phiên làm việc của nhân viên chỉ gắn với duy nhất 1 vai trò và 1 cặp `(facilityId, departmentId)`. Mọi kiểm tra quyền thông thường đều dựa trên cặp này tại Application boundary.

---

## 3. Đối chiếu hiện trạng: Fact, Policy và Inference

### 3.1. Thực tế mã nguồn hiện tại (Fact)

| Thành phần | Vị trí mã nguồn | Thực tế ghi nhận |
|---|---|---|
| **Prototype điều chuyển trên UI** | [`PatientQueueContextMenu.java:71-75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75)<br>[`ExaminationPanel.java:1080-1091`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091) | Menu chuột phải "Chuyển Bệnh Nhân Sang Phòng Khác" mở [`TransferRoomSelectDialog.java:15-34`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34) chứa danh sách tên phòng hardcode dạng chuỗi (`"Phòng 101 - Khám Nội 1"`,...). Nút "Lưu" chỉ đóng dialog và mở `TransferSlipPrintPreviewDialog` để in phiếu; **hoàn toàn không gọi Application service, không gửi ID lượt khám, không kiểm tra quyền và không cập nhật CSDL**. |
| **Giao diện tiếp đón** | [`ReceptionPanel.java:77-115`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L77-L115) | Màn hình tiếp đón chỉ tải danh sách phòng khám và hàng đợi trong ngày theo scope phiên; **chưa có bất kỳ nút, menu hay hành động điều chuyển nào**. |
| **Mô hình Domain `LuotKham`** | [`LuotKham.java:29-55,88-124`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L29-L55) | Thực thể lưu `maCoSo`, `maKhoa`, `maPhongKham` (nullable), `trangThai`, `soThuTu`, `soThuTuDisplay`, `dnVnTien`, và cờ `emrLocked`. Có các phương thức `startExam()`, `completeExam()`, `lockEmr()`, nhưng **không có phương thức `transfer()` hay Domain Event nào liên quan đến điều chuyển**. Trạng thái enum [`TrangThaiLuotKham.java:3-10`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L3-L10) liệt kê giá trị đơn thuần, không có state machine. |
| **Cơ sở dữ liệu & Schema** | [`V1__Initial_Clinic_Schema.sql:329-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380) | Bảng `LuotKham` có các cột `maCoSo`, `maKhoa`, `maPhongKham`, `version`, `trangThai`. **Không có bảng lịch sử điều chuyển, không lưu vết khoa/phòng nguồn, người thực hiện, thời điểm chuyển, hoặc lý do chuyển**. |
| **Cổng phân quyền (Authorization)** | [`AuthorizationService.java:51-54`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationService.java#L51-L54) | Phương thức `evaluate()` từ chối mọi yêu cầu có `targetFacilityId != context.facilityId() || targetDepartmentId != context.departmentId()` với mã `TARGET_MISMATCH`. Cơ chế đánh giá chung này **không thể xử lý yêu cầu chuyển liên khoa cùng cơ sở của `RECEPTIONIST`**, đòi hỏi một guard riêng cho use case điều chuyển. |
| **Cổng tiếp đón & Truy vấn hàng đợi** | [`ReceptionQueuePort.java:9-15`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/ReceptionQueuePort.java#L9-L15)<br>[`ReceptionQueueRepositoryImpl.java:125-155`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L125-L155) | Cổng hàng đợi chỉ hỗ trợ đăng ký mới (`registerPatient`) và đọc hàng đợi (`getReceptionQueue`, `getRoomSummaries`). Thứ tự hàng đợi được sắp xếp cố định theo `ORDER BY l.dnVnTien DESC, l.soThuTu, l.id` cho đúng cặp `(maCoSo, maKhoa)` của ngày khám; **chưa có hàm cập nhật vị trí/hàng đợi khi chuyển**. |
| **Hệ thống Audit** | [`AuditActionCode.java:23-34`](../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L23-L34) | Đã có mã hành động `VISIT_REGISTERED`, nhưng **chưa có mã hành động `VISIT_TRANSFERRED`**. |

### 3.2. Chính sách đã phê duyệt (Policy)

* Quyết định `DEC-AUTHZ-04b`: Cho phép `RECEPTIONIST` chuyển liên khoa cùng cơ sở; `DOCTOR` chỉ chuyển trong khoa; cấm chuyển xuyên cơ sở; không mở quyền đọc dữ liệu khoa đích ([`phase2-role-permission-matrix-approval.md:8-18`](phase2-role-permission-matrix-approval.md#L8-L18)).
* Quyết định `DEC-TIME-01`: Chuẩn hóa thời gian kỹ thuật/audit bằng UTC, thời gian nghiệp vụ phòng khám bằng múi giờ `Asia/Ho_Chi_Minh` ([`phase2-implementation-contract.md:8-12`](phase2-implementation-contract.md#L8-L12)).
* Cổng an ninh `SEC-01`: Giữ trạng thái từ chối mặc định (`DENY`) ở tầng runtime cho tới khi implementation contract, guard, audit và bộ kiểm thử đạt chuẩn ([`phase2-implementation-contract.md:32-50`](phase2-implementation-contract.md#L32-L50)).

### 3.3. Suy luận kỹ thuật (Inference)

* Để thực hiện điều chuyển mà không phá vỡ tính đóng băng của `AuthorizationService.evaluate()` (vốn bảo vệ toàn bộ các thao tác thông thường), cần một **Transfer Authorization Guard chuyên biệt** trong tầng Application để kiểm tra đồng thời cặp nguồn và cặp đích theo ma trận `DEC-AUTHZ-04b`.
* Thao tác điều chuyển trả về một biên nhận kết quả (Receipt DTO) tối giản (chỉ chứa `visitId`, `sourceDepartmentId`, `targetDepartmentId`, `transferredAt`); **tuyệt đối không trả về thông tin danh sách bệnh nhân hoặc chi tiết hàng đợi của khoa đích** nhằm tuân thủ triệt để nguyên tắc không cấp quyền đọc liên khoa.
* Luồng điều chuyển phải chạy trong một `TransactionBoundary` duy nhất: cập nhật thực thể `LuotKham`, ghi nhận lịch sử điều chuyển, và phát sinh bản ghi audit `VISIT_TRANSFERRED`. Nếu audit thất bại, toàn bộ giao tác phải bị rollback (fail-closed).

---

## 4. Danh mục các khoảng trống quyết định tác nghiệp cần Owner phê duyệt

Dưới đây là 7 vấn đề nghiệp vụ chưa được giải quyết trong các tài liệu hiện hành. Mỗi vấn đề đi kèm các phương án và phân tích tác động triển khai cụ thể để Owner cân nhắc lựa chọn:

### Khoảng trống 1: Trạng thái lượt khám đủ điều kiện điều chuyển (Transfer-Eligible Statuses)
* **Hiện trạng nguồn:** `TrangThaiLuotKham` có các giá trị: `CHO_KHAM`, `DANG_KHAM`, `DA_KHAM`, `KET_LUAN`, `HOAN_THANH`, `DA_HUY`, `CHO_CLS` ([`TrangThaiLuotKham.java:3-10`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L3-L10)). `LuotKham.java:43` có cờ `emrLocked`. Chính sách `DEC-AUTHZ-04b` chỉ nêu quyền của actor, chưa quy định lượt khám ở trạng thái nào thì được phép chuyển.
* **Các phương án lựa chọn:**
  * **Phương án 1A (Khắt khe - Chỉ hàng đợi chờ khám):** Chỉ cho phép điều chuyển khi lượt khám ở trạng thái `CHO_KHAM`. Nếu bác sĩ đã nhấn bắt đầu khám (`DANG_KHAM`) hoặc lượt đã kết thúc/hủy/khóa EMR thì từ chối.
  * **Phương án 1B (Linh hoạt lâm sàng theo vai trò):**
    - `RECEPTIONIST`: Chỉ được chuyển lượt ở trạng thái `CHO_KHAM`.
    - `DOCTOR`: Được chuyển lượt ở trạng thái `CHO_KHAM`, `DANG_KHAM`, hoặc `CHO_CLS` (phục vụ hội chẩn hoặc chuyển phòng chuyên môn trong khoa). Tuyệt đối cấm chuyển lượt ở trạng thái `HOAN_THANH`, `DA_HUY`, hoặc khi `emrLocked == true`.
  * **Phương án 1C (Toàn diện kèm trạng thái chuyển tiếp):** Cho phép cả `CHO_KHAM` và `DANG_KHAM`, nhưng khi chuyển, hệ thống tự động reset trạng thái lượt khám về `CHO_KHAM` tại đích và xóa thời gian `thoiGianBatDauKham`.
* **Tác động triển khai:**
  - Quy định logic kiểm tra invariant trong `LuotKham.transferTo(...)` hoặc `TransferVisitHandler`.
  - Định nghĩa các mã lỗi nghiệp vụ trả về giao diện: `VISIT_STATUS_INELIGIBLE`, `EMR_LOCKED_CANNOT_TRANSFER`.

---

### Khoảng trống 2: Yêu cầu về phòng khám đích (Destination Room Requirement & Hierarchy)
* **Hiện trạng nguồn:** Trong CSDL và Domain, trường `maPhongKham` là **nullable** ([`V1__Initial_Clinic_Schema.sql:336`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L336), [`LuotKham.java:42`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L42)). Phòng khám gắn trực tiếp với khoa thông qua `PhongKham.maKhoa` ([`PhongKham.java:4-21`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/PhongKham.java#L4-L21)).
* **Các phương án lựa chọn:**
  * **Phương án 2A (Bắt buộc chỉ định phòng đích):** Người thực hiện chuyển (cả Tiếp đón và Bác sĩ) bắt buộc phải chọn cụ thể một phòng khám đích (`destinationRoomId` không được null). Phòng này phải đang hoạt động (`hoatDong == true`) và thuộc đúng khoa đích.
  * **Phương án 2B (Tùy chọn phòng theo phân cấp):**
    - Chuyển liên khoa (`RECEPTIONIST`): Chỉ bắt buộc chọn khoa đích (`destinationDepartmentId`); phòng đích (`destinationRoomId`) có thể để trống (null) để bộ phận điều phối tại khoa đích tự phân phòng sau.
    - Chuyển nội bộ khoa (`DOCTOR`): Bắt buộc phải chọn phòng khám đích cụ thể khác với phòng hiện tại.
* **Tác động triển khai:**
  - Ảnh hưởng trực tiếp đến validation của `TransferVisitCommand` (cho phép hay không cho phép `destinationRoomId == null`).
  - Định nghĩa câu truy vấn xác thực phòng khám đích: `SELECT COUNT(*) FROM PhongKham WHERE id = ? AND maKhoa = ? AND hoatDong = 1`.
  - Giao diện người dùng: Dropdown chọn phòng là bắt buộc hay tùy chọn.

---

### Khoảng trống 3: Quy tắc cấp số thứ tự, độ ưu tiên và vị trí hàng đợi tại đích (Queue Numbering & Priority)
* **Hiện trạng nguồn:** Hàng đợi được sắp xếp theo `dnVnTien DESC, soThuTu, id` ([`ReceptionQueueRepositoryImpl.java:154`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L154)). Hiện tại số thứ tự `soThuTu` được cấp tăng dần trong ngày khi tiếp đón đăng ký mới.
* **Các phương án lựa chọn:**
  * **Phương án 3A (Cấp số mới tại đích):** Cấp số thứ tự (`soThuTu`) mới nhất theo bộ đếm hàng đợi của khoa/phòng đích trong ngày. Số cũ tại khoa nguồn được lưu trữ vào lịch sử điều chuyển.
  * **Phương án 3B (Bảo lưu số cũ):** Giữ nguyên giá trị `soThuTu` ban đầu của bệnh nhân, chỉ thay đổi `maKhoa`/`maPhongKham`. Bệnh nhân sẽ đứng ở vị trí tương ứng với số cũ trong danh sách của phòng đích.
  * **Phương án 3C (Ưu tiên tiếp nhận điều chuyển):** Cấp số mới tại đích nhưng tự động bật cờ ưu tiên (`dnVnTien = true` hoặc cờ `isPriorityTransfer = true`) để bệnh nhân được đưa lên đầu danh sách chờ của khoa/phòng mới, tránh việc bệnh nhân phải chờ lại từ đầu.
* **Tác động triển khai:**
  - Nếu chọn 3A hoặc 3C: Cần phương thức cấp số nguyên tử `getNextQueueNumber(date, facilityId, departmentId, roomId)` trong repository và cập nhật lại cả `soThuTuDisplay`.
  - Nếu chọn 3B: Có nguy cơ trùng số thứ tự (`soThuTu`) hiển thị giữa hai bệnh nhân khác nhau tại cùng một phòng đích.

---

### Khoảng trống 4: Ràng buộc với chỉ định dịch vụ, đơn thuốc và thanh toán đang phát sinh (Orders & Billing Barriers)
* **Hiện trạng nguồn:** Thực thể `LuotKham` có quan hệ với `ChiDinhDichVu`, `DonThuoc`, và `HoaDon` ([`LuotKham.java:82-84`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L82-L84)). Chính sách hủy lượt (`DEC-AUTHZ-04a`) cấm hủy khi đã có chỉ định/thanh toán, nhưng chính sách điều chuyển (`DEC-AUTHZ-04b`) chưa quy định xử lý các khoản mục này ra sao khi chuyển khoa.
* **Các phương án lựa chọn:**
  * **Phương án 4A (Chặn tuyệt đối nếu có dịch vụ/chi phí):** Từ chối điều chuyển nếu lượt khám đã có bất kỳ chỉ định dịch vụ (`ChiDinhDichVu`), đơn thuốc (`DonThuoc`) hoặc hóa đơn (`HoaDon`) nào chưa hoàn tất hoặc đã phát sinh chi phí. Bắt buộc khoa nguồn phải hoàn tất hoặc hủy toàn bộ dịch vụ trước khi chuyển.
  * **Phương án 4B (Cho phép chuyển, dữ liệu gắn theo lượt khám):** Cho phép điều chuyển bình thường. Toàn bộ chỉ định và đơn thuốc đã kê ở khoa nguồn được giữ nguyên trong hồ sơ bệnh án của lượt khám đó, bác sĩ khoa đích tiếp tục theo dõi điều trị.
  * **Phương án 4C (Phân tách theo loại điều chuyển):**
    - Chuyển nội bộ khoa (`DOCTOR`): Cho phép chuyển phòng, toàn bộ chỉ định giữ nguyên.
    - Chuyển liên khoa (`RECEPTIONIST`): Chỉ cho phép khi chưa có chỉ định hoặc toàn bộ chỉ định đã có kết quả trả về.
* **Tác động triển khai:**
  - Yêu cầu xây dựng query kiểm tra tính toàn vẹn phụ thuộc trong cùng giao tác điều chuyển: `hasActiveOrders(visitId)`, `hasPendingBills(visitId)`.
  - Tác động đến báo cáo doanh thu và phân bổ thù lao bác sĩ/khoa phòng trong tương lai.

---

### Khoảng trống 5: Mốc thời gian hiệu lực nghiệp vụ và giới hạn ngày khám (Effective Time & Date Boundaries)
* **Hiện trạng nguồn:** `LuotKham.ngayKham` là kiểu `LocalDate` ([`LuotKham.java:38`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L38)). Chính sách thời gian `DEC-TIME-01` quy định giờ phòng khám dùng múi giờ `Asia/Ho_Chi_Minh`, thời gian hệ thống dùng UTC.
* **Các phương án lựa chọn:**
  * **Phương án 5A (Chỉ cho phép điều chuyển trong ngày):** Chỉ cho phép điều chuyển các lượt khám có `ngayKham` trùng với ngày hiện tại của hệ thống (theo giờ `Asia/Ho_Chi_Minh`). Lượt khám của các ngày trước đó không thể điều chuyển.
  * **Phương án 5B (Cho phép chuyển qua ngày kèm cập nhật ngày khám):** Nếu điều chuyển qua ngày (ví dụ ca trực đêm), cho phép cập nhật `ngayKham` sang ngày hiện tại, lưu vết `originalNgayKham` vào lịch sử.
* **Tác động triển khai:**
  - Logic xác thực trong Application: `if (!visit.getNgayKham().equals(currentDateInVietnam())) throw new TransferEligibilityException(...)`.
  - Đảm bảo tính nhất quán của hàng đợi tiếp đón (vốn truy vấn theo `WHERE ngayKham = ?`).

---

### Khoảng trống 6: Lưu trữ lịch sử điều chuyển, phân loại lý do và Payload Audit
* **Hiện trạng nguồn:** `AuditActionCode` chưa có mã chuyển lượt. Bảng `LuotKham` chỉ có các trường vị trí hiện tại, không có bảng con lưu lịch sử chuyển vị trí ([`V1__Initial_Clinic_Schema.sql:329-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380)).
* **Các phương án lựa chọn:**
  * **Phương án 6A (Chỉ dùng Audit Trail):**
    - Bổ sung `VISIT_TRANSFERRED` vào `AuditActionCode`.
    - Toàn bộ thông tin nguồn/đích (`sourceFacilityId`, `sourceDepartmentId`, `sourceRoomId`, `targetDepartmentId`, `targetRoomId`, `reasonCode`, `oldQueueNumber`, `newQueueNumber`) được lưu dưới dạng JSON trong metadata của bảng `AuditLog`. Không thay đổi schema bảng nghiệp vụ.
  * **Phương án 6B (Bảng lịch sử quan hệ chuyên biệt + Audit):**
    - Tạo bảng mới `LichSuDieuChuyenLuotKham` (Migration Flyway mới) để phục vụ truy vấn lâm sàng và in ấn phiếu chuyển viện/chuyển khoa.
    - Đồng thời vẫn ghi bản ghi `VISIT_TRANSFERRED` vào `AuditLog` để phục vụ giám sát an ninh.
  * **Quy chuẩn về lý do điều chuyển (Reason Semantics):**
    - *Lựa chọn R1 (Mã cố định + Ghi chú tùy chọn):* Chọn lý do từ danh mục mã chuẩn hóa (ví dụ: `CHUYEN_KHOA_CHUYEN_MON`, `YEU_CAU_BENH_NHAN`, `PHONG_KHAM_QUA_TAI`, `SAI_SOT_TIEP_DON`) kèm ghi chú bổ sung không chứa PII.
    - *Lựa chọn R2 (Văn bản tự do bắt buộc):* Bắt buộc nhập text tối thiểu 10 ký tự, tối đa 255 ký tự.
* **Tác động triển khai:**
  - Nếu chọn 6A: Không cần migration CSDL, triển khai nhanh và gọn nhẹ.
  - Nếu chọn 6B: Cần tạo migration mới, định nghĩa Entity `LichSuDieuChuyen` và repository tương ứng.
  - Mã lý do chuẩn hóa (R1) giúp ngăn chặn rò rỉ thông tin y tế nhạy cảm (PHI) vào log kiểm toán.

---

### Khoảng trống 7: Kiểm soát đồng thời (Concurrency), Khóa lạc quan và Tính lũy nghiệm (Idempotency)
* **Hiện trạng nguồn:** Bảng `LuotKham` có cột `version BIGINT DEFAULT 0` phục vụ khóa lạc quan ([`V1__Initial_Clinic_Schema.sql:367`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L367), [`BaseEntity.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java)).
* **Các phương án lựa chọn:**
  * **Phương án 7A (Khóa lạc quan CAS + Idempotency Key):**
    - Lệnh điều chuyển `TransferVisitCommand` gửi kèm `expectedVersion` và một `idempotencyKey` sinh từ UI.
    - Cập nhật bằng câu lệnh Compare-And-Swap (CAS): `UPDATE LuotKham SET ..., version = version + 1 WHERE id = ? AND version = ?`.
    - Nếu trùng `idempotencyKey` cùng payload: Trả về kết quả thành công của giao tác trước (lũy nghiệm), không thực hiện lại side effect.
    - Nếu version không khớp: Trả về lỗi xung đột `CONCURRENCY_CONFLICT`, yêu cầu client reload dữ liệu.
  * **Phương án 7B (Khóa bi quan hàng - Row-level Locking):**
    - Sử dụng `SELECT ... WITH (UPDLOCK, ROWLOCK)` trong transaction của SQL Server để tuần tự hóa các thao tác điều chuyển trên cùng một lượt khám.
* **Tác động triển khai:**
  - Ngăn ngừa tình trạng Lost Update (hai người dùng cùng chuyển một bệnh nhân đến hai nơi khác nhau cùng lúc).
  - Đảm bảo an toàn tuyệt đối cho số thứ tự hàng đợi và tính toàn vẹn của giao tác.

---

## 5. Tổng hợp bảng đối chiếu khoảng trống và tác động kỹ thuật

| Khoảng trống quyết định | Tùy chọn A | Tùy chọn B | Tùy chọn C | Tác động kỹ thuật chính |
|---|---|---|---|---|
| **1. Trạng thái đủ điều kiện** | Chỉ `CHO_KHAM` | `CHO_KHAM` (Tiếp đón) + `DANG_KHAM`/`CHO_CLS` (Bác sĩ) | Cho phép cả hai, tự động reset về `CHO_KHAM` | Domain guard invariant, mapping lỗi trạng thái |
| **2. Phòng khám đích** | Bắt buộc mọi trường hợp | Bắt buộc với Bác sĩ; Tùy chọn với Tiếp đón | — | Command DTO nullable, query kiểm tra phòng active |
| **3. Số thứ tự & Hàng đợi** | Cấp số mới tại đích | Giữ nguyên số cũ | Cấp số mới kèm cờ ưu tiên (`dnVnTien = true`) | Cơ chế sinh sequence hàng đợi trong CSDL, sắp xếp queue |
| **4. Dịch vụ & Thu phí** | Chặn nếu có dịch vụ/thu phí | Cho phép, giữ nguyên dịch vụ | Khác nhau giữa Bác sĩ và Tiếp đón | Query phụ thuộc liên module, phân bổ chi phí |
| **5. Giới hạn ngày khám** | Chỉ chuyển trong ngày | Cho phép chuyển qua ngày | — | So khớp múi giờ `Asia/Ho_Chi_Minh`, invariant ngày |
| **6. Lưu vết & Lý do** | Chỉ ghi Audit Log JSON | Tạo bảng lịch sử riêng + Audit | — (Lý do: Mã enum R1 vs Text R2) | Quyết định có hay không có Flyway migration mới |
| **7. Đồng thời & Idempotency** | Khóa lạc quan CAS + Idempotency | Khóa bi quan `UPDLOCK` | — | Câu lệnh UPDATE SQL, xử lý retry trên Desktop UI |

---

## 6. Tiêu chí kiểm định nghiệm thu khi triển khai (Acceptance Gates)

Khi các quyết định trên được Owner thông qua và cổng SEC-01 được giải phóng, nhiệm vụ triển khai code thực tế bắt buộc phải thỏa mãn các tiêu chí kiểm định sau:

1. **Phân quyền và bảo mật (Authorization):**
   - Bộ kiểm thử độc lập chứng minh: `RECEPTIONIST` chuyển khác cơ sở -> `DENIED`; `DOCTOR` chuyển khác khoa -> `DENIED`; vai trò khác -> `DENIED`.
   - Chứng minh bằng unit test và test giả lập (mocks/fakes): Quá trình điều chuyển **không gọi bất kỳ hàm đọc bệnh nhân (`getById`, `search`) hay hàm đọc hàng đợi của khoa đích**. Biên nhận trả về chỉ chứa thông tin tối giản của giao tác.
2. **Giao tác nguyên tử & Fail-closed:**
   - Cập nhật vị trí `LuotKham` và ghi nhận `AuditLog` phải nằm chung trong một `TransactionBoundary`.
   - Kiểm thử mô phỏng lỗi khi ghi audit: Toàn bộ cập nhật vị trí và số thứ tự phải được rollback hoàn toàn về trạng thái ban đầu.
3. **Đồng thời & Lũy nghiệm (Concurrency & Idempotency):**
   - Hai request điều chuyển đồng thời trên cùng một version lượt khám: Đúng 1 request thành công, request còn lại nhận lỗi xung đột rõ ràng, không làm hỏng dữ liệu hàng đợi.
4. **Không rò rỉ dữ liệu nhạy cảm:**
   - Payload audit chỉ chứa mã định danh, mã lý do, thời gian và số phiên; không chứa thông tin lâm sàng tự do hoặc PII thừa.

---

## 7. Tài liệu hướng dẫn và căn cứ đã đọc (Guidance Read)

Các tài liệu hướng dẫn bắt buộc đã được đọc kỹ trước khi lập báo cáo:
- `docs/PROJECT_CONVENTIONS.md` (Quy ước phát triển dự án và làm việc của Agent).
- `README.md` (Tổng quan kiến trúc dự án và thiết lập môi trường).
- `DOMAIN_ANALYSIS.md` (Báo cáo phân tích toàn diện package `vn.clinicmanagement.domain`).
- `ERD_DIAGRAM.md` (Sơ đồ thực thể quan hệ CSDL EHC HIS).
- `UI_CODING_STANDARDS.md` (Quy chuẩn thiết kế giao diện và màn hình Desktop).
- `docs/ke-hoach-trien-khai/phase2-role-permission-matrix-approval.md` (Phê duyệt ma trận phân quyền Phase 2).
- `docs/ke-hoach-trien-khai/phase2-implementation-contract.md` (Hợp đồng triển khai Phase 2 & Cổng SEC-01).
- `docs/ke-hoach-trien-khai/phase3-visit-transfer-contract-review.md` (Báo cáo review hợp đồng luồng điều chuyển).
- `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-owner-decisions.md` (Đề xuất quyết định Owner vòng đời lượt khám).
- `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-review.md` (Review vòng đời lượt khám).
