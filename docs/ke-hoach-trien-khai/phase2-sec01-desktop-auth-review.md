# Báo cáo đánh giá độc lập xác thực và phiên làm việc Desktop (SEC-01)

**Mã tài liệu:** `docs/ke-hoach-trien-khai/phase2-sec01-desktop-auth-review.md`  
**Nhánh kiểm tra:** `feature/patient-reception-workflow` tại HEAD `a01bbc1` (cập nhật hardening tại `b4e29f6`, Wave 5 tại `074aa71`, Wave 6 tại `030e043`)  
**Ngày thực hiện:** 07/11/2026 (Asia/Ho_Chi_Minh)  
**Cập nhật Wave 4:** 07/11/2026 (Asia/Ho_Chi_Minh) — Khắc phục FINDING-SEC-01-02 và FINDING-SEC-01-03 tại commit `b4e29f6` (task `bea9a713`)  
**Cập nhật Wave 5:** 07/11/2026 (Asia/Ho_Chi_Minh) — Bổ sung headless regression tests cho FINDING-SEC-01-01 tại commit `074aa71` (task `c7b085c5` [leaderANTI])  
**Cập nhật Wave 6:** 07/11/2026 (Asia/Ho_Chi_Minh) — Bổ sung helper đồng bộ trạng thái `MainDashboardFrame.syncLockOverlayForSessionState` và headless regression test cho FINDING-SEC-01-01 tại commit `030e043` (task `cfe04e73` [leaderANTI])  
**Loại đánh giá:** Đánh giá tĩnh & đối soát kiểm thử độc lập (Read-only static source and focused test review).  
**Trạng thái cổng SEC-01:** `[~] In progress` — **Không tuyên bố hoàn tất cổng (No gate closure claimed)**; visual acceptance trên màn hình thực tế vẫn còn mở theo Definition A.

---

## 1. Mục tiêu và phạm vi tuân thủ

### 1.1. Mục tiêu
Báo cáo này tiến hành rà soát độc lập toàn diện mã nguồn và kiểm thử của phân hệ xác thực/phiên làm việc Desktop (`clinic-desktop`), đối soát trực tiếp với các chính sách an toàn thông tin **SEC-01** đã được Chủ dự án (Owner) phê duyệt ngày 05/11/2026 và 06/11/2026 (ghi nhận tại [`docs/ke-hoach-trien-khai/phase2-security-policy-proposal.md`](phase2-security-policy-proposal.md) và [`docs/ke-hoach-trien-khai/phase2-sec01-baseline-acceptance.md`](phase2-sec01-baseline-acceptance.md)).

### 1.2. Cam kết và giới hạn thực thi
- **Phạm vi tệp thay đổi duy nhất:** Báo cáo chỉ ghi nhận vào tệp `docs/ke-hoach-trien-khai/phase2-sec01-desktop-auth-review.md`. Không sửa đổi bất kỳ tệp mã nguồn Java, cấu hình hoặc schema nào.
- **Không truy cập CSDL thực/SQL Server:** Không kết nối đến `clinicDb` hay SQL Server `localhost:1433`. Không chạy các opt-in test yêu cầu SQL Server. Không tạo, xóa hoặc drop database.
- **Không thực hiện thao tác Git phá hủy:** Không push, không tạo PR, không merge, không xóa branch hay sửa lịch sử git.
- **Không tự ý đóng cổng nghiệp vụ:** Không tuyên bố đóng cổng SEC-01, PAT-02 hay REC-01. Mọi đánh giá tuân thủ nghiêm ngặt nguyên tắc chỉ ghi nhận bằng chứng kiểm thử tổng hợp (synthetic/in-memory).
- **Quy trình truy vấn RAG-first:** Đã tuân thủ quy trình nạp tài liệu hướng dẫn và trích đoạn tài liệu cục bộ; các trích dẫn đường dẫn và số dòng trong báo cáo này được đối soát trực tiếp từ mã nguồn thực tế tại commit `a01bbc1`.

---

## 2. Đối soát chi tiết 7 chính sách SEC-01 đã được phê duyệt

