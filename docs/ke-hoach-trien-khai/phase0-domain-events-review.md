# Rà Soát Phase 0: Vòng Đời Sự Kiện Miền (`PLAT-EVENT-01`)

> **Trạng thái:** Chỉ phân tích hiện trạng dựa trên bằng chứng mã nguồn (`Phase 0 analysis only`).  
> **Phạm vi thay đổi:** Chỉ tạo tài liệu báo cáo này (`docs/ke-hoach-trien-khai/phase0-domain-events-review.md`). Không chỉnh sửa mã Java, schema SQL, script migration hay tài liệu hiện hữu.  
> **Quy ước bằng chứng:** Mọi nhận định hiện trạng đều gắn đường dẫn file và số dòng cụ thể. Mọi đề xuất thiết kế và hợp đồng triển khai tiếp theo được gắn nhãn rõ ràng là **`[Khuyến nghị / Recommendation]`**. Không tự đặt chính sách nghiệp vụ/bảo mật cho Phase 2; mọi ví dụ minh họa chỉ dùng dữ liệu tổng hợp (`synthetic data`), tuyệt đối không dùng dữ liệu bệnh nhân thật.

---

## 1. Mục tiêu, Phạm vi & Danh sách Tài liệu Hướng dẫn Đã Đọc

### 1.1. Mục tiêu rà soát
Truy vết toàn bộ các hợp đồng và lớp sự kiện miền (`Domain Event`) đã khai báo trong hệ thống qua suốt vòng đời:
1. Khởi tạo/cập nhật Aggregate và đăng ký sự kiện trong `clinic-domain`.
2. Hợp đồng và hiện thực `IDomainEventPublisher` / `IDomainEventHandler`.
3. Ranh giới giao dịch (`TransactionBoundary`, `JdbcTransactionManager`, `IUnitOfWork`, `JdbcUnitOfWork`), thời điểm phát hành sự kiện, hành vi khi lỗi/rollback/thử lại (`retry`).
4. Lưu trữ bền vững (`persistence`) và hành vi cập nhật giao diện (`clinic-desktop` Swing UI).
5. Độ phủ kiểm thử hiện tại (`src/test/java`) và khế ước triển khai tối thiểu cho bước tiếp theo của `PLAT-EVENT-01`.

### 1.2. Danh sách tài liệu hướng dẫn đã đọc trước khi kiểm tra mã nguồn
- Quy ước nền của Orchestrator: `docs/PROJECT_CONVENTIONS.md` (nạp trong gói task)
- `README.md` (`L1-L162`)
- `DOMAIN_ANALYSIS.md` (`L1-L265`, đặc biệt Mục `1.1`, `1.2`, `4.2 Domain Events Specification` tại `L119-L135`, và `10. Architectural Patterns Summary` tại `L222-L246`)
- `ERD_DIAGRAM.md` (`L1-L654`)
- `UI_CODING_STANDARDS.md` (`L1-L343`)
- `PROJECT_COMPLETENESS_REPORT.md` (`L1-L238`)
- `docs/AI_AGENT_PILOT_TASKS.md` (`L1-L186`)
- `docs/ke-hoach-trien-khai/README.md` (`L1-L115`)
- `docs/ke-hoach-trien-khai/00-hien-trang.md` (`L1-L143`)
- `docs/ke-hoach-trien-khai/01-ma-tran-model-man-hinh.md` (`L1-L157`)
- `docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md` (`L1-L183`, đặc biệt `Đợt 2A.2` tại `L64-L66`)
- `docs/ke-hoach-trien-khai/03-model-task-board.md` (`L1-L249`, đặc biệt `PLAT-EVENT-01` tại `L59-L68` và `SEC-AUDIT-01` tại `L70-L79`)
- `docs/ke-hoach-trien-khai/phase2-auth-persistence-review.md` (`L1-L173`)
- `docs/ke-hoach-trien-khai/phase2-desktop-session-review.md` (`L1-L227`)
- `docs/ke-hoach-trien-khai/phase2-domain-audit-review.md` (`L1-L229`)
- `docs/ke-hoach-trien-khai/phase2-security-policy-proposal.md` (`L1-L220`)
- Các tài liệu phân hệ trong `docs/ke-hoach-trien-khai/phan-he/`:
  - `03-tiep-don-benh-nhan-nhan-su.md`
  - `04-lich-hen-kham-emr.md`
  - `05-dich-vu-can-lam-sang.md`
  - `06-kho-nha-thuoc.md`
  - `07-vien-phi-payroll.md`
  - `08-co-so-bao-mat-cau-hinh.md`
  - `09-danh-muc-bao-cao-tai-lieu.md`

> **Lưu ý về file hướng dẫn không tồn tại trong repo:** `AGENTS.md`, `CONTRIBUTING.md`, `ARCHITECTURE.md`, `docs/DEVELOPMENT_CONVENTIONS.md`, `docs/README.md` hiện chưa có trên nhánh làm việc; đã đối chiếu đầy đủ các tài liệu hiện có nêu trên.

---

## 2. Hiện trạng Khung Sự Kiện Miền (`Domain Event Framework`)

| Thành phần | Đường dẫn & Dòng | Hiện trạng trong mã nguồn | Khoảng trống có bằng chứng |
| :--- | :--- | :--- | :--- |
| `BaseEntity<ID>` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java:15-102` | Lưu danh sách sự kiện nội bộ trong `private transient List<IDomainEvent> domainEvents = new ArrayList<>();` (`L28`). Cung cấp `registerDomainEvent(IDomainEvent event)` (`L90-L93`), `getDomainEvents()` trả về `Collections.unmodifiableList(...)` (`L95-L98`) và `clearDomainEvents()` (`L100-L102`). | `registerDomainEvent` (`L90-L93`) không kiểm tra `event == null` (`Objects.requireNonNull`). Không có thành phần nào trong `clinic-application` hoặc `clinic-infrastructure` gọi `getDomainEvents()` hay `clearDomainEvents()`. |
| `IAggregateRoot` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/IAggregateRoot.java:1-3` | Marker interface rỗng (`public interface IAggregateRoot {}` tại `L3`). | Không khai báo phương thức truy xuất/xóa sự kiện miền nên `IUnitOfWork` hoặc repository không thể thu thập sự kiện qua kiểu `IAggregateRoot` nếu không ép kiểu sang `BaseEntity<?>`. |
| `IUnitOfWork` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/IUnitOfWork.java:1-6` | Chỉ khai báo `int saveChanges()` (`L4`) và `boolean saveEntities()` (`L5`). | Không có phương thức đăng ký Aggregate đã thay đổi (`registerEntity`/`trackEntity`) để thu thập và phát hành `domainEvents` khi kết thúc giao dịch. |
| `IDomainEvent` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/IDomainEvent.java:1-9` | Khai báo `UUID getEventId()` (`L7`) và `LocalDateTime getOccurredOn()` (`L8`). | Chỉ có metadata cơ bản (`eventId`, `occurredOn`); không có hợp đồng chuẩn hóa loại sự kiện hoặc định danh Aggregate nếu cần truy vết tổng quát. |
| `AbstractDomainEvent` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/AbstractDomainEvent.java:1-14` | Khởi tạo `this.eventId = UUID.randomUUID()` (`L7`) và `this.occurredOn = LocalDateTime.now()` (`L8`). | Sử dụng trực tiếp đồng hồ hệ thống `LocalDateTime.now()` (`L8`), không hỗ trợ truyền `Clock` hoặc mốc thời gian tất định khi kiểm thử. |
| `IDomainEventPublisher` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/IDomainEventPublisher.java:1-5` | Khai báo `void publish(IDomainEvent event);` (`L4`). | **Không có bất kỳ class nào implement `IDomainEventPublisher`** trong toàn bộ `clinic-domain`, `clinic-application`, `clinic-infrastructure`, và `clinic-desktop`. |
| `IDomainEventHandler<T>` | `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/IDomainEventHandler.java:1-5` | Khai báo `void handle(T event);` (`L4`) với `T extends IDomainEvent`. | **Không có bất kỳ class nào implement `IDomainEventHandler`** trong toàn bộ repository. |

---

## 3. Truy Vết Toàn Bộ 10 Loại `Domain Event` Đã Khai Báo

Hiện tại gói `vn.clinicmanagement.domain.common.events` khai báo **10 lớp sự kiện cụ thể** kế thừa `AbstractDomainEvent`. Bảng dưới đây đối chiếu từng sự kiện từ tài liệu thiết kế (`DOMAIN_ANALYSIS.md:119-135`) tới nơi khởi tạo trong Aggregate, Application Handler, Infrastructure Repository và Desktop UI:

| # | Lớp sự kiện (`clinic-domain/.../common/events/`) | Payload hiện tại | Nơi Aggregate gọi `registerDomainEvent(...)` | Nơi kích hoạt ở Application / Infrastructure / UI | Phát hiện & Lỗi/Khoảng trống cụ thể |
| :- | :--- | :--- | :--- | :--- | :--- |
| 1 | `BenhNhanRegisteredEvent` (`BenhNhanRegisteredEvent.java:3-18`) | `patientId` (`int`), `cccd` (`String`), `fullName` (`String`) (`L4-L6`) | Constructor 9 tham số của `BenhNhan` (`clinic-domain/.../patient/BenhNhan.java:53-64`, dòng `L63`): `registerDomainEvent(new BenhNhanRegisteredEvent(getId() != null ? getId() : 0, soCCCD, getHoTen()))`. | **Không bao giờ được gọi trong luồng thực tế.** UI `PatientRegistrationFormPanel.toRegisterCommand` (`clinic-desktop/.../PatientRegistrationFormPanel.java:345`) và `BenhNhanRepositoryImpl.mapPatient` (`clinic-infrastructure/.../BenhNhanRepositoryImpl.java:193`) đều khởi tạo qua constructor rỗng `new BenhNhan()` (`BenhNhan.java:51`) rồi gọi setter. | 1) **Bị bỏ qua trong luồng thật:** `RegisterPatientHandler.handle` (`clinic-application/.../RegisterPatientHandler.java:35-85`) nhận `BenhNhan` tạo bằng constructor rỗng nên danh sách `domainEvents` luôn rỗng.<br>2) **Sai `patientId = 0`:** Ngay cả khi gọi constructor 9 tham số trước khi lưu DB, `getId()` lúc đó là `null` nên `patientId` trong event luôn là `0` (`BenhNhan.java:63`), trong khi ID thật chỉ được gán sau lệnh `INSERT ... OUTPUT INSERTED.id` tại `BenhNhanRepositoryImpl.add` (`BenhNhanRepositoryImpl.java:119`).<br>3) **Dữ liệu nhạy cảm (PII):** Event giữ trực tiếp `cccd` và `fullName` (`BenhNhanRegisteredEvent.java:5-6`). |
| 2 | `LuotKhamStartedEvent` (`LuotKhamStartedEvent.java:3-18`) | `visitId` (`int`), `patientId` (`int`), `doctorId` (`int`) (`L4-L6`) | `LuotKham.startExam()` (`clinic-domain/.../examination/LuotKham.java:88-95`, dòng `L94`). Kiểm tra `trangThai == CHO_KHAM` (`L89`), chuyển sang `DANG_KHAM` (`L92`), gán `thoiGianBatDauKham = LocalDateTime.now()` (`L93`). | Chưa có handler hoặc UI nào gọi `startExam()`. `ReceptionQueueRepositoryImpl.registerVisit` (`clinic-infrastructure/.../ReceptionQueueRepositoryImpl.java:45-50`) chỉ tạo `LuotKham` ở trạng thái `CHO_KHAM` bằng `new LuotKham()` và setter. | Nếu `startExam()` được gọi trên thực thể chưa có `id` hoặc `maBacSi == null`, event nhận `visitId = 0` hoặc `doctorId = 0` (`LuotKham.java:94`). Phương thức cũng chưa cập nhật audit metadata (`markAsUpdated`). |
| 3 | `LuotKhamCompletedEvent` (`LuotKhamCompletedEvent.java:3-18`) | `visitId` (`int`), `patientId` (`int`), `doctorId` (`int`) (`L4-L6`) | `LuotKham.completeExam()` (`clinic-domain/.../examination/LuotKham.java:97-107`). Kiểm tra EMR chưa khóa và `trangThai == DANG_KHAM`, chuyển sang `HOAN_THANH`, gán thời điểm kết thúc rồi đăng ký event. | Chưa có handler hoặc UI nào gọi `completeExam()`; kiểm thử trực tiếp aggregate không chứng minh wiring consumer. | **Finding ban đầu đã được sửa:** nhận định cũ rằng phương thức cho phép hoàn tất từ `CHO_KHAM`/`DA_HUY` đã bị superseded. Nay trạng thái khác `DANG_KHAM` bị từ chối trước khi thay đổi trạng thái/thời gian/event (`LuotKham.java:101-107`); kiểm thử trạng thái hợp lệ và mọi trạng thái không hoạt động tại `LuotKhamTest.java:60-89`. |
| 4 | `EmrLockedEvent` (`EmrLockedEvent.java:3-18`) | `visitId` (`int`), `doctorId` (`int`), `signature` (`String`) (`L4-L6`) | `LuotKham.lockEmr(String signature, int doctorId)` (`clinic-domain/.../examination/LuotKham.java:109-124`). Từ chối nếu đã khóa, chữ ký null/blank hoặc doctor ID không dương; sau đó ghi trường khóa và đăng ký event. | Chưa có handler hoặc UI nào gọi `lockEmr(...)`. Trên UI, `ReopenMedicalRecordPanel.handleSave` (`clinic-desktop/.../ReopenMedicalRecordPanel.java:375-406`) chỉ hiển thị `JOptionPane` giả lập mà chưa gọi domain/DB. | **Finding ban đầu đã được sửa:** kiểm tra chữ ký và doctor ID hiện có trong `LuotKham.java:113-124`. `LuotKhamTest.java:107-158` kiểm tra tham số hợp lệ, null/blank, doctor ID không dương, khóa lặp và không đổi state/event khi bị từ chối. Aggregate test không chứng minh có handler hay consumer được wiring. |
| 5 | `DonThuocApprovedEvent` (`DonThuocApprovedEvent.java:3-18`) | `prescriptionId` (`int`), `visitId` (`int`), `doctorId` (`int`) (`L4-L6`) | `DonThuoc.approve()` (`clinic-domain/.../prescription/DonThuoc.java:38-44`, dòng `L43`). Kiểm tra `trangThai == TAM` (`L39-L41`), chuyển sang `DA_DUYET` (`L42`). | Chưa có repository, application handler hay màn hình UI nào gọi `DonThuoc.approve()`. | Nếu `approve()` được gọi trước khi `DonThuoc` được gán ID từ DB (`getId() == null`), `prescriptionId` trong event sẽ bằng `0` (`DonThuoc.java:43`). |
| 6 | `DonThuocDispensedEvent` (`DonThuocDispensedEvent.java:3-15`) | `prescriptionId` (`int`), `dispenserId` (`int`) (`L4-L5`) | `DonThuoc.completeDispense(int maNguoiCapThuoc)` (`clinic-domain/.../prescription/DonThuoc.java:46-54`, dòng `L53`). Kiểm tra `trangThai == DA_DUYET` (`L47-L49`), chuyển sang `DA_XUAT_HET` (`L50`). | Chưa có repository, application handler hay UI nào gọi `completeDispense(...)`. `PharmacyDispensePanel` (`clinic-desktop/.../PharmacyDispensePanel.java:50-54`) hiện dùng dữ liệu mẫu tĩnh trong bộ nhớ. | Không kiểm tra `maNguoiCapThuoc <= 0` (`DonThuoc.java:46-54`); `prescriptionId` bằng `0` nếu `getId() == null` (`L53`). |
| 7 | `HoaDonCreatedEvent` (`HoaDonCreatedEvent.java:3-18`) | `invoiceId` (`int`), `visitId` (`int`), `totalAmount` (`BigDecimal`) (`L6-L8`) | **Không có nơi nào đăng ký.** `HoaDon.java` có câu lệnh `import vn.clinicmanagement.domain.common.events.HoaDonCreatedEvent;` tại `clinic-domain/.../billing/HoaDon.java:7`, nhưng không bao giờ khởi tạo hay gọi `registerDomainEvent(new HoaDonCreatedEvent(...))`. | Không có. | **Sự kiện mồ côi (`Orphaned event`):** Tài liệu `DOMAIN_ANALYSIS.md` (`L130`, `L237`) ghi nhận `HoaDon` phát sinh `HoaDonCreatedEvent`, nhưng `HoaDon` (`HoaDon.java:47-68`) chỉ có constructor rỗng (`L47`) và `recordPayment` (`L49-L68`). |
| 8 | `PaymentRecordedEvent` (`PaymentRecordedEvent.java:3-18`) | `invoiceId` (`int`), `amountPaid` (`BigDecimal`), `paymentMethod` (`String`) (`L6-L8`) | `HoaDon.recordPayment(...)` (`clinic-domain/.../billing/HoaDon.java:49-68`, dòng `L67`): `registerDomainEvent(new PaymentRecordedEvent(getId() != null ? getId() : 0, amount, phuongThuc.name()))`. | Chưa có repository hay application handler nào gọi `recordPayment(...)`. `BillingInvoicePanel.handleCollectPayment` (`clinic-desktop/.../BillingInvoicePanel.java:264-295`) chỉ sửa trực tiếp `DefaultTableModel` (`L289-L291`). | Nếu `phuongThuc == null`, `phuongThuc.name()` tại `HoaDon.java:67` sẽ ném `NullPointerException`. Nếu `HoaDon` chưa có `id`, `invoiceId` trong event là `0` (`L67`). |
| 9 | `LichHenBookedEvent` (`LichHenBookedEvent.java:3-21`) | `appointmentId` (`int`), `patientId` (`int`), `doctorId` (`int`), `startTime` (`LocalDateTime`) (`L5-L8`) | Constructor 9 tham số của `LichHen` (`clinic-domain/.../appointment/LichHen.java:36-48`, dòng `L47`): `registerDomainEvent(new LichHenBookedEvent(getId() != null ? getId() : 0, maBenhNhan, maBacSi, thoiGianBatDau))`. | Chưa có repository hay application handler nào khởi tạo `LichHen`. | 1) **Còn hiệu lực — `appointmentId = 0` khi tạo trước khi persist:** `getId()` chưa được gán trong constructor nên event nhận `0` (`LichHen.java:47`); chưa có test xác nhận ID event sau persist.<br>2) **Finding ban đầu về null `createdBy` đã được sửa:** constructor gọi `setCreatedBy(createdBy)` (`LichHen.java:45`), còn `BaseEntity.setCreatedBy` chỉ từ chối giá trị khác null mà `<= 0` (`BaseEntity.java:46-51`). `LichHenTest.constructorPreservesNullCreator` (`clinic-domain/src/test/java/vn/clinicmanagement/domain/appointment/LichHenTest.java:10-20`) xác nhận constructor thành công với creator null và getter vẫn null. |
| 10 | `LowStockWarningEvent` (`LowStockWarningEvent.java:3-21`) | `itemId` (`int`), `itemName` (`String`), `currentStock` (`BigDecimal`), `threshold` (`BigDecimal`) (`L4-L7`) | **Không có nơi nào đăng ký hoặc import.** Đã kiểm tra `Item.java` (`clinic-domain/.../inventory/Item.java:11-74`), `TonKho.java` (`TonKho.java:6-30`), `PhieuXuatKho.java` (`PhieuXuatKho.java:11-44`), `FefoAllocationService.java` (`FefoAllocationService.java:7-27`). | Không có. | **Sự kiện mồ côi (`Orphaned event`):** Không có bất kỳ class nào tham chiếu tới `LowStockWarningEvent`. Ngoài ra, trường ngưỡng trong code đặt tên là `threshold` (`LowStockWarningEvent.java:7,20`), còn `DOMAIN_ANALYSIS.md:133` ghi là `minStock`. |