| STT | Chính sách SEC-01 được duyệt | Hiện trạng triển khai tại HEAD `a01bbc1` | Trích dẫn mã nguồn & dòng cụ thể | Đánh giá tuân thủ |
|---|---|---|---|:---:|
| 1 | **Một vai trò hiệu lực duy nhất mỗi phiên** (`One selected role per session`) | Sau khi xác thực danh tính, người dùng phải chọn đúng 1 vai trò được gán trong CSDL. Nếu có 1 vai trò, tự động chọn; nếu nhiều, bắt buộc người dùng chọn qua hộp thoại. Không gộp quyền (no role union), không chọn ngầm vai trò đầu tiên khi có nhiều vai trò. | - [`AuthenticateUserHandler.java:141-150`](../../clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateUserHandler.java#L141-L150): lọc vai trò gán hợp lệ, trả lỗi `AUTH_NO_ROLE` nếu trống.<br>- [`LoginForm.java:175-176, 301-306, 321-324`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L175-L176): hàm `chooseAssigned` tự động chọn nếu danh sách có độ dài 1, hoặc hiển thị hộp thoại `showInputDialog`; đối soát lựa chọn bằng so khớp danh tính (`choice == selected`).<br>- [`CompleteLoginSessionHandler.java:79-85, 108-112`](../../clinic-application/src/main/java/vn/clinicmanagement/application/auth/CompleteLoginSessionHandler.java#L79-L85): xác minh vai trò đã chọn thuộc danh sách gán và còn hiệu lực trong giao dịch CSDL.<br>- [`UserSession.java:38-45, 106-108`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L38-L45): lưu trữ đúng một `roleId`, `roleCode`, `roleName`. | **ĐẠT (PASS)** |
| 2 | **Phạm vi dữ liệu chính xác cặp cơ sở + khoa** (`Exact facility + department scope`) | Mỗi phiên chọn đúng một cặp cơ sở–khoa từ phân công hợp lệ. Mọi truy vấn và lệnh nghiệp vụ phải ràng buộc cả cơ sở lẫn khoa phòng. Không cấp quyền xem liên khoa hoặc toàn hệ thống. | - [`AuthenticateUserHandler.java:152-162`](../../clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateUserHandler.java#L152-L162): đọc danh sách phân công cơ sở–khoa, từ chối nếu không có phạm vi hợp lệ (`AUTH_NO_SCOPE`).<br>- [`LoginForm.java:177-179, 326-329`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L177-L179): ép buộc chọn cặp cơ sở–khoa từ danh sách được gán.<br>- [`CompleteLoginSessionHandler.java:87-93, 113-116`](../../clinic-application/src/main/java/vn/clinicmanagement/application/auth/CompleteLoginSessionHandler.java#L87-L93): kiểm tra lại phân công cơ sở–khoa trong CSDL trong transaction.<br>- [`UserSession.java:46-49, 111-114`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L46-L49): lưu `facilityId`, `facilityName`, `departmentId`, `departmentName`.<br>- [`MainDashboardFrame.java:60-68`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L60-L68): kiểm tra ngữ cảnh ủy quyền khớp hoàn toàn với phiên hoàn tất. | **ĐẠT (PASS)** |
| 3 | **Từ chối mặc định** (`Default-deny`) | Mọi quyền điều hướng và luồng nghiệp vụ chưa có cấu hình cấp phép rõ ràng (`isDuocPhep = true`, phạm vi `KHOA_PHONG`) đều bị chặn. Ma trận không cấp quyền cho workflow tương lai (EMR, CLS, Pharmacy, Billing). | - [`NavigationPermissionsHandler.java:47-78`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/NavigationPermissionsHandler.java#L47-L78): chỉ nạp các quyền `XEM` hợp lệ, cấm dòng mơ hồ hoặc khác `KHOA_PHONG`; nếu không có quyền nào thì trả về `NAVIGATION_GRANTS_MISSING`.<br>- [`DashboardNavigationPolicy.java:13-21`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/DashboardNavigationPolicy.java#L13-L21): quy tắc switch-case từ chối mặc định mọi khóa (`reception`, `treatment`, `examination`, `billing`, v.v.), chỉ cho phép `home` (nếu có grant `HOME`) và hai hộp thoại thông tin tĩnh `user`, `help`.<br>- [`MainDashboardFrame.java:364-381`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L364-L381): hàm `canNavigate`, `guardedButton`, `openGuarded` bảo vệ mọi nút bấm và hành động mở tab. | **ĐẠT (PASS)** |
| 4 | **Khóa nhàn rỗi 15 phút & hết hạn tuyệt đối 12 giờ** (`Idle 15m & Absolute 12h`) | Phiên tự động khóa sau 15 phút không hoạt động; hết hạn tuyệt đối sau 12 giờ tính từ thời điểm cấp phiên. Thời gian cấu hình từ `ThamSoHeThong`, nhưng bị giới hạn cứng không vượt quá trần 15 phút / 12 giờ. Hoạt động khi đang khóa không làm mở khóa hay gia hạn phiên. | - [`SessionTimeoutPolicy.java:26-56`](../../clinic-application/src/main/java/vn/clinicmanagement/application/session/SessionTimeoutPolicy.java#L26-L56): đọc cấu hình với mặc định an toàn 15 phút / 12 giờ.<br>- [`AuthenticationCompositionRoot.java:60-70`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/AuthenticationCompositionRoot.java#L60-L70): áp trần tối đa 15 phút nhàn rỗi và 12 giờ tuyệt đối ngay cả khi CSDL cấu hình lớn hơn.<br>- [`SessionLifecycleService.java:126-135, 151-185, 198-204`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L126-L135): ràng buộc `absoluteExpiresAt`, kiểm tra chuyển trạng thái `LOCKED_IDLE` và `LOCKED_ABSOLUTE`; hàm `recordActivity` chỉ cập nhật khi phiên `ACTIVE`.<br>- [`MainDashboardFrame.java:128-143, 392-409`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L128-L143): Timer 1 giây định kỳ kiểm tra khóa; AWT event listener lắng nghe bàn phím/chuột để ghi nhận hoạt động; chặn phím khi phiên không `ACTIVE`. | **ĐẠT (PASS)** |
| 5 | **Xác thực lại cùng tài khoản / vai trò / phạm vi** (`Strict Re-authentication`) | Mở khóa phiên yêu cầu nhập mật khẩu của chính tài khoản đó; kiểm tra lại vai trò và cơ sở–khoa còn được gán trong hệ thống. Nếu bị thu hồi hoặc đổi tài khoản khác: từ chối, ghi audit vi phạm, giữ nguyên trạng thái khóa. Khóa tuyệt đối bắt đầu phiên mới có correlation ID mới nhưng giữ bản nháp trên giao diện. | - [`SessionLifecycleService.java:206-288`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L206-L288): hàm `reauthenticate` kiểm tra ID, username, vai trò và phạm vi; ghi audit tương ứng `AUTH_INVALID`, `AUTH_FORBIDDEN`, `ACCOUNT_REVOKED`, `SCOPE_MISMATCH`.<br>- [`MainDashboardFrame.java:411-445`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L411-L445): thực hiện xác thực lại qua `SwingWorker` chạy ngầm, không làm đơ giao diện.<br>- [`SessionLockOverlay.java:24-58`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLockOverlay.java#L24-L58): Glass pane che phủ toàn bộ màn hình, chặn toàn bộ chuột/phím xuống các panel bên dưới. | **ĐẠT (PASS)** |
| 6 | **Fail-closed khi lỗi Audit hoặc cấp quyền** (`Fail closed on audit/grant failure`) | Thao tác đăng nhập, hoàn tất phiên và xác thực lại phải ghi audit thành công mới được tiếp tục. Nếu ghi audit thất bại hoặc nạp quyền lỗi, hệ thống lập tức hủy phiên/từ chối mở quyền và không để lại phiên hoạt động. | - [`CompleteLoginSessionHandler.java:102-156`](../../clinic-application/src/main/java/vn/clinicmanagement/application/auth/CompleteLoginSessionHandler.java#L102-L156): cập nhật lần đăng nhập và ghi audit `AUTH_LOGIN_SUCCESS` trong cùng giao dịch; nếu lỗi CSDL/audit thì rollback toàn bộ và trả lỗi `AUTH_UNAVAILABLE`.<br>- [`LoginForm.java:227-245`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L227-L245): nếu nạp quyền điều hướng thất bại (`navigationResult.isFailure()`), gọi `clearSession()` và từ chối mở Dashboard.<br>- [`SessionLifecycleService.java:255-261`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L255-L261): ghi audit `AUTH_REAUTH_SUCCESS` đồng bộ trước khi mở khóa; nếu lỗi ngoại lệ thì fail-closed và giữ phiên bị khóa. | **ĐẠT (PASS)** |
| 7 | **Dọn dẹp phiên & bảo vệ trực tiếp đường dẫn** (`Session-change cleanup & direct-route guards`) | Đăng xuất hoặc đóng cửa sổ phải xóa sạch `UserSession`, hủy tài nguyên workspace, cảnh báo bản nháp chưa lưu, chờ xả audit (drain) có giới hạn và quay về `LoginForm`. Mọi cách mở view trực tiếp (qua `openView`, phím tắt, popup) đều phải bị chặn khi chưa active hoặc không có quyền. | - [`MainDashboardFrame.java:447-481`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L447-L481): `requestClose` hiện hộp thoại cảnh báo bản nháp chưa lưu; dừng timer và listener; xóa phiên `sessionLifecycle.clear()`; đóng tất cả cửa sổ con; chạy worker chờ xả audit tối đa 3 giây (`awaitPendingAudits`) rồi mới quay về `LoginForm` hoặc thoát.<br>- [`MainDashboardFrame.GuardedWorkspace:489-523`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L489-L523): ghi đè `openView`, `activateView`, `closeView`, kiểm tra `allowed(key)` và chỉ cho phép `HomeWelcomePanel`. Các panel khác bị chặn hoàn toàn.<br>- [`MainDashboardFrame.java:225-285`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L225-L285): Popup menu Đón tiếp kiểm tra `canNavigate("reception")` cả trước khi hiển thị (`show`) và khi bấm từng mục con. | **ĐẠT (PASS)** |

---

## 3. Rà soát chuyên sâu từng thành phần cấu trúc

### 3.1. `LoginForm` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java`)
- **Khởi tạo và an toàn trạng thái:** Constructor ([`L40-L60`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L40-L60)) gọi ngay `UserSession.getInstance().clearSession()` để triệt tiêu mọi trạng thái sót lại từ phiên trước. Bổ sung `WindowListener` ([`L54-L59`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L54-L59)) tăng `attemptGeneration` và xóa phiên nếu người dùng đóng cửa sổ trước khi hoàn tất đăng nhập.
- **Xử lý bất đồng bộ chuẩn EDT:** Thao tác đăng nhập ([`L136-L200`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L136-L200)) tách biệt hoàn toàn giữa UI và I/O:
  - Kiểm tra cờ `loginPending` và `isDisplayable()` nhằm ngăn chặn click đúp.
  - Xóa mảng ký tự mật khẩu ngay sau khi đóng gói `AuthenticateUserCommand` ([`L153`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L153)).
  - Gọi handler xác thực trong `SwingWorker.doInBackground()` ([`L157-L159`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L157-L159)).
- **Lựa chọn vai trò và phạm vi:** Nếu tài khoản có nhiều vai trò hoặc nhiều phạm vi, hàm `chooseAssigned` ([`L301-L306`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L301-L306)) kích hoạt hộp thoại chọn tường minh; hủy chọn sẽ hủy toàn bộ tiến trình đăng nhập mà không kích hoạt Dashboard hay lưu session dở dang.
- **Chuẩn bị và kích hoạt phiên (Two-Phase Publication):**
  - Hàm `prepareSession` ([`L288-L299`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L288-L299)) khẳng định không chạy trên EDT (`assert !isEventDispatchThread()`). Thực hiện lệnh hoàn tất phiên, nạp ma trận điều hướng và tạo lifecycle service ngoài background thread.
  - Hàm `activateIfCurrent` ([`L312-L319`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L312-L319)) khẳng định chạy trên EDT (`assert isEventDispatchThread()`), chỉ công bố phiên vào `UserSession` khi cửa sổ đăng nhập còn hiển thị và đúng `generation`.

### 3.2. `AuthenticationCompositionRoot` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/AuthenticationCompositionRoot.java`)
- **Tích hợp đồng bộ Transaction & Repository:**
  - `createHandler()` ([`L25-L34`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/AuthenticationCompositionRoot.java#L25-L34)) và `createCompleteLoginSessionHandler()` ([`L36-L44`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/AuthenticationCompositionRoot.java#L36-L44)) sử dụng chung phiên bản `JdbcUnitOfWork`, `JdbcTransactionManager`, `JdbcConnectionProvider`, `NhanVienRepositoryImpl` và `JdbcAuditRepositoryImpl`. Đảm bảo tính nguyên tử (atomic) của giao dịch xác thực, khóa tài khoản (throttle store) và ghi nhật ký kiểm toán hệ thống.
- **Áp đặt giới hạn chính sách thời gian chờ (Ceiling Clamp):**
  - Trong `createSessionTimeoutPolicy()` ([`L60-L70`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/AuthenticationCompositionRoot.java#L60-L70)), hàm chủ động so sánh giá trị cấu hình từ cơ sở dữ liệu với trần được phê duyệt:
    ```java
    return new SessionTimeoutPolicy(
            configured.idleTimeout().compareTo(Duration.ofMinutes(15)) > 0
                    ? Duration.ofMinutes(15) : configured.idleTimeout(),
            configured.absoluteTimeout().compareTo(Duration.ofHours(12)) > 0
                    ? Duration.ofHours(12) : configured.absoluteTimeout());
    ```
    Quy tắc này bảo đảm: nếu CSDL bị cấu hình sai lệch hoặc cấu hình thời gian nhàn rỗi quá 15 phút hay thời gian tuyệt đối quá 12 giờ, hệ thống Desktop lập tức chặn lại và ép về mức trần tối đa đã được Owner phê duyệt.

### 3.3. `UserSession` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java`)
- **Quản lý phiên dạng Singleton an toàn Package-Private:**
  - Thuộc tính `loggedOn` được khai báo `volatile` ([`L11`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L11)).
  - Các phương thức biến đổi phiên: `setSession(...)` ([`L38-L59`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L38-L59)), `setLifecycleMetadata(...)` ([`L61-L72`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L61-L72)), `setLastActivityAt(...)` ([`L74`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L74)), `setLocked(...)` ([`L75-L78`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L75-L78)) đều để phạm vi truy cập mặc định (**package-private**). Mã ngoài package `vn.clinicmanagement.desktop.core.session` không thể tự ý sửa đổi danh tính, vai trò hay cơ sở–khoa.
  - Kiểm tra bất biến chặt chẽ: `setSession` ném `IllegalArgumentException` nếu vai trò hoặc phạm vi null ([`L39`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L39)); `setLifecycleMetadata` yêu cầu `absoluteExpiresAt.isAfter(issuedAt)` ([`L63`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L63)).
  - `clearSession()` ([`L80-L100`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java#L80-L100)) đặt lại toàn bộ định danh về 0, chuỗi rỗng và timestamp null.
  - **Hardening Wave 4 (commit `b4e29f6`):** Hai setter `setStationId(int)` và `setWorkstationId(String)` đã được thu hẹp về phạm vi package-private, đóng hoàn toàn FINDING-SEC-01-02 (được kiểm chứng qua `8/8` tests PASS trong `UserSessionContextTest`).

### 3.4. `SessionLifecycleService` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java`)
- **Thực thi `ActiveSessionPort` phục vụ Authorization ở tầng Application:**
  - `findActiveSession(UUID)` ([`L323-L345`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L323-L345)) đối soát UUID phiên, kiểm tra trạng thái hoạt động thực tế (`active == (currentState == ACTIVE)`), chưa bị khóa (`!session.isLocked()`), và chưa quá hạn tuyệt đối.
  - `currentAuthorizationContext()` ([`L347-L357`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L347-L357)) chỉ trả về ngữ cảnh hợp lệ khi phiên ở trạng thái `ACTIVE`.
  - `currentAuditActorContext()` ([`L360-L372`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L360-L372)) tự động sinh `correlationId` ngẫu nhiên mới (`UUID.randomUUID()`) cho từng thao tác nghiệp vụ, không tái sử dụng session ID cho correlation ID của audit nghiệp vụ.
- **Xác thực lại (Re-authentication) an toàn tuyệt đối:**
  - Hàm `reauthenticate` ([`L206-L288`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L206-L288)) khẳng định không chạy trên EDT ([`L208`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L208)).
  - Kiểm tra nghiêm ngặt: sai username/userId ➔ ghi nhận `AUTH_INVALID`/`AUTH_FORBIDDEN`; mất vai trò ➔ `ACCOUNT_REVOKED`; mất phân công cơ sở–khoa ➔ `SCOPE_MISMATCH`.
  - Ghi audit `AUTH_REAUTH_SUCCESS` đồng bộ qua `auditWritePort.appendIndependently(...)` ([`L256`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L256)); nếu việc ghi audit phát sinh ngoại lệ, hệ thống báo lỗi an toàn và trả về `false` (phiên vẫn bị khóa).
  - Đối với phiên hết hạn tuyệt đối (`LOCKED_ABSOLUTE`), hàm tái sinh phiên mới với correlation ID mới và mốc thời gian tuyệt đối mới ([`L272-L277`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L272-L277)).
- **Xử lý Audit bất đồng bộ không làm nghẽn UI EDT:**
  - Các sự kiện chuyển trạng thái tự động (`AUTH_SESSION_LOCKED_IDLE`, `AUTH_SESSION_EXPIRED_ABSOLUTE`, `AUTH_LOGOUT`) được phân phối qua `dispatchAudit` ([`L428-L455`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L428-L455)) trên thread ngầm daemon `DEFAULT_AUDIT_EXECUTOR`, hoàn toàn không gây giật lag giao diện Swing.
  - Hàm `awaitPendingAudits(Duration)` ([`L458-L471`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L458-L471)) hỗ trợ đợi xả hết các bản ghi audit tồn đọng khi đóng ứng dụng.

### 3.5. `SessionLockOverlay` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLockOverlay.java`)
- **Tấm chắn Glass Pane toàn diện:**
  - Khởi tạo với thuộc tính opaque, focus cycle root, tiêu thụ toàn bộ sự kiện chuột và con lăn (`addMouseListener`, `addMouseMotionListener`, `addMouseWheelListener` tiêu thụ event, [`L26-L31`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLockOverlay.java#L26-L31)).
  - Cung cấp form đăng nhập lại với trường Tên đăng nhập và Mật khẩu, hiển thị thông báo phân biệt giữa khóa nhàn rỗi và hết hạn tuyệt đối (`setAbsoluteExpiry`, [`L62-L66`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLockOverlay.java#L62-L66)).
  - Xóa mảng char mật khẩu ngay sau khi đóng gói credentials ([`L53`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLockOverlay.java#L53)).

### 3.6. `MainDashboardFrame` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java`)
- **Khởi tạo và đối soát ngữ cảnh ủy quyền:**
  - Constructor ([`L56-L77`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L56-L77)) yêu cầu bộ ba tham số: `CompletedLoginSession`, `Set<String> grants`, `SessionLifecycleService`. Nếu ngữ cảnh ủy quyền không khớp với phiên hoàn tất, ném `IllegalArgumentException` ngay lập tức ([`L60-L68`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L60-L68)). Không còn bất kỳ constructor nào cho phép truyền role text hay danh tính giả lập.
- **Giám sát nhàn rỗi và sự kiện toàn cục:**
  - `sessionMonitor` (Timer 1 giây, [`L128`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L128)) gọi `updateSessionLock()`.
  - `activityMonitor` (AWTEventListener, [`L129-L132`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L129-L132)) ghi nhận tương tác chuột/phím của người dùng.
  - `lockKeys` (KeyEventDispatcher, [`L133-L140`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L133-L140)) chặn mọi phím tắt đi vào các panel bên dưới khi phiên đang khóa hoặc đang đóng.
- **Không gian làm việc bảo vệ (`GuardedWorkspace`):**
  - Lớp `GuardedWorkspace` ([`L489-L523`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L489-L523)) kế thừa `WorkspaceManager`, ghi đè `openView`, `activateView`, `closeView`:
    ```java
    @Override public void openView(BaseViewPanel view) {
        if (view != null && view.getClass() == HomeWelcomePanel.class && allowed(view.getViewKey()))
            super.openView(view);
    }
    ```
    Đây là chốt chặn cực kỳ quan trọng: Trong bản khôi phục này, chỉ có `HomeWelcomePanel` được phép mở. Mọi panel nghiệp vụ nhạy cảm khác nếu cố tình gọi mở qua mã lệnh sẽ bị từ chối và chặn đứng.
- **Quy trình đóng không gian làm việc an toàn (`closeWorkspace`):**
  - Khi người dùng nhấn nút Thoát hoặc đóng cửa sổ, hàm `requestClose` ([`L447-L456`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L447-L456)) bật cảnh báo hủy các bản nháp chưa lưu.
  - Hàm `closeWorkspace` ([`L458-L481`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L458-L481)) dừng timer, gỡ listener, gọi `sessionLifecycle.clear()`, hủy worker xác thực lại, dọn dẹp view (`disposeViews()`), đóng các cửa sổ con, và đợi tối đa 3 giây để xả nhật ký kiểm toán ngầm (`awaitPendingAudits`) trước khi mở lại `LoginForm` hoặc thoát ứng dụng.
- **Hardening Wave 4 (commit `b4e29f6`):** Hộp thoại thông tin "Người Dùng" (`showUserInfoDialog`) đã được sửa để đọc thông tin hiển thị trực tiếp từ `CompletedLoginSession` thay vì `UserSession.getInstance()`, đóng hoàn toàn FINDING-SEC-01-03 (được kiểm chứng qua `8/8` tests PASS trong `UserSessionContextTest`).

### 3.7. `DashboardNavigationPolicy` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/DashboardNavigationPolicy.java`)
- Lưu trữ tập quyền `grants` dưới dạng bản sao bất biến `Set.copyOf` ([`L10`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/DashboardNavigationPolicy.java#L10)).
- Hàm `canOpen(String key)` ([`L13-L21`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/DashboardNavigationPolicy.java#L13-L21)):
  - Chỉ cho phép `"home"` nếu quyền chứa `"HOME"`.
  - Cho phép `"user"` và `"help"` vì đây là hộp thoại thông tin tĩnh/trợ giúp.
  - Từ chối mặc định (`default -> false`) cho tất cả các từ khóa phân hệ nghiệp vụ khác.

---

## 4. Đánh giá độ bao phủ kiểm thử (Test Coverage & Gaps Analysis)

### 4.1. Các bài kiểm thử tập trung hiện có
Các bài kiểm thử tự động sử dụng hoàn toàn dữ liệu tổng hợp (synthetic data) và mock clock/port:

1. **`SessionLifecycleServiceTest` (515 dòng, 17 ca kiểm thử)** ([`SessionLifecycleServiceTest.java:1-515`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleServiceTest.java#L1-L515)):
   - `idleAtFourteenFiftyNineRemainsActiveThenLocksAtFifteenMinutes`: kiểm tra chính xác mốc 899 giây vẫn active, 900 giây (15 phút) chuyển `LOCKED_IDLE` và ghi audit thành công.
   - `activityBeforeIdleBoundaryResetsItAndActivityWhileLockedDoesNotUnlock`: tương tác trước 15 phút làm mới hạn nhàn rỗi; tương tác khi đang bị khóa không làm mở khóa.
   - `idleReauthenticationRequiresSameUserRoleAndCurrentlyAssignedScopeAndAuditsEachOutcome`: kiểm tra xác thực lại thất bại theo từng trường hợp: sai user (`AUTH_FORBIDDEN`), sai/mất vai trò (`ACCOUNT_REVOKED`), sai/mất phân công khoa phòng (`SCOPE_MISMATCH`), thành công mở khóa và ghi audit.
   - `absoluteExpiryRequiresFreshAuthenticationAndStartsNewBoundedSessionWithSingleTransitionAudit`: hết hạn 12 giờ chuyển `LOCKED_ABSOLUTE`, xác thực lại cấp phiên mới và correlation ID mới.
   - `logoutClearsIdentifiersAndAllLifecycleTimestampsAndAuditsExactlyOnce`: đăng xuất xóa sạch định danh và timestamp, ghi audit 1 lần duy nhất.
   - `sessionWithoutBoundedLifecycleMetadataFailsClosed`: phiên thiếu metadata giới hạn tự động trả về `LOGGED_OUT`.
   - `activeSessionPortReturnsAnAtomicMatchingActorScopeSnapshot` & `activeSessionPortReportsLockedAndExpiredSessionsAsInactive`: kiểm tra tính toàn vẹn của cổng `ActiveSessionPort`.
   - `lockingAndLogoutStillApplyWhenIndependentAuditWriteFailsAndDiagnosticsRemainSafe`: lỗi ghi audit khi khóa/đăng xuất không làm hỏng trạng thái ngắt phiên, không làm lộ dữ liệu nhạy cảm trong log chẩn đoán.
   - `reauthenticationRemainsLockedWhenSuccessAuditCannotBeWritten`: lỗi ghi audit khi xác thực lại thành công lập tức fail-closed, giữ nguyên trạng thái khóa.
   - `sessionTransitionAuditRunsAsynchronouslyOffSwingEdtWithoutBlockingUiThread`: khẳng định I/O audit chạy ngoài EDT.
   - `listenerNotifiedImmediatelyOnSessionReplacementAndUnsubscribeSafely`: các listener nhận thông báo chuyển trạng thái tức thì.
   - `delayedStartupUsesCompletionIssuanceAndDoesNotExtendAbsoluteDeadline`: việc khởi động chậm của giao diện neo theo thời điểm hoàn tất đăng nhập `loginAtUtc()`, không kéo dài thời gian sống tối đa.
   - `expiredOrFutureCompletionCannotPublish`: từ chối phiên hoàn tất có thời gian tương lai hoặc đã hết hạn trước khi nạp.
   - `logoutDuringReauthenticationAuditPreventsLateUnlock`: hủy phiên khi đang ghi audit xác thực lại sẽ ngăn chặn việc mở khóa muộn.
   - `reauthenticationAndAuditDrainRefuseIoOnEdt`: chặn thao tác reauth và drain audit trực tiếp trên EDT.

2. **`SessionLockOverlayTest` (36 dòng, 1 ca kiểm thử)** ([`SessionLockOverlayTest.java:1-36`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/core/session/SessionLockOverlayTest.java#L1-L36)):
   - Kiểm tra Glass pane khi `visible` chặn toàn bộ input bên dưới (`isInputBlocked() == true`), bắt sự kiện nút Unlock và Logout.

3. **`DashboardNavigationPolicyTest` (6 ca kiểm thử)** ([`DashboardNavigationPolicyTest.java`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/dashboard/DashboardNavigationPolicyTest.java)):
   - `allSensitiveRoutesStayDeniedEvenWithEveryKnownGrant`: kiểm tra tất cả các route nhạy cảm (`reception`, `treatment`, `examination`, `billing`, `paraclinical`, `pharmacy`, `reports`, `admin`, `system`, `catalog`, `PATIENT`, `AUDIT_READ`) đều bị từ chối tuyệt đối ngay cả khi tập quyền chứa đầy đủ mọi mã grant.
   - `directOpenRetainedTabAndImpersonatedHomePanelAreGuarded`: xác minh `GuardedWorkspace` chặn đứng việc mở panel giả mạo hoặc panel không phải `HomeWelcomePanel`.
   - `dashboardHasNoFabricatedIdentityOrDisplayLabelConstructor`: kiểm tra `MainDashboardFrame` chỉ có đúng một constructor 3 tham số an toàn.
   - **Bổ sung Wave 5 (commit `074aa71`, task `c7b085c5` [leaderANTI]):** Bổ sung headless regression tests cho `MainDashboardFrame` định dạng thông tin người dùng (`formatDoctorUserInfo`/`formatStaffUserInfo`) và chuyển đổi trạng thái enable/disable của các nút bấm trên Ribbon khi phiên khóa nhàn rỗi / xác thực lại (`syncRibbonButtonsForSessionState`), đạt `5/5` tests PASS trong `DashboardNavigationPolicyTest`.
   - **Bổ sung Wave 6 (commit `030e043`, task `cfe04e73` [leaderANTI]):** Bổ sung helper đồng bộ trạng thái `MainDashboardFrame.syncLockOverlayForSessionState` và ca kiểm thử `lockOverlayVisibilityAndInputBlockingTrackSessionLifecycleTransitions` trong `DashboardNavigationPolicyTest` (xác minh tính hiển thị của `SessionLockOverlay` và cơ chế chặn input `isInputBlocked()` qua các chuyển đổi vòng đời phiên), đạt `6/6` tests PASS trong `DashboardNavigationPolicyTest`.

4. **Các bài kiểm thử tầng Application liên quan trực tiếp:**
   - `CompleteLoginSessionHandlerTest` (342 dòng, 10 ca kiểm thử): kiểm tra tính nguyên tử của giao dịch cập nhật đăng nhập, xác minh vai trò/phạm vi còn hiệu lực, rollback khi audit lỗi, bảo toàn correlation ID.
   - `NavigationPermissionsHandlerTest` (193 dòng): kiểm tra nạp quyền `XEM` cho từng vai trò và từ chối các mã vai trò không hỗ trợ hoặc phạm vi không phải `KHOA_PHONG`.
   - `SessionTimeoutPolicyTest` (42 dòng): kiểm tra parse cấu hình tham số hệ thống và áp dụng mặc định an toàn khi cấu hình thiếu hoặc sai.

### 4.2. Khoảng trống kiểm thử (Test Gaps)
Mặc dù tầng core session và logic nghiệp vụ được kiểm thử rất kỹ, qua rà soát phát hiện các khoảng trống kiểm thử cấp giao diện người dùng Desktop:
1. **Chưa có kiểm thử tự động cho `LoginForm` SwingWorker flow:** Hiện chưa có bài test headless mô phỏng việc bấm nút Đăng nhập trên `LoginForm`, chạy `SwingWorker`, hiển thị hộp thoại chọn vai trò/phạm vi (`chooseAssigned`) và xử lý cờ `generation`.
2. **Chưa có kiểm thử vòng đời tự động cho `MainDashboardFrame` Timer & AWT Listener:** Chưa có bài test Swing tích hợp kiểm tra Timer 1 giây định kỳ kích hoạt `updateSessionLock()` và AWTEventListener thực tế bắt các event chuột/phím trên cửa sổ để gọi `recordActivity()`.
3. **Chưa có kiểm thử tương tác luồng xác thực lại trên `MainDashboardFrame`:** Chưa có bài test mô phỏng `reauthenticate` chạy `SwingWorker` gọi `AuthenticationCompositionRoot.createHandler()` trong ngữ cảnh Dashboard thực tế.

---

## 5. Danh mục các phát hiện được xếp hạng ưu tiên (Prioritized Findings)

| Mã phát hiện | Mức độ ưu tiên | Trạng thái | Thành phần / Tệp nguồn | Mô tả chi tiết phát hiện | Khuyến nghị hành động & Ghi chú khắc phục |
|---|:---:|:---:|---|---|---|
| **FINDING-SEC-01-01** | **Medium (P2)** | **Đang hoàn thiện (In Progress / Partially Hardened)** | `clinic-desktop/src/test/java/vn/clinicmanagement/desktop/` | **Thiếu headless UI interaction tests cho `LoginForm` và `MainDashboardFrame`:** Các bài test ban đầu (`SessionLifecycleServiceTest`, `DashboardNavigationPolicyTest`, `SessionLockOverlayTest`) kiểm tra logic state machine rất tốt, nhưng chưa có test tự động kiểm chứng toàn bộ chu trình SwingWorker trong `LoginForm.onLogin()` và Timer trong `MainDashboardFrame`. | Bổ sung các bài test Swing headless có kiểm soát (sử dụng synthetic mock handler) để kiểm chứng chu trình chuyển trạng thái giao diện của `LoginForm` và `MainDashboardFrame` mà không cần hiển thị màn hình thật.<br>- **Wave 5 hardening (commit `074aa71`, task `c7b085c5` [leaderANTI]):** Đã bổ sung headless regression tests trong `DashboardNavigationPolicyTest` cho `MainDashboardFrame` user info formatting (`formatDoctorUserInfo`/`formatStaffUserInfo`) và idle-lock/reauthentication ribbon button state transitions (`syncRibbonButtonsForSessionState`, `5/5` tests PASS trong `DashboardNavigationPolicyTest`).<br>- **Wave 6 hardening (commit `030e043`, task `cfe04e73` [leaderANTI]):** Bổ sung helper `MainDashboardFrame.syncLockOverlayForSessionState` và ca kiểm thử `lockOverlayVisibilityAndInputBlockingTrackSessionLifecycleTransitions` trong `DashboardNavigationPolicyTest` (`6/6` tests PASS). |
| **FINDING-SEC-01-02** | **Low (P3)** | **ĐÃ GIẢI QUYẾT (RESOLVED)** | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java` | **Public setter `setStationId` và `setWorkstationId` trên Singleton `UserSession`:** Trong khi các trường danh tính và phạm vi (`userId`, `roleId`, `facilityId`, `departmentId`) đều được bảo vệ chặt chẽ bằng package-private setter, hai trường `stationId` và `workstationId` ban đầu có public setter, cho phép mã UI bên ngoài can thiệp trạm làm việc trong lúc phiên đang mở. | **RESOLVED** trong commit `b4e29f6` (task `bea9a713` [leaderANTI]): `UserSession.setStationId` và `setWorkstationId` đã được thu hẹp phạm vi truy cập về package-private (`UserSession.setStationId`/`setWorkstationId` narrowed to package-private). Kiểm thử: `8/8` tests PASS trong `UserSessionContextTest`. |
| **FINDING-SEC-01-03** | **Low (P3)** | **ĐÃ GIẢI QUYẾT (RESOLVED)** | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java` | **Nút "Người Dùng" (User) đọc chuỗi hiển thị trực tiếp từ Singleton `UserSession`:** Khi bấm nút "Người Dùng" trên Ribbon, action handler ban đầu đọc trực tiếp `UserSession.getInstance().getFullName()` thay vì lấy từ `CompletedLoginSession` bất biến được lưu trong phiên. Mặc dù đây chỉ là hộp thoại thông tin tĩnh không ảnh hưởng đến phân quyền, nhưng nên duy trì tính nhất quán về nguồn thông tin tin cậy. | **RESOLVED** trong commit `b4e29f6` (task `bea9a713` [leaderANTI]): `MainDashboardFrame` hộp thoại 'Người Dùng' đã chuyển sang đọc trực tiếp từ `CompletedLoginSession` thay vì `UserSession.getInstance()`. Kiểm thử: `8/8` tests PASS trong `UserSessionContextTest`. |
| **FINDING-SEC-01-04** | **Informational** | **Mở (Open)** | Toàn bộ phân hệ Desktop | **Kiểm thử trực quan (Manual Visual Smoke) chưa hoàn tất:** Theo Definition A, SEC-01 chưa thể đóng chỉ dựa trên kiểm thử headless và code review tĩnh. Review này không khởi chạy ứng dụng và không có bằng chứng visual sign-off của Owner. | Giữ nguyên trạng thái `SEC-01` là `[~] In progress`. Thực hiện phiên kiểm thử trực quan thủ công theo quy trình [`phase1-doctor-ui-visual-smoke-runbook.md`](phase1-doctor-ui-visual-smoke-runbook.md) trước sự chứng kiến/nghiệm thu của Owner. |

### 5.1. Nhật ký giải quyết phát hiện Wave 4 (Finding Resolution Log — Wave 4)

- **FINDING-SEC-01-02 (`UserSession.setStationId`/`setWorkstationId` narrowed to package-private):**
  - **Trạng thái:** **RESOLVED** trong commit `b4e29f6` (task `bea9a713` [leaderANTI]).
  - **Chi tiết xử lý:** Thu hẹp phạm vi truy cập của `UserSession.setStationId(int)` và `UserSession.setWorkstationId(String)` từ `public` về package-private, đồng nhất với cơ chế bảo vệ danh tính phiên (`userId`, `roleId`, `facilityId`, `departmentId`). Mã ngoài package `vn.clinicmanagement.desktop.core.session` không thể tự ý sửa đổi trạm làm việc.
  - **Bằng chứng kiểm thử:** `8/8` tests PASS trong `UserSessionContextTest`.
- **FINDING-SEC-01-03 (`MainDashboardFrame` 'Người Dùng' dialog reading from `CompletedLoginSession` instead of `UserSession.getInstance()`):**
  - **Trạng thái:** **RESOLVED** trong commit `b4e29f6` (task `bea9a713` [leaderANTI]).
  - **Chi tiết xử lý:** Hộp thoại 'Người Dùng' (`showUserInfoDialog`) trên `MainDashboardFrame` chuyển sang đọc thông tin định danh trực tiếp từ trường bất biến `completedSession` (kiểu `CompletedLoginSession`) được tiêm vào constructor thay vì gọi singleton mutable `UserSession.getInstance()`.
  - **Bằng chứng kiểm thử:** `8/8` tests PASS trong `UserSessionContextTest`.

### 5.2. Nhật ký làm cứng phát hiện Wave 5 (Finding Hardening Log — Wave 5)

- **FINDING-SEC-01-01 (`MainDashboardFrame` headless user info formatting and ribbon button state transitions):**
  - **Trạng thái:** **PARTIALLY HARDENED** trong commit `074aa71` (task `c7b085c5` [leaderANTI]).
  - **Chi tiết xử lý:** Bổ sung headless regression tests trong `DashboardNavigationPolicyTest` cho `MainDashboardFrame` định dạng thông tin người dùng (`formatDoctorUserInfo`/`formatStaffUserInfo`) và chuyển đổi trạng thái enable/disable của các nút bấm trên Ribbon khi phiên làm việc bị khóa nhàn rỗi hoặc sau khi tái xác thực thành công (`syncRibbonButtonsForSessionState`).
  - **Bằng chứng kiểm thử:** `5/5` tests PASS trong `DashboardNavigationPolicyTest`.

### 5.3. Nhật ký làm cứng phát hiện Wave 6 (Finding Hardening Log — Wave 6)

- **FINDING-SEC-01-01 (`MainDashboardFrame.syncLockOverlayForSessionState` and `lockOverlayVisibilityAndInputBlockingTrackSessionLifecycleTransitions` test):**
  - **Trạng thái:** **PARTIALLY HARDENED** trong commit `030e043` (task `cfe04e73` [leaderANTI]).
  - **Chi tiết xử lý:** Bổ sung helper `MainDashboardFrame.syncLockOverlayForSessionState` đồng bộ trạng thái hiển thị của `SessionLockOverlay` và chế độ hết hạn tuyệt đối với trạng thái phiên (`SessionLifecycleState`), đồng thời bổ sung ca kiểm thử headless regression `lockOverlayVisibilityAndInputBlockingTrackSessionLifecycleTransitions` trong `DashboardNavigationPolicyTest` nhằm xác minh tính hiển thị của `SessionLockOverlay` cũng như cơ chế chặn input (`isInputBlocked()`) đồng bộ chính xác theo các chuyển đổi trạng thái vòng đời phiên (`ACTIVE` -> `LOCKED_IDLE` -> `ACTIVE` sau khi re-auth).
  - **Bằng chứng kiểm thử:** `6/6` tests PASS trong `DashboardNavigationPolicyTest`.

---

## 6. Danh mục các cổng kiểm soát còn lại trước khi nghiệm thu SEC-01

Căn cứ theo bản nghiệm thu nền tảng [`docs/ke-hoach-trien-khai/phase2-sec01-baseline-acceptance.md`](phase2-sec01-baseline-acceptance.md), danh mục các điều kiện tiên quyết cần thực hiện trước khi có thể chính thức đóng cổng SEC-01 bao gồm:

- [x] **1. Khôi phục mã nguồn Desktop tương thích xác thực mới:** Khôi phục cấu trúc `LoginForm`, `MainDashboardFrame`, `SessionLifecycleService`, `UserSession`, `SessionLockOverlay`, `DashboardNavigationPolicy`. *(Đã hoàn thành tại HEAD `a01bbc1`)*.
- [x] **2. Ràng buộc một vai trò & một cặp cơ sở–khoa:** Kiểm tra tại `AuthenticateUserHandler`, `LoginForm` và `CompleteLoginSessionHandler`. *(Đã hoàn thành và xác minh mã nguồn)*.
- [x] **3. Bắt buộc từ chối mặc định (Default-deny):** Toàn bộ phân hệ nhạy cảm chưa sẵn sàng đều bị khóa trong `DashboardNavigationPolicy` và `GuardedWorkspace`. *(Đã hoàn thành và có unit test)*.
- [x] **4. Giới hạn phiên 15m nhàn rỗi và 12h tuyệt đối:** Áp dụng trần tối đa trong `AuthenticationCompositionRoot` và `SessionLifecycleService`. *(Đã hoàn thành và có unit test)*.
- [x] **5. Xác thực lại nghiêm ngặt & Fail-closed Audit:** Xác minh cùng user/role/scope, fail-closed khi audit lỗi. *(Đã hoàn thành và có unit test)*.
- [ ] **6. Thực hiện Manual Visual Smoke trên màn hình thật (Live Display):**
  - Khởi chạy ứng dụng Desktop qua script `run-app.sh` kết nối với cơ sở dữ liệu `clinicDb`.
  - Kiểm tra trực quan giao diện Đăng nhập chuẩn 440×390, kiểm tra hộp thoại chọn vai trò/phạm vi khi đăng nhập tài khoản có nhiều quyền.
  - Quan sát hiển thị Dashboard đúng theo vai trò (Bác sĩ hoặc Admin), kiểm tra Ribbon menu và các nút bị vô hiệu hóa an toàn.
  - Kiểm tra giao diện khóa phiên màn hình (Session Lock Overlay) sau 15 phút hoặc khi kích hoạt thử nghiệm; thực hiện mở khóa lại với mật khẩu đúng và sai.
  - Kiểm tra đăng xuất quay về màn hình đăng nhập sạch sẽ.
  - Ghi nhận biên bản kiểm thử trực quan với đầy đủ ảnh chụp, thông tin commit `a01bbc1`, ngày giờ và kết quả nghiệm thu của Chủ dự án.
- [ ] **7. Duy trì trạng thái Default-Deny cho các phân hệ tương lai:** Giữ nguyên trạng thái chưa kích hoạt (`default-deny`) đối với chuyển viện (Visit transfer), lịch hẹn (Appointment), bệnh án điện tử (EMR), cận lâm sàng (CLS), dược/kho (Pharmacy/Inventory) và viện phí (Billing) cho đến khi từng phân hệ này đạt cổng nghiệm thu riêng.

### Khuyến nghị chất lượng ngoài gate

`FINDING-SEC-01-01` (headless interaction tests cho `LoginForm`/`MainDashboardFrame`) là việc hardening được khuyến nghị, không phải điều kiện độc lập để đóng Gate Definition A. Gate vẫn yêu cầu các bằng chứng baseline đã duyệt và visual smoke có Owner sign-off.

---

## 7. Kết luận

Đánh giá tĩnh trên nhánh `feature/patient-reception-workflow` tại commit `a01bbc1` không tìm thấy đường bypass Critical/High trong các đường xác thực, scope và điều hướng đã được rà soát. Đây không phải kết luận runtime hoặc nghiệm thu toàn bộ phân hệ: Reception vẫn bị gate, một số luồng chưa được nối, và review không khởi chạy ứng dụng.

**SEC-01 tiếp tục `[~] In progress`** cho tới khi các tiêu chí Definition A được chứng minh trên ứng viên hiện tại và Owner hoàn tất visual smoke. Headless interaction tests được ghi là khuyến nghị chất lượng ngoài gate, không thêm điều kiện mới vào quyết định đã duyệt.