---

## 4. Ranh Giới Giao Dịch, Thời Điểm Phát Hành, Xử Lý Lỗi/Thử Lại, Lưu Trữ & Hành Vi UI

### 4.1. Ranh giới giao dịch (`Transaction Boundary`) và `Unit of Work`
1. **Hợp đồng giao dịch tầng Application:**
   - `TransactionBoundary` (`clinic-application/src/main/java/vn/clinicmanagement/application/common/TransactionBoundary.java:6-8`) định nghĩa `<T> Result<T> inTransaction(Supplier<Result<T>> operation);`.
   - `RegisterPatientHandler.handle` (`clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java:43-84`) bọc toàn bộ chuỗi kiểm tra trùng CCCD (`L45-L49`), thêm mới bệnh nhân (`L54`), tạo bảo hiểm (`L59-L66`) và tạo lượt khám (`L72`) bên trong `transactionBoundary.inTransaction(...)`.
2. **Quản lý kết nối và commit/rollback tầng Infrastructure:**
   - `JdbcTransactionManager` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java:27-63`) mở một `Connection` từ `DataSource` (`L39`), đặt `connection.setAutoCommit(false)` (`L40`), gắn vào `ThreadLocal<Connection> currentConnection` (`L14`, `L41`), gọi `unitOfWork.beginTransaction()` (`L43`), rồi thực thi `operation.get()` (`L44`).
   - Nếu `result == null || result.isFailure() || Boolean.TRUE.equals(rollbackOnly.get())` (`L45`), `JdbcTransactionManager` gọi `connection.rollback()` (`L46`) và `unitOfWork.rollback()` (`L47`). Nếu giao dịch lồng nhau (`nested transaction`) thất bại ở bên trong, cờ `rollbackOnly` (`L15`, `L31-L34`) đảm bảo giao dịch ngoài cùng vẫn bị rollback (`L45-L52`).
   - Khi thành công, `JdbcTransactionManager` gọi `unitOfWork.saveChanges()` (`L53`) ngay trước `connection.commit()` (`L54`), và dọn `ThreadLocal` trong khối `finally` (`L58-L62`).
3. **Thực trạng `JdbcUnitOfWork` và các Repository:**
   - `JdbcUnitOfWork` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcUnitOfWork.java:9-30`) chỉ giữ bộ đếm số dòng thay đổi `ThreadLocal<Integer> changedRows` (`L10-L24`), trả về tổng số dòng trong `saveChanges()` (`L22-L24`) và trả `saveChanges() >= 0` trong `saveEntities()` (`L27-L29`).
   - `JdbcUnitOfWork` **không** lưu tham chiếu tới các `BaseEntity` đã thêm/sửa, **không** đọc `getDomainEvents()`, **không** gọi `IDomainEventPublisher`, và **không** gọi `clearDomainEvents()`.
   - Toàn bộ 5 JDBC repository hiện có (`BenhNhanRepositoryImpl.java:103-180`, `BaoHiemRepositoryImpl.java:32-70`, `PhongKhamRepositoryImpl.java:27-81`, `ReceptionQueueRepositoryImpl.java:34-151`, `NhanVienRepositoryImpl.java:31-131`) thực thi câu lệnh SQL `INSERT`/`UPDATE` trực tiếp trên `Connection` hiện hành ngay khi phương thức repository được gọi (tức là bên trong `operation.get()` tại `JdbcTransactionManager.java:44`, trước `connection.commit()` tại `L54`) và chỉ gọi `unitOfWork.registerChange(...)`. Không có repository nào đọc hay xóa `domainEvents` của thực thể.

### 4.2. Phân tích thời điểm phát hành (`Transaction Timing`) và hành vi Lỗi / Thử lại (`Failure & Retry`)
- **Hiện trạng thời điểm phát hành:** Do chưa có `IDomainEventPublisher` và chưa có bước thu thập sự kiện trong `JdbcUnitOfWork` hay Application Handler, các sự kiện được `registerDomainEvent(...)` chỉ nằm trong `transient List<IDomainEvent> domainEvents` (`BaseEntity.java:28`) trên bộ nhớ heap và bị thu hồi bộ nhớ (`garbage-collected`) sau khi kết thúc use case.
- **Hệ quả về thời điểm giao dịch (`Pre-commit` vs. `Post-commit`) trên kiến trúc JDBC hiện hữu:**
  - *Trước `connection.commit()` (ví dụ tại điểm `unitOfWork.saveChanges()` ở `JdbcTransactionManager.java:53` hoặc cuối `operation.get()` ở `L44`):* Các handler chạy trên cùng thread và dùng cùng `JdbcConnectionProvider` (`JdbcTransactionManager.java:12,65-72`) sẽ tham gia vào cùng một giao dịch JDBC. Nếu handler ghi dữ liệu vào DB (như ghi nhật ký hệ thống/bệnh án) và ném ngoại lệ, toàn bộ giao dịch nghiệp vụ + handler sẽ cùng `rollback` tại `JdbcTransactionManager.java:55-57`. Ngược lại, nếu một bước sau đó trong `operation.get()` trả về `Result.failure(...)` (ví dụ `ReceptionQueueRepositoryImpl.registerVisit` thất bại tại `RegisterPatientHandler.java:72-75` sau khi `BenhNhanRepositoryImpl.add` đã chạy tại `L54`), mọi tác vụ phụ ngoài DB (như cập nhật UI hoặc gửi thông báo) nếu chạy quá sớm ngay lúc tạo thực thể sẽ phản ánh dữ liệu đã bị rollback.
  - *Sau `connection.commit()` (`JdbcTransactionManager.java:54`):* Đảm bảo dữ liệu nghiệp vụ đã được xác nhận bền vững trong DB (bao gồm các khóa chính `IDENTITY` sinh từ `OUTPUT INSERTED.id` tại `BenhNhanRepositoryImpl.java:119`, `BaoHiemRepositoryImpl.java:50`, `ReceptionQueueRepositoryImpl.java:55`). Tuy nhiên, nếu một handler chạy sau `commit()` bị lỗi, giao dịch nghiệp vụ đã hoàn tất và không thể tự rollback.
- **Hiện trạng xử lý lỗi, thử lại (`retry`) và tính lũy đẳng (`idempotency`):**
  - Hiện **không có** cơ chế retry, hàng đợi sự kiện hay bộ điều phối lỗi cho `IDomainEventHandler`.
  - Mặc dù tầng domain đã khai báo lớp `IdempotencyRecord` (`clinic-domain/src/main/java/vn/clinicmanagement/domain/inventory/IdempotencyRecord.java:5-23`) và tài liệu `DOMAIN_ANALYSIS.md:241-246` nhắc tới kiểm tra trùng lặp bằng `IdempotencyRecord`, nhưng trong toàn bộ các script migration `V1__Initial_Clinic_Schema.sql` (`L1-L1331`), `V2__Add_Optimistic_Locking_And_Audit_Constraint.sql`, `V3__Seed_Initial_Data.sql`, và `V4__Seed_Reception_Sample_Data.sql` **không tồn tại bảng `IdempotencyRecord`** và không có repository nào đọc/ghi thực thể này.

### 4.3. Hiện trạng Lưu trữ bền vững (`Persistence`) liên quan tới Sự kiện & Nhật ký
- Schema SQL `V1__Initial_Clinic_Schema.sql` có bảng `NhatKyHeThong` (`L1080-L1099`) và `NhatKyHoSoBenhAn` (`L1102-L1117`), tương ứng với hai thực thể domain `NhatKyHeThong` (`clinic-domain/.../system/NhatKyHeThong.java:10-81`) và `NhatKyHoSoBenhAn` (`clinic-domain/.../system/NhatKyHoSoBenhAn.java:9-62`). Tuy nhiên, hiện chưa có repository hay event handler nào trong `clinic-application` hoặc `clinic-infrastructure` ghi dữ liệu vào hai bảng này.
- Không có bảng lưu trữ sự kiện (`event store` hay `outbox`) trong `V1`–`V4`. Ứng dụng hiện là một Desktop Modular Monolith chạy trong một tiến trình JVM duy nhất kết nối trực tiếp tới SQL Server qua JDBC (`HikariCP` tại `DatabaseConfig.java`, giao dịch `ThreadLocal` tại `JdbcTransactionManager.java:14-63`), chưa có yêu cầu hay bằng chứng tích hợp message broker ngoài tiến trình trong phạm vi hiện tại.

### 4.4. Hành vi Giao diện (`clinic-desktop` UI Behavior)
- **Màn hình Tiếp đón (`ReceptionPanel`):**
  - Khi người dùng bấm đăng ký, `ReceptionPanel` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java:141-165`) chạy `registerPatientHandler.handle(command)` bên trong `SwingWorker.doInBackground()` (`L143-L146`).
  - Trong `SwingWorker.done()` trên luồng EDT (`L149-L163`), nếu `result.isSuccess()`, UI hiện hộp thoại thông báo, gọi `formPanel.resetForm()` (`L160`) và gọi trực tiếp `refreshData()` (`L161`) để tải lại danh sách phòng khám và hàng đợi tiếp đón qua `getReceptionQueueHandler.handle(...)` (`L75-L99`).
  - UI **không** lắng nghe `BenhNhanRegisteredEvent` hay bất kỳ `IDomainEvent` nào.
- **Các màn hình Bác sĩ, Khám bệnh, Nhà thuốc, Viện phí:**
  - `DoctorStationPanel` (`clinic-desktop/.../doctor/DoctorStationPanel.java:37-81`), `ExaminationPanel` (`clinic-desktop/.../examination/ExaminationPanel.java:159-264`), `PharmacyDispensePanel` (`clinic-desktop/.../pharmacy/PharmacyDispensePanel.java:50-54`) và `BillingInvoicePanel` (`clinic-desktop/.../billing/BillingInvoicePanel.java:264-295,350-377`) hiện dùng dữ liệu mẫu tĩnh trong bộ nhớ hoặc cập nhật trực tiếp `DefaultTableModel`, chưa kết nối với `IDomainEventPublisher` / `IDomainEventHandler`.

---

## 5. Khoảng Trống Kiểm Thử Hiện Tại (`Test Gaps`)

Đã rà soát toàn bộ **16 file test hiện có** trong checkout (đối chiếu `rg --files clinic-domain/src/test/java clinic-application/src/test/java clinic-infrastructure/src/test/java clinic-desktop/src/test/java`):
- `clinic-domain`: `DateRangeTest`, `MoneyTest`, `PhoneNumberTest` (3 file, 16 test case) — kiểm thử value object; không có test về domain event hoặc aggregate được nêu ở trên.
- `clinic-application`: `AuthenticateUserHandlerTest`, `Pbkdf2PasswordHasherTest`, `RegisterPatientDuplicateCccdTest`, `RegisterPatientHandlerTest`, `SearchAndReceptionQueueHandlerTest` (5 file, 26 test case) — không có test về phát hành/điều phối domain event.
- `clinic-infrastructure`: `DatabaseMigrationRunnerTest`, `FreshDatabaseMigrationSqlServerTest`, `JdbcTransactionManagerTest`, `SqlServerBatchSplitterTest`, `BenhNhanRepositoryImplTest` (5 file, 21 test case; migration SQL Server opt-in được skip trong lệnh mặc định) — không có test về phát hành domain event.
- `clinic-desktop`: `ReceptionSqlServerPersistenceTest`, `ReceptionSqlServerProcessRestartTest`, `PatientRegistrationFormPanelTest` (3 file, 8 test case; hai SQL Server E2E opt-in được skip trong lệnh mặc định) — không có test về UI lắng nghe domain event.

**Kết luận về độ phủ test sự kiện miền:** **0 / 16 file test** kiểm thử `BaseEntity.registerDomainEvent`, `getDomainEvents`, `clearDomainEvents`, `AbstractDomainEvent`, `IDomainEventPublisher`, `IDomainEventHandler`, hoặc bất kỳ lớp nào trong 10 lớp sự kiện miền. Đây là source inventory của checkout và khớp số liệu Maven `mvn clean verify`: 71 test được phát hiện, 68 pass, 0 failure/error, 3 SQL Server opt-in skip.

**Cập nhật sau review (05/11/2026):** Codex task `8109c55f-128d-4d6d-bdda-7a7fe21d9f3f` bổ sung `BaseEntityDomainEventsTest` với 5 test cho collection semantics (`register`, null rejection, immutable snapshots, `pull-and-clear`). Hiện có **1 / 18 file test** phủ phần collection nền; vẫn chưa có test publisher/handler, phát sinh event ở aggregate, hoặc phát hành trong transaction. Sau task này, root JDK 17 `mvn clean verify` phát hiện 78 test: 75 pass, 0 failure/error, 3 SQL Server opt-in skip.

**Đối chiếu bổ sung (06/11/2026):** Inventory và kết luận trên là snapshot lịch sử; chúng không phản ánh các test aggregate được bổ sung sau đó. `LuotKhamTest` hiện trực tiếp kiểm tra hoàn tất, khóa EMR, event và các trường hợp bị từ chối; `LichHenTest` xác nhận creator null. Chi tiết và giới hạn bằng chứng hiện hành được ghi tại Mục 10. Các test này không chứng minh event publication gắn với transaction hoặc consumer nghiệp vụ/audit.
---

## 6. Khế Ước Triển Khai Tối Thiểu Tiếp Theo Cho `PLAT-EVENT-01` (`[Khuyến nghị / Recommendation]`)

> **Nhãn phân loại:** Toàn bộ mục 6 dưới đây là **`[Khuyến nghị / Recommendation]`** nhằm định nghĩa bước triển khai tối thiểu, an toàn tiếp theo cho task `PLAT-EVENT-01` (theo `docs/ke-hoach-trien-khai/03-model-task-board.md:59-68`). Không áp đặt kiến trúc Outbox hay Message Broker ngoài tiến trình vì chưa có bằng chứng yêu cầu; đồng thời không tự đặt chính sách audit/bảo mật của Phase 2 (vốn thuộc `SEC-POLICY-01` và `SEC-AUDIT-01`).

### 6.1. Nguyên tắc thiết kế tối thiểu (`[Khuyến nghị / Recommendation]`)
1. **Giữ đúng kiến trúc Clean Architecture & Modular Monolith hiện hữu:**
   - Không thêm bảng Outbox, không thêm thư viện hàng đợi ngoài tiến trình khi ứng dụng đang chạy trong một tiến trình Desktop/JDBC duy nhất.
   - Tách bạch rõ phần **cơ chế thu thập/phát hành sự kiện trong bộ nhớ (`in-process event dispatching`)** của `PLAT-EVENT-01` khỏi phần **chính sách ghi nhận nhật ký hệ thống/EMR (`SEC-POLICY-01`, `SEC-AUDIT-01`)**.
2. **Bảo toàn tính đúng đắn của ID thực thể và ranh giới giao dịch:**
   - Với các thực thể nhận khóa chính `IDENTITY` từ DB sau lệnh `INSERT` (như `BenhNhan`, `LichHen`, `HoaDon`, `DonThuoc`, `LuotKham`), sự kiện phát hành ra cho handler phải mang ID thực tế (hoặc chỉ đăng ký/hoàn thiện sự kiện thông qua phương thức miền rõ ràng sau khi ID đã xác định, thay vì dựa vào constructor bị bỏ qua hoặc ghi `id = 0`).

### 6.2. Các hạng mục hợp đồng cụ thể cho bước code tiếp theo của `PLAT-EVENT-01` (`[Khuyến nghị / Recommendation]`)
1. **`[Khuyến nghị / Recommendation]` Hoàn thiện quản lý sự kiện trên `BaseEntity` và các Aggregate trong `clinic-domain`:**
   - Trong `BaseEntity.registerDomainEvent(IDomainEvent event)` (`BaseEntity.java:90-93`): bổ sung `Objects.requireNonNull(event, "event must not be null")`.
   - Cung cấp phương thức tiện ích `pullDomainEvents()` (hoặc sử dụng cặp `getDomainEvents()` + `clearDomainEvents()` nguyên tử) để lấy bản sao danh sách sự kiện hiện có và xóa ngay danh sách trên thực thể nhằm tránh phát hành trùng lặp trong cùng một vòng đời đối tượng.
   - Khắc phục các điểm đăng ký sự kiện bị lệch hoặc mồ côi trên Aggregate:
     - `BenhNhan` (`BenhNhan.java:51-64`): bổ sung phương thức miền tường minh (ví dụ `markRegistered()` hoặc factory/method được gọi thống nhất khi đăng ký bệnh nhân mới sau khi đã có ID/thông tin hợp lệ) thay vì chỉ đặt trong constructor 9 tham số đang không được gọi bởi `PatientRegistrationFormPanel` và `RegisterPatientHandler`.
     - `HoaDon` (`HoaDon.java:47-68`): bổ sung phương thức/factory khởi tạo hóa đơn hoặc đăng ký `HoaDonCreatedEvent` để khớp với import sẵn có tại `HoaDon.java:7` và `DOMAIN_ANALYSIS.md:130`, đồng thời kiểm tra `Objects.requireNonNull(phuongThuc, ...)` trong `recordPayment` (`L67`).
     - `LichHen` (`LichHen.java:36-48`): mục tiêu sửa lỗi creator null ở review ban đầu đã được đáp ứng; `setCreatedBy(createdBy)` chấp nhận null và được kiểm tra bởi `LichHenTest.constructorPreservesNullCreator` (`L10-L20`). Không còn là hạng mục mở. Vấn đề `appointmentId = 0` trước khi ID được gán vẫn cần được giải quyết/kiểm thử theo contract ID sự kiện.
     - `LuotKham` (`LuotKham.java:97-124`): các mục tiêu review ban đầu về chỉ hoàn tất khi `DANG_KHAM`, chữ ký khác null/blank và `doctorId > 0` đã được triển khai, với kiểm thử tại `LuotKhamTest.java:60-158`; không còn là hạng mục mở. Vẫn cần acceptance cho ID/payload khi aggregate chưa có ID thực.
     - `LowStockWarningEvent` (`LowStockWarningEvent.java:3-21`): bổ sung phương thức kiểm tra ngưỡng tồn kho trên `Item`/`TonKho` để phát sinh `LowStockWarningEvent` khi tồn kho thực tế rơi xuống hoặc thấp hơn `mucTonToiThieu` (`Item.java:24`), hoặc ghi chú rõ nếu hoãn sang `INV-CORE-01`.
2. **`[Khuyến nghị / Recommendation]` Cung cấp bộ phát hành sự kiện đồng bộ trong tiến trình (`In-Process Synchronous Publisher`):**
   - Triển khai một lớp `InMemoryDomainEventPublisher` (hoặc `SynchronousDomainEventPublisher`) implement `IDomainEventPublisher` (`IDomainEventPublisher.java:3-5`), cho phép đăng ký các `IDomainEventHandler<T>` (`IDomainEventHandler.java:3-5`) theo kiểu sự kiện `Class<T>` và điều phối tuần tự, xác định (`deterministic order`) trên luồng gọi.
   - Định nghĩa rõ quy ước thời điểm phát hành:
     - Nếu handler cần chạy **trong cùng giao dịch JDBC** (ví dụ: ghi `NhatKyHeThong` / `NhatKyHoSoBenhAn` trong `SEC-AUDIT-01` để đảm bảo nếu ghi audit lỗi thì giao dịch nghiệp vụ cũng rollback), sự kiện cần được thu thập và `publish` **trước** `connection.commit()` trong `JdbcTransactionManager.inTransaction` (`JdbcTransactionManager.java:44-54`) sau khi các lệnh repository đã gán xong ID.
     - Luôn đảm bảo `entity.clearDomainEvents()` được gọi sau khi đã thu thập sự kiện và mọi trạng thái tạm được dọn sạch trong `finally` nếu giao dịch rollback, tránh rò rỉ sự kiện của giao dịch thất bại sang lần gọi sau.
3. **`[Khuyến nghị / Recommendation]` Kiểm thử đơn vị tập trung với dữ liệu tổng hợp (`Synthetic Data Only`):**
   - Bổ sung unit test trong `clinic-domain` và `clinic-application` sử dụng dữ liệu tổng hợp (ví dụ: bệnh nhân giả lập `"Nguyen Van Mau"`, CCCD tổng hợp `"001099000001"`, bác sĩ ID `10`, lượt khám ID `100`):
     - Kiểm thử `BaseEntity`: `registerDomainEvent`, `getDomainEvents` (bất biến), `clearDomainEvents` (xóa sạch sau khi lấy).
     - Kiểm thử từng Aggregate (`BenhNhan`, `LuotKham`, `DonThuoc`, `HoaDon`, `LichHen`): xác nhận đúng loại event, đúng payload và thứ tự phát sinh khi chuyển trạng thái hợp lệ; xác nhận **không** phát sinh event khi invariant bị vi phạm (ném exception).
     - Kiểm thử `IDomainEventPublisher` + `IDomainEventHandler` giả lập (`capturing test handler`): xác nhận sự kiện được chuyển tới đúng handler đăng ký, danh sách sự kiện trên Aggregate được xóa sau khi publish, và ngoại lệ từ handler được lan truyền rõ ràng theo đúng ranh giới giao dịch.

---

## 7. Nhật Ký Kiểm Định (`Validation Record`)

### 7.1. Lệnh kiểm định đã thực thi và kết quả thực tế
Theo hướng dẫn của gói task (*"Run relevant checks when the sandbox permits them; report exact results and blockers. The Orchestrator runs its own validation after the agent."*), đã thực thi lệnh kiểm định Maven mặc định trong sandbox:

- **Lệnh chạy:** `mvn test`
- **Chế độ chạy:** Standard Sandbox (`BypassSandbox: false`) tại thư mục worktree `/Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/6f6e6515-661c-47ae-bf17-5e3abd56281e`
- **Kết quả thực tế / Blocker của Sandbox:**
  ```text
  Command failed to run: sandbox configuration error: deny .git/: non-absolute file path.
  ```
- **Nguyên nhân kỹ thuật:** Worktree hiện tại được mount dưới cấu hình sandbox của môi trường thực thi có rule `deny .git/: non-absolute file path`, khiến mọi lệnh shell khởi chạy qua `run_command` trong Standard Sandbox bị chặn ngay ở bước khởi tạo sandbox trước khi tiến trình `mvn` được tạo. Theo quy ước an toàn của Orchestrator (`docs/ke-hoach-trien-khai/03-model-task-board.md:31`), Agent không tự ý bật `BypassSandbox: true` trong phiên chạy tự động và không can thiệp vào cache/cấu hình ngoài phạm vi; toàn bộ thay đổi trong task này chỉ là 1 file Markdown mới (`docs/ke-hoach-trien-khai/phase0-domain-events-review.md`), không chạm tới bất kỳ file Java, `pom.xml`, hay tài nguyên build nào nên kết quả build/test Maven của dự án được giữ nguyên cho bước hậu kiểm của Orchestrator.

### 7.2. Tự đối chiếu Tiêu chí Nghiệm thu (`Acceptance Criteria Check`)
1. **Chỉ thay đổi `docs/ke-hoach-trien-khai/phase0-domain-events-review.md`:** **ĐẠT (PASS)** — Chỉ tạo duy nhất file báo cáo này, không sửa bất kỳ file nào khác.
2. **Mọi nhận định đều trỏ tới đường dẫn file/số dòng hoặc được gắn nhãn rõ ràng là khuyến nghị (`[Khuyến nghị / Recommendation]`):** **ĐẠT (PASS)** — Xem Mục 2, 3, 4, 5 (trích dẫn chính xác file và dòng) và Mục 6 (gắn nhãn `[Khuyến nghị / Recommendation]`).
3. **Mô tả thời điểm giao dịch, hành vi lỗi/thử lại và khoảng trống kiểm thử mà không áp đặt kiến trúc Outbox hay kiến trúc khác khi chưa có bằng chứng:** **ĐẠT (PASS)** — Xem Mục 4.1, 4.2, 4.3, 5 và 6.1.
4. **Ghi nhận chính xác lệnh kiểm định Maven mặc định và kết quả/blocker:** **ĐẠT (PASS)** — Xem Mục 7.1.

---

## 8. Phụ lục cập nhật sau review — `PLAT-EVENT-PUBLISHER-01` (06/11/2026)

> Phần 1–7 ở trên là snapshot lịch sử của review ban đầu; kết luận tại thời điểm đó được giữ nguyên. Phụ lục này ghi nhận trạng thái sau tích hợp cục bộ, không thay thế hay diễn giải lại các kết luận lịch sử.

ClinicOrchestrator task `0a565269-28b5-491a-85b6-abbe5ed81323` được tích hợp cục bộ bằng commit `5dba0e6`. `clinic-application` hiện có `InProcessDomainEventPublisher`: publisher đồng bộ trên luồng gọi, phân phối tới handler đăng ký theo đúng lớp sự kiện, giữ thứ tự đăng ký và truyền lỗi handler ra ngoài, dừng dispatch khi handler ném lỗi. Sáu unit test dùng sự kiện tổng hợp bao phủ exact-class dispatch, thứ tự/luồng gọi, không handler, kiểm tra null, truyền lỗi và tính độc lập giữa các instance.

**Ranh giới implementation:** đây là primitive dispatch in-process độc lập. Chưa có cơ chế thu thập sự kiện từ aggregate và dispatch gắn với transaction; publisher chưa được đăng ký/nối vào transaction manager, `UnitOfWork`, use case hay workflow lâm sàng/audit. Các test hiện tại không xác nhận aggregate event ID/payload hoặc chấp nhận payload sự kiện nghiệp vụ, và chưa có đăng ký handler nghiệp vụ thực.

**Còn lại dưới `PLAT-EVENT-01`:** (1) thiết kế/kiểm thử thu thập và dispatch event đúng ranh giới transaction, gồm hành vi rollback; (2) xác định và kiểm thử event ID/payload hợp lệ sau khi aggregate có định danh; (3) wiring handler thực theo contract nghiệp vụ/audit đã được duyệt; (4) xác định hành vi retry và liệu có cần outbox. Không có retry hoặc outbox được triển khai trong task publisher này. Parent `PLAT-EVENT-01` tiếp tục `[~]` In progress.

**Bằng chứng kiểm định được ghi nhận:** configured JDK 17 `mvn -Djava.awt.headless=true clean verify` thành công với **252 tests, 0 failures, 0 errors, 4 SQL Server opt-in skips** trên 56 Surefire XML reports. Kết quả này là baseline verify mặc định; bốn skip là test SQL Server opt-in, không thuộc dispatcher.

---

## 9. Phụ lục cập nhật — UTC event metadata foundation (`PLAT-EVENT-UTC-01`, 06/11/2026)

ClinicOrchestrator task `81d52408` được tích hợp cục bộ bằng commit `5510622`. Theo technical metadata decision DEC-TIME-01, `AbstractDomainEvent` nay mặc định lấy clock từ `Clock.systemUTC()` và chuyển `clock.instant()` thành `LocalDateTime` tại `ZoneOffset.UTC`. Giá trị trả về không chứa timezone hay offset; các trường ngày/giờ của nó được diễn giải là UTC. `IDomainEvent.getOccurredOn()` tài liệu hóa semantics này. Clock có thể được inject; hai test tổng hợp xác nhận một instant cố định vẫn ánh xạ sang UTC dù clock có zone `Asia/Tokyo`, và constructor từ chối `null` clock.

**Giới hạn phạm vi:** đây chỉ là nền tảng timestamp cho metadata sự kiện. Event consumers/storage hiện có chưa được wiring với thay đổi này, không có schema hoặc dữ liệu lưu trữ nào được chuyển đổi; không hoàn tất event collection/dispatch theo transaction, aggregate payload/ID, handler nghiệp vụ hoặc audit workflow. `PLAT-EVENT-01` tiếp tục `[~]` In progress với các hạng mục đó còn mở.

**Bằng chứng kiểm định được ghi nhận cho task:** configured JDK 17 `mvn -Djava.awt.headless=true clean verify` thành công với **254 tests, 0 failures, 0 errors, 4 SQL Server opt-in skips** trên 57 Surefire XML reports. Đây là default verify baseline mới; bốn SQL Server tests vẫn opt-in và không thuộc lượt mặc định.

---

## 10. Đối chiếu hiện hành — LuotKham và LichHen (06/11/2026)

Mục này supersede các finding lịch sử nêu trên khi chúng mâu thuẫn với source/test hiện tại; không xóa snapshot ban đầu.

| Hạng mục | Hiện trạng xác minh | Bằng chứng |
| :--- | :--- | :--- |
| `completeExam()` | Chỉ hoàn tất lượt khám ở trạng thái `DANG_KHAM`; EMR đang khóa vẫn bị từ chối. Trường hợp khác trạng thái không đổi state, thời gian kết thúc hoặc danh sách event. **Finding trạng thái cũ đã đóng.** | `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java:97-107`; `clinic-domain/src/test/java/vn/clinicmanagement/domain/examination/LuotKhamTest.java:60-104` |
| `lockEmr(signature, doctorId)` | Từ chối khóa lặp, chữ ký null/blank và doctor ID `<= 0`; chỉ sau khi qua kiểm tra mới ghi trường khóa và đăng ký `EmrLockedEvent`. **Finding validation cũ đã đóng.** | `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java:109-124`; `clinic-domain/src/test/java/vn/clinicmanagement/domain/examination/LuotKhamTest.java:107-158` |
| `LichHen` với `createdBy == null` | Constructor chuyển nguyên giá trị null cho `BaseEntity.setCreatedBy`; setter cho phép null. Constructor hoàn tất và `getCreatedBy()` trả null. **Finding runtime cũ đã đóng.** | `clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/LichHen.java:36-47`; `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java:45-51`; `clinic-domain/src/test/java/vn/clinicmanagement/domain/appointment/LichHenTest.java:10-20` |
| ID event `LichHenBookedEvent` | Vẫn có khả năng phát event với `appointmentId = 0`: constructor phát event khi aggregate chưa có ID và dùng fallback `0`. Test null creator không xác nhận ID event hoặc post-persistence behavior. **Finding này còn mở.** | `clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/LichHen.java:47`; chưa có assertion ID event trong `LichHenTest.java:10-20` |
| ID event `LuotKham` | `completeExam()` và `lockEmr()` dùng fallback visit ID `0` nếu `getId()` null; các test hành vi hiện tại khởi tạo visit với ID `42`, nên chưa xác nhận event ID cho aggregate chưa persist. | `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java:106,123`; `clinic-domain/src/test/java/vn/clinicmanagement/domain/examination/LuotKhamTest.java:170-176` |
| Phát hành/wiring | Việc aggregate đăng ký event và test xác nhận event trên aggregate **không** chứng minh event được publish trong transaction hoặc nối với handler nghiệp vụ, consumer giao dịch hay audit. `PLAT-EVENT-01` vẫn `[~] In progress`; task board nêu rõ transaction collection/dispatch và consumer wiring còn mở. | `docs/ke-hoach-trien-khai/03-model-task-board.md:45,175`; Mục 8 ở trên |

**Validation của lần đối chiếu này:** `mvn -pl clinic-domain -Dtest=LuotKhamTest,LichHenTest test` thành công (**24 tests, 0 failures, 0 errors, 0 skipped**); `git diff --check` thành công. Chỉ tài liệu này được phép thay đổi. Không bổ sung hoặc sửa test/source trong task report-only này.
