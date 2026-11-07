# Owner Visual Smoke Checklist for Restored Desktop Experience

**Document Status:** Grounded manual visual smoke checklist and static source comparison matrix  
**Target File:** `docs/ke-hoach-trien-khai/desktop-owner-visual-smoke-checklist.md`  
**Reference Commits:**
- Original Desktop Design Baseline (`main`): [`6aabb850ac0bc4296fbf548cae478702b311bc61`](../../clinic-desktop) (`6aabb85`)
- Current Feature Branch Baseline: `feature/patient-reception-workflow` at commit `e953bc5` (restored desktop tree from `982c1f6`)

---

## 1. Mục Đích, Quy Định Ranh Giới Và Giới Hạn Nghiệm Thu

### 1.1. Mục đích
Tài liệu này cung cấp danh mục kiểm tra trực quan (visual smoke checklist) có đối soát mã nguồn tĩnh (source-grounded) dành riêng cho **Chủ dự án (Owner)** trực tiếp nghiệm thu và so sánh trải nghiệm giao diện Desktop được khôi phục với bản thiết kế gốc trên nhánh `main`.

### 1.2. Ranh giới vận hành và bảo vệ dữ liệu (Approved Contract v1)
Theo hợp đồng đã được phê duyệt `18847587-233a-4f02-99cd-13e1eaf0736a`:
1. **Môi trường chạy ứng dụng trực quan:** Cơ sở dữ liệu cục bộ `clinicDb` trên SQL Server `localhost:1433` được dành riêng cho việc khởi chạy ứng dụng desktop thông qua script launcher (`run-app.sh`), kiểm tra trực quan thủ công bởi Owner và trải nghiệm thực tế.
2. **Nghiêm cấm kiểm thử tự động ghi vào `clinicDb`:** Mọi tiến trình kiểm thử tự động (automated tests, CI/CD của worker agents) **tuyệt đối không được kết nối hoặc ghi vào `clinicDb`**. Quá trình build/test mặc định `mvn test` và `mvn clean verify` bắt buộc phải chạy in-memory với dữ liệu tổng hợp (synthetic-only).
3. **Phân tách kiểm tra tĩnh và nghiệm thu trực quan:**
   - **Static Source Comparisons (Đối chiếu mã nguồn tĩnh):** Được phân tích và trích dẫn trực tiếp từ kho mã nguồn (paths và line numbers cụ thể) giữa bản gốc `6aabb85` và bản khôi phục `e953bc5` / `982c1f6`.
   - **Manual Launch & Visual Inspection (Khởi chạy và quan sát trực tiếp):** Bắt buộc phải do Owner trực tiếp khởi chạy trong môi trường đồ họa desktop thật để quan sát cách hiển thị pixel, phông chữ, bảng màu, hành vi cửa sổ và tương tác chuột/bàn phím.
   - **Không tự nhận nghiệm thu trực quan:** Agent thực hiện đối soát mã nguồn và biên soạn tài liệu; **tuyệt đối không tự nhận (claim) nghiệm thu trực quan** hoặc suy diễn kết quả quan sát của Owner.
4. **Bảo toàn thiết kế gốc:** Không tự ý đề xuất hoặc áp dụng việc thiết kế lại bố cục (redesign layout), bảng màu hoặc thành phần giao diện. Giữ nguyên tỷ lệ tối ưu cho màn hình 13-inch (1280×800 và 1440×900).
5. **Lỗi biên dịch tại baseline lịch sử `e953bc5`:** Thời điểm checklist được soạn, Desktop còn ba lỗi xác thực tại `AuthenticationCompositionRoot.java:19` và `LoginForm.java:136,140`. Các lỗi đó đã được xử lý trong ứng viên hiện tại; kết quả build mới được ghi ở mục 8. Không dùng bản dựng baseline `e953bc5` để nghiệm thu đăng nhập.

---

## 2. Đối Chiếu Mã Nguồn Tĩnh: Thiết Kế Gốc (`6aabb85`) vs Bản Khôi Phục (`e953bc5` / `982c1f6`)

Qua kiểm tra đối chiếu mã nguồn, toàn bộ các file thành phần giao diện chính của `clinic-desktop` sau khi được khôi phục tại `982c1f6` (và duy trì trên `e953bc5`) là **trùng khớp mã nguồn (byte-identical)** với thiết kế gốc tại commit `6aabb85`:

| Phân hệ / Màn hình | Tệp mã nguồn (Repository-relative path) | Dòng mã nguồn (Line numbers) | Hiện trạng đối chiếu mã nguồn tĩnh |
|---|---|---|---|
| **Cửa sổ đăng nhập** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java) | [L31–L98](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L31-L98) | Trùng khớp 100% với `6aabb85`. Kích thước cố định 440×390, không co giãn, nền trắng, 2 ô nhập liệu tên đăng nhập/mật khẩu, 1 nút đăng nhập lớn. Không có dropdown chọn vai trò/cơ sở trên UI. |
| **Khung Dashboard** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java) | [L42–L98](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L42-L98), [L103–L311](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L103-L311) | Trùng khớp 100% với `6aabb85`. Khung phóng to toàn màn hình, Ribbon đỉnh, vùng làm việc CardLayout ở giữa, dải MDI Tab cao 26px và thanh StatusBar ở đáy. |
| **Thanh trạng thái** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/components/StatusBarPanel.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/components/StatusBarPanel.java) | [L34–L85](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/components/StatusBarPanel.java#L34-L85) | Trùng khớp 100% với `6aabb85`. Chiều cao 25px, gồm chính xác 9 ô thông tin từ trái qua phải (Đã đăng ký, EHC mini, Tên cơ sở linh hoạt, Số thu, DB, VN, Đồng hồ thực tế, Thông báo nền vàng, Trợ giúp). |
| **Quản lý MDI Tab** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/WorkspaceManager.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/WorkspaceManager.java) | [L43–L100](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/WorkspaceManager.java#L43-L100) | Trùng khớp 100% với `6aabb85`. Điều phối chuyển đổi CardLayout, đồng bộ trạng thái chọn nút Ribbon, gọi vòng đời `onShow()` / `onHide()`. |
| **Màn hình Tiếp đón** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java) | [L28–L64](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L28-L64) | Trùng khớp 100% với `6aabb85`. Bố cục `BorderLayout`: Thanh công cụ đỉnh, JSplitPane dọc chia tại 218px (Form nhập liệu ở trên, Trạng thái phòng ở dưới), thanh phím tắt footer ở đáy. |
| **Form nhập Tiếp đón** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java) | [L64–L138](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java#L64-L138), [L141–L301](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java#L141-L301), [L316–L423](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java#L316-L423) | Trùng khớp 100% với `6aabb85`. Gồm thanh tìm kiếm nhanh đỉnh, chia 2 cột dọc (Cột trái: 8 dòng hành chính bệnh nhân; Cột phải: 8 dòng thông tin thẻ BHYT). |
| **Bảng phòng Tiếp đón** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ClinicRoomStatusPanel.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ClinicRoomStatusPanel.java) | [L37–L70](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ClinicRoomStatusPanel.java#L37-L70), [L75–L117](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ClinicRoomStatusPanel.java#L75-L117), [L122–L200](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ClinicRoomStatusPanel.java#L122-L200) | Trùng khớp 100% với `6aabb85`. Dải 9 tab ngang (mặc định tab 2 "Trạng Thái Phòng Khám"), hiển thị lưới 3 cột song song (cột giữa bôi màu ĐỎ phòng được chọn "Phòng Tiêm Chủng"). |
| **Bàn khám Bác sĩ** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java) | [L31–L130](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L31-L130) | Trùng khớp 100% với `6aabb85`. Bố cục **2 vùng chính (two-pane layout)** chia ngang tại 250px: Bên trái là cột hàng đợi bệnh nhân; bên phải là 5 tab lâm sàng (Tab 1 "Bệnh Nhân" chứa form lâm sàng và bảng lịch sử; 4 tab còn lại hiển thị placeholder thông báo sẵn sàng kết nối). |
| **Thanh công cụ Bác sĩ** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java) | [L28–L85](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L28-L85) | Trùng khớp 100% với `6aabb85`. Nút chức năng bác sĩ bên trái (nút "In" viền nền vàng nhạt); banner tóm tắt thông tin bệnh nhân chữ đỏ bên phải kèm ô tỷ lệ BHYT 80% to màu xanh navy. |
| **Hàng đợi Bác sĩ** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java) | [L31–L142](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L31-L142) | Trùng khớp 100% với `6aabb85`. Thanh lọc vàng nhạt phía trên, bảng hàng đợi renderer 2 dòng cho bệnh nhân, footer 2 hộp chọn "(Tất cả bệnh nhân)" và "(Tất cả khoa)". |
| **Lâm sàng & Tiền sử** | [`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java) & [`DoctorHistoryPanel.java`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java) | [`DoctorClinicalForm.java:48–112`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L48-L112); [`DoctorHistoryPanel.java:25–45`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L25-L45) | Trùng khớp 100% với `6aabb85`. Lưới GridBagLayout 11 hàng hiển thị thông tin hành chính/lâm sàng với nhãn liên kết màu xanh navy; bảng lịch sử 8 cột bên dưới. |

> [!IMPORTANT]
> **Đối chiếu thực tế so với tài liệu tiêu chuẩn `UI_CODING_STANDARDS.md`:**
> 1. **Bàn khám Bác sĩ:** Mã nguồn thực tế tại `6aabb85` và bản khôi phục là **bố cục 2 vùng (two-pane)**: cột hàng đợi bên trái (250px) và vùng làm việc đa tab bên phải. Tài liệu `UI_CODING_STANDARDS.md` (mục 2.3 dòng 223–229) mô tả thiết kế kỳ vọng 3 cột (hàng đợi trái, hồ sơ giữa, chỉ định phải). **Bắt buộc giữ nguyên bố cục 2 vùng của mã nguồn gốc; tuyệt đối không tự ý tái cấu trúc sang 3 cột khi chưa có yêu cầu từ Owner.**
> 2. **Màn hình Đăng nhập:** Mã nguồn gốc là form gọn 440×390 với 2 trường Username/Password. Tài liệu chuẩn mô tả các dropdown chọn vai trò/cơ sở; mã nguồn gốc không chứa các dropdown này.
> 3. **Biểu tượng Ribbon:** Mã nguồn gốc sử dụng các ký tự UTF-8/emoji làm icon trên `RibbonButton` (🏠, 📋, 👨‍⚕️, ⚙️, ❌). Đây là hiện trạng gốc cần giữ ổn định, không tự ý thay đổi icon khi nghiệm thu.

---

## 3. Quy Trình Khởi Chạy Và Các Bước Kiểm Tra Trực Quan Thủ Công (Manual Inspection Steps)

### 3.1. Yêu cầu môi trường thử nghiệm trực quan
- **Màn hình hiển thị:** Độ phân giải chuẩn 13-inch: **1280×800** hoặc **1440×900**, Display Scaling chuẩn 100% (hoặc Retina 2x trên macOS).
- **Phông chữ hệ thống:** Lớp [`AppFonts`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/config/AppFonts.java) cài đặt Segoe UI / Tahoma / Helvetica / Arial.
- **Tài khoản kiểm tra (Synthetic account):** Sử dụng tài khoản mẫu tổng hợp (ví dụ: `admin` hoặc `tiennv` với mật khẩu demo theo quy ước), tuyệt đối không dùng thông tin cá nhân thật.

---

### 3.2. Bước 1: Kiểm tra màn hình Đăng Nhập (`LoginForm`)
**Căn cứ mã nguồn:** [`LoginForm.java:31–98`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L31-L98)

1. **Thao tác khởi chạy:** Khởi chạy cửa sổ đăng nhập ứng dụng.
2. **Quan sát kích thước & vị trí:**
   - Cửa sổ xuất hiện chính giữa màn hình (`setLocationRelativeTo(null)`).
   - Kích thước chính xác **440×390 pixel**; viền cửa sổ không có nút phóng to (Maximize) và không thể kéo giãn kích thước (`setResizable(false)`).
   - Tiêu đề cửa sổ (Title): `Đăng nhập - Phần Mềm Quản Lý Bệnh Viện EHC`.
3. **Quan sát nội dung & bố cục hiển thị:**
   - **Nền:** Màu trắng (`Color.WHITE`), lề trong `EmptyBorder(25, 30, 25, 30)`.
   - **Phần đầu (Header):**
     - Dòng 1: `HỆ THỐNG Y TẾ THÔNG MINH EHC` (căn giữa, phông đậm `AppFonts.SECTION`, màu xanh thương hiệu `UIConstants.COLOR_PRIMARY`).
     - Dòng 2: `Đăng nhập tài khoản & chọn phân hệ làm việc` (căn giữa, chữ nhỏ `UIConstants.FONT_SMALL`, màu xám `Color.GRAY`).
   - **Phần thân (Form nhập):**
     - Nhãn `Tên đăng nhập:` (in đậm), bên dưới là ô nhập văn bản `txtUsername`.
     - Nhãn `Mật khẩu:` (in đậm), bên dưới là ô nhập mật khẩu che ký tự `txtPassword`.
     - Khoảng cách giữa các hàng đều đặn 6px (`GridLayout(4, 1, 6, 6)`).
   - **Phần chân (Footer):**
     - Nút bấm lớn màu xanh: `ĐĂNG NHẬP HỆ THỐNG` (chiều cao 42px, nền `UIConstants.COLOR_PRIMARY`, chữ trắng in đậm).
4. **Kiểm tra hành vi nhập liệu & phản hồi:**
   - Nhấn phím `Tab`: Con trỏ di chuyển tự nhiên từ ô `Tên đăng nhập` sang `Mật khẩu` rồi đến nút `ĐĂNG NHẬP HỆ THỐNG`.
   - Để trống và nhấn Đăng nhập: Xuất hiện hộp thoại cảnh báo `Vui lòng nhập tên đăng nhập và mật khẩu.` (tiêu đề `Cảnh báo`, icon cảnh báo).
   - Khi đang xử lý đăng nhập ngầm (`SwingWorker`): Nút bấm và các ô nhập liệu bị vô hiệu hóa tạm thời (`setEnabled(false)`) để chống nhấn đúp.

---

### 3.3. Bước 2: Kiểm tra khung Dashboard chính (`MainDashboardFrame`)
**Căn cứ mã nguồn:** [`MainDashboardFrame.java:42–98, 103–311`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L42-L98)

1. **Khởi chạy vào Dashboard:** Đăng nhập với quyền Bác sĩ (`role = "Bác sĩ điều trị"`) hoặc quyền Quản trị (`role = "admin"`).
2. **Quan sát hình học khung cửa sổ (Geometry):**
   - Cửa sổ tự động phóng to tối đa toàn màn hình (`MAXIMIZED_BOTH`), kích thước tối thiểu đảm bảo `1200×750`.
   - Tiêu đề thanh tiêu đề:
     - Với vai trò Bác sĩ: `PHẦN MỀM QUẢN LÝ BỆNH VIỆN - 2018.10.5.1 - BUILD: 10/2018`
     - Với vai trò Quản trị/khác: `PHẦN MỀM QUẢN LÝ BỆNH VIỆN - 2021.1.15.1 - 2021 - PC : DESKTOP-E64DCHT`
3. **Quan sát thanh Ribbon đỉnh (North Ribbon):**
   - **Ribbon vai trò Bác sĩ (`buildDoctorRibbonBar`):** Gồm chính xác 8 nút từ trái qua phải:
     1. `Trang Chủ` (🏠)
     2. `Đón Tiếp` (📋) — Có menu xổ xuống 6 mục khi nhấp chuột
     3. `Điều Trị` (👨‍⚕️) — Được chọn mặc định (viền nổi bật màu cam)
     4. `Hệ Thống` (⚙️)
     5. `Danh Mục` (📑)
     6. `Người Dùng` (👥)
     7. `Trợ Giúp` (❓)
     8. `Kết Thúc` (❌)
   - **Menu xổ xuống của nút "Đón Tiếp" (Popup 6 mục):** Nhấp vào nút "Đón Tiếp", kiểm tra menu xổ xuống nền trắng, viền xám hiển thị đủ 6 mục:
     1. `Đăng Ký Khám Bệnh`
     2. `Danh Sách Đón Tiếp`
     3. `Danh Sách Bệnh Nhân`
     4. `Màn Hình Chờ Tổng Hợp`
     5. `Kiosk Đăng Ký Khám`
     6. `Kiosk Đánh Giá Chất Lượng`
4. **Quan sát vùng làm việc MDI và dải Tab chân màn hình (Bottom MDI Tab Strip):**
   - Dải tab cao 26px nằm ngay phía trên thanh trạng thái, nền xanh xám nhạt `RGB(216, 228, 242)`.
   - Khi đăng nhập quyền Bác sĩ: Tab `Trang Chủ` và tab `Điều Trị` được tự động mở.
   - Nhấp vào tab `Trang Chủ`: Màn hình giữa chuyển sang `HomeWelcomePanel`, nút `Trang Chủ` trên Ribbon tự động chuyển trạng thái được chọn (đồng bộ hai chiều giữa Tab và Ribbon).
   - Nhấp vào nút đóng `×` trên tab: Tab đóng lại và giải phóng màn hình tương ứng.
5. **Quan sát thanh trạng thái chân màn hình (`StatusBarPanel`):**
   - Chiều cao chính xác **25px**, nền xám nhạt `RGB(240, 242, 246)`, viền trên 1px.
   - Kiểm tra hiển thị đủ **9 ô thông tin**:
     1. Ô 1 (96px): Chữ xanh đậm `Đã đăng ký` căn giữa.
     2. Ô 2 (148px): Icon EHC mini và nhãn `[EHC] - Phần mềm...`.
     3. Ô 3 (Co giãn linh hoạt): Tên cơ sở `PHÒNG KHÁM TAI MŨI HỌNG SHIHAN >>Ho...` (hoặc lấy từ session).
     4. Ô 4 (142px): Chữ xanh `Số thu: duongng test`.
     5. Ô 5 (108px): Chữ xanh `DB: SHIHh VN`.
     6. Ô 6 (32px): Chữ xanh `VN`.
     7. Ô 7 (162px): Đồng hồ thời gian thực `dd/MM/yyyy HH:mm:ss`, số giây cập nhật nhảy từng giây.
     8. Ô 8 (118px): **Nền vàng nhạt `RGB(255, 251, 214)`**, icon chuông xanh và chữ `Thông báo (0)`.
     9. Ô 9 (80px): Icon mic và nhãn `Trợ giúp`, không có vạch ngăn bên phải.

---

### 3.4. Bước 3: Kiểm tra phân hệ Tiếp Đón & Đăng Ký Khám (`ReceptionPanel`)
**Căn cứ mã nguồn:** [`ReceptionPanel.java:28–54`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L28-L54)

1. **Kích hoạt màn hình:** Nhấp vào nút `Đón Tiếp` -> chọn `Đăng Ký Khám Bệnh`.
2. **Quan sát bố cục phân chia (Split Pane):**
   - Vùng trung tâm được chia dọc bằng `JSplitPane`:
     - Nửa trên: Khung nhập liệu thông tin bệnh nhân & BHYT (`PatientRegistrationFormPanel`).
     - Vị trí thanh chia (Divider location): Chính xác tại **218px**, độ dày 4px.
     - Nửa dưới: Bảng dải tab và trạng thái phòng khám (`ClinicRoomStatusPanel`).
3. **Thanh công cụ đỉnh Tiếp đón (`ReceptionTopToolbar`):**
   - Chiều cao 50px, nền xanh nhạt `RGB(238, 243, 248)`.
   - Bên trái gồm 6 nút hành động dọc (icon phía trên, chữ phía dưới): `Menu`, `In`, `Làm Mới`, `Bệnh Nhân Mới`, `Nhập Thẻ BH`, `Kiểm Tra Thẻ BH`.
   - Bên phải gồm cụm:
     - Hộp `Cửa đón tiếp` có ô số `2` nổi bật **nền vàng rực `RGB(255, 255, 60)`**, viền đen.
     - Hộp có biểu tượng loa phát thanh và chữ `Gọi Số Tiếp Theo`.
4. **Khung nhập liệu (`PatientRegistrationFormPanel`):**
   - **Thanh tìm kiếm nhanh (trên cùng form):** Dropdown `Đối tượng:` (Viện phí, BHYT, Dịch vụ), checkbox đỏ `ĐN vn tiền`, nhãn tím `Tìm kiếm :`, ô `Số điện thoại:`, ô `Số CCCD:`.
   - **Chia 2 cột chính (JSplitPane ngang, tỷ lệ 0.58):**
     - **Cột trái (Hành chính bệnh nhân - 8 dòng):**
       - Dòng 1: `Mã BN:` (giá trị mẫu `22000326`), `Họ và tên:` (`NGUYỄN THỊ LIÊN`).
       - Dòng 2: `Giới tính:` (`Nữ`), `Ngày sinh:` chia 3 ô (Ngày `25`, Tháng `10`, Năm `2000`), nhãn tuổi xanh lam `22 tuổi`, `Nơi sinh:` (`bắc ninh`).
       - Dòng 3: `SN/thôn/phố:`, `Ty/Hộ/Xp:`, nhãn địa chỉ gợi ý, `Nghề nghiệp:` (`Nhân dân`) kèm nút `...`.
       - Dòng 4: `Điện thoại:` (`012345678`), `Email:`, `Dân tộc:` (`Kinh`), `Quốc tịch:` (`Việt nam`).
       - Dòng 5: `CMND/CCCD:` (`111122222333`), `Ngày cấp:` (3 ô: `01`/`01`/`2018`), `Nơi cấp:` (`bắc ninh`).
       - Dòng 6: Nút `TT khác`, `Nơi làm việc:` (`hà nội`).
       - Dòng 7: `ĐK khám:` (`Khám bệnh`), `Lúc:` (`10:39 23/08/2022`), `Lý do khám:` (`đau mỏi`).
       - Dòng 8: `Dịch vụ:` (mặc định chọn `Khám Nhi`), `Phòng khám:` (mặc định chọn `Phòng Tiêm Chủng` và **bôi vàng nổi bật**).
     - **Cột phải (Thông tin thẻ BHYT - 8 dòng):**
       - Tiêu đề `Thông tin thẻ BHYT` và 2 checkbox chữ đỏ: `TE có giấy khai sinh`, `Không giữ thẻ BHYT`.
       - Dòng 1: `Số BHYT:` chia 4 ô phân đoạn, `Nơi sống:` (dropdown: rỗng, K1, K2, K3).
       - Dòng 2: `KCBBĐ:` (2 ô mã và tên cơ sở KCB ban đầu).
       - Dòng 3: `Hạn thẻ:` (2 ô từ ngày - đến ngày).
       - Dòng 4: `Địa chỉ thẻ:` (ô text dài).
       - Dòng 5: `Tuyến KCB:` (Đúng tuyến / Trái tuyến), nhãn mức hưởng `0%` màu xanh đậm.
       - Dòng 6: Checkbox chữ xanh lam: `Giấy chứng nhận không cùng chi trả trong năm`.
       - Dòng 7: `Ngày miễn cùng chi trả:` (3 ô ngày/tháng/năm).
       - Dòng 8: `Ngày đóng đủ 5 năm liên tục:` (3 ô ngày/tháng/năm).
5. **Dải 9 Tab và Bảng trạng thái phòng khám (`ClinicRoomStatusPanel`):**
   - Kiểm tra đủ dải **9 tab ngang**: `DS Đăng Ký Khám`, `Lịch Sử Khám Và Điều Trị`, `Trạng Thái Phòng Khám` (mặc định chọn, có gờ xanh dương đậm trên đỉnh), `Sinh Hiệu`, `Zalo`, `Thông Tin Xác Thực`, `Gia Đình`, `Quân Nhân`, `Ghi Chú`.
   - **Giao diện Tab 2 ("Trạng Thái Phòng Khám"):** Hiển thị lưới **3 cột song song**:
     - Cột 1 (trái): Bảng các phòng khám với các cột `Tên Phòng Khám`, `ĐK`, `Chờ`, `Khám`.
     - Cột 2 (giữa): Bảng phòng khám trong đó dòng `Phòng Tiêm Chủng` có chữ và số được hiển thị **màu ĐỎ in đậm** (`Color(210, 35, 35)`).
     - Cột 3 (phải): Lưới ô kẻ trống sẵn sàng nạp STT.
   - **Chuyển sang Tab 0 ("DS Đăng Ký Khám"):** Bảng hiển thị 7 cột: `Ngày Đăng Ký | Số thứ tự`, `STT Khám`, `Trạng Thái`, `Ngày Hẹn`, `Ngày Khám`, `Bác Sĩ Khám`, `Ngày Ra` (kèm dòng dữ liệu mẫu STT 2).
6. **Thanh phím tắt chân trang Tiếp đón (`ReceptionFooterBar`):**
   - Chiều cao 40px, căn lề phải gồm 4 nút gradient bo góc có biểu tượng và 2 dòng chữ:
     1. `Bệnh Nhân Mới (F1)`
     2. `Lưu Lại (F2)`
     3. `In Phiếu Khám (F3)`
     4. `Thẻ KCB (F4)`

---

### 3.5. Bước 4: Kiểm tra Bàn Khám Bác Sĩ & Quản Lý Điều Trị (`DoctorStationPanel`)
**Căn cứ mã nguồn:** [`DoctorStationPanel.java:31–130`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L31-L130)

1. **Kích hoạt màn hình:** Nhấp vào nút `Điều Trị` trên Ribbon.
2. **Quan sát bố cục tổng thể (Two-Pane Layout):**
   - Phía trên là thanh công cụ bác sĩ và banner tóm tắt bệnh nhân (`DoctorActionBar`).
   - Phía dưới là `JSplitPane` ngang chia tại **250px**:
     - Cột trái: Hàng đợi bệnh nhân chờ khám (`DoctorQueueSidebar`).
     - Cột phải: Vùng làm việc đa tab lâm sàng (`clinicalTabs`).
3. **Thanh công cụ Bác sĩ & Banner tóm tắt bệnh nhân (`DoctorActionBar`):**
   - **Bên trái:** 9 nút hành động: `Menu`, `In` (nút **nền vàng nhạt `RGB(254, 237, 208)`** đặc trưng), `Làm mới`, `Tìm Kiếm`, `Bệnh Án`, `Dịch Vụ`, `Thuốc/VT`, `Phác Đồ ĐT`, `Thanh Toán`.
   - **Bên phải:** Banner thông tin tóm tắt bệnh nhân:
     - 3 dòng chữ màu đỏ đậm (`RGB(139, 20, 20)`):
       - Dòng 1: `TEST BN 033 | 18039327 | 1995 (23 tuổi) | Nam` (phông to `AppFonts.SECTION`).
       - Dòng 2: `GD-4-21-21-212-12125 | Còn 49 ngày`.
       - Dòng 3: `Xã Việt Hồng, H. Thanh Hà, Hải Dương`.
     - Khối BHYT ngoài cùng bên phải: Nhãn đỏ `BHYT` và số tỷ lệ to màu xanh navy đậm `80%`.
4. **Cột hàng đợi bệnh nhân bên trái (`DoctorQueueSidebar`):**
   - Độ rộng 250px.
   - Trên cùng là thanh lọc ngày vàng nhạt `RGB(255, 255, 215)` với nhãn `13/11/18 >> 13/11/18 - BH(115...` và biểu tượng kính lúp 🔍.
   - Danh sách bệnh nhân (bảng không kẻ ô, chiều cao dòng 38px): Mỗi bệnh nhân hiển thị 2 dòng (Dòng 1: Tên in đậm; Dòng 2: Mã bên dưới; Cột STT bên phải hiển thị số badge).
   - Dòng bệnh nhân `TEST BN 033` được chọn mặc định với màu nền xanh dương `RGB(0, 120, 215)`, chữ trắng.
   - Chân cột hàng đợi gồm 2 hộp chọn có mũi tên ▾: `(Tất cả bệnh nhân) - 1176` (chữ xanh đậm) và `(Tất cả khoa)` (chữ đỏ).
5. **Vùng làm việc 5 Tab lâm sàng bên phải:**
   - Gồm 5 tab: `Bệnh Nhân`, `Công Khám(1)`, `Thuốc(2)`, `Xử Trí`, `Phản Hồi`.
   - **Tab 1 ("Bệnh Nhân"):**
     - **Phần trên (`DoctorClinicalForm`):** Lưới thông tin lâm sàng 11 hàng bố trí cân đối bằng `GridBagLayout`:
       - Hàng 1: Mã BN (`18039327`), Tên bệnh nhân (`TEST BN 033`), Giới tính (`Nam`).
       - Hàng 2: Ngày sinh (`01/02/1995`), NS (`1995`), Nghề nghiệp (`Nhân dân`).
       - Hàng 3: Dân tộc (`Kinh`), Quốc tịch (`Việt Nam`), Điện thoại.
       - Hàng 4: Địa chỉ (`Xã Việt Hồng, H. Thanh Hà, Hải Dương`), Nơi làm việc.
       - Hàng 5: Người nhà, Đón tiếp lúc (`08:26 07/11/2018`), Loại đón tiếp (`Khám bệnh`).
       - Hàng 6: Đối tượng (`Bảo hiểm`), Số BHYT (`GD-4-21-21-212-12125`), Mức hưởng (`80%`).
       - Hàng 7: Nơi KCBBĐ, Hạn thẻ (`01/01/18 - 31/12/18`), Tuyến BHYT (`Đúng tuyến`).
       - Hàng 8: Mã bệnh án (`18KNGDC001599`), Số vào viện (`028850/18`), Mã điều trị (`346777`).
       - Hàng 9: Khoa phòng (`Ngoại Đầu cổ > P.Điều Trị Ngoại DC`), Bác sĩ điều trị (`Lê Tiến Quân`), Bệnh án (`Ung bướu`).
       - Hàng 10: Ngày vào (`09:40 11/11/2018`), Ngày ra, Chẩn đoán (`D21-Bướu lành khác của mô liên kết và mô mềm khác`).
       - Hàng 11: Bệnh kèm theo, Xử trí.
       - Các trường dạng liên kết (Link) hiển thị màu xanh navy đậm `RGB(15, 65, 145)`, có gạch chân viền mờ dưới và đổi con trỏ chuột dạng bàn tay (`HAND_CURSOR`).
     - **Phần dưới (`DoctorHistoryPanel`):** Tiêu đề xanh `Lịch Sử Khám và Điều Trị`, bảng 8 cột: `Ngày Vào`, `Hình Thức Vào`, `Khoa`, `Phòng`, `Ngày Ra`, `Xử Trí`, `Dịch`, `Chẩn Đoán`.
   - **Tab 2 đến Tab 5:** Hiển thị panel placeholder thông báo an toàn (ví dụ: `Nội dung chỉ định dịch vụ công khám & thủ thuật - Sẵn sàng kết nối dữ liệu khám bệnh EMR & chỉ định thuốc`).
6. **Thử nghiệm tương tác chuyển đổi bệnh nhân:**
   - Nhấp chọn bệnh nhân khác trong hàng đợi (ví dụ: `TEST BN 060` hoặc `PHẠM QUANG XX`).
   - Quan sát Banner tóm tắt đỉnh, form lâm sàng và bảng lịch sử lập tức cập nhật thông tin tương ứng của bệnh nhân vừa chọn mà không bị giật khung hình.

---

## 4. Các Giới Hạn Hành Vi Fail-Closed Bắt Buộc Đối Với Các Luồng Chưa Triển Khai

Để đảm bảo an toàn tuyệt đối và tuân thủ ranh giới cổng bảo mật `SEC-01`, toàn bộ các chức năng chưa được hoàn thiện use-case/phân quyền bắt buộc phải duy trì trạng thái **chặn đóng an toàn (fail-closed / default-deny)**:

1. **Các tab lâm sàng chuyên sâu (Bàn khám Bác sĩ):**
   - Các tab `Công Khám(1)`, `Thuốc(2)`, `Xử Trí`, `Phản Hồi` hiện tại hiển thị panel placeholder tĩnh. **Tuyệt đối không cho phép nhập liệu tự do, kê đơn thuốc hoặc thanh toán khi chưa có use case EMR/Pharmacy/Billing và chưa qua cổng `SEC-01`.**
2. **Các nút hành động trên thanh công cụ Bác sĩ:**
   - Các nút `Thuốc/VT`, `Phác Đồ ĐT`, `Thanh Toán`, `Dịch Vụ` không được thực hiện bất kỳ thao tác ghi (mutation) nào vào cơ sở dữ liệu khi chưa có ngữ cảnh phiên làm việc xác thực.
3. **Thao tác Gọi số và Lưu trên Tiếp đón:**
   - Nút `Gọi Số Tiếp Theo` trên `ReceptionTopToolbar` và nút `Lưu Lại (F2)` trên `ReceptionFooterBar` trong bản khôi phục chỉ hiển thị hộp thoại thông báo mô phỏng (`JOptionPane.showMessageDialog`). Chúng không được thực thi lệnh SQL trực tiếp hay thay đổi hàng đợi thực tế khi chưa nối qua Application Service và Transaction Manager.
4. **Menu Kiosk:**
   - Các mục `Kiosk Đăng Ký Khám` và `Kiosk Đánh Giá Chất Lượng` trên menu xổ xuống của nút "Đón Tiếp" chỉ bật hộp thoại thông tin giới thiệu mô hình, không kích hoạt luồng Kiosk phần cứng.
5. **Phương thức tải dữ liệu (`refreshData`):**
   - `ReceptionPanel.refreshData()` và `DoctorStationPanel.refreshData()` là các stub rỗng, không được thực hiện các truy vấn JDBC blocking trực tiếp trên luồng giao diện Swing EDT.
6. **Chính sách thoát ứng dụng:**
   - Các nút `Kết Thúc` hiện gọi `System.exit(0)` sau hộp thoại xác nhận. Khi khôi phục phiên đầy đủ, cần kích hoạt dọn dẹp phiên và ghi audit logout trước khi đóng tiến trình.

---

## 5. Bảng Bằng Chứng Nghiệm Thu Trực Quan (Compact Evidence Table)

> **Hướng dẫn dành cho Owner:** Điền kết quả quan sát thực tế vào cột **Kết quả (PASS / FAIL / DIFF)** và ghi chú chi tiết sai khác (nếu có) vào cột **Ghi chú của Owner**.

| Mã Kiểm Tra | Thành Phần / Khu Vực | Căn Cứ Mã Nguồn Tĩnh (`file:lines`) | Bước Kiểm Tra Trực Quan Thực Tế | Kết Quả Kỳ Vọng Hiển Thị | Phân Loại | Kết Quả (PASS / FAIL / DIFF) | Ghi Chú Của Owner |
|---|---|---|---|---|---|---|---|
| **CHK-LOGIN-01** | `LoginForm` Kích thước & Cửa sổ | [`LoginForm.java:31–37`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L31-L37) | Mở cửa sổ đăng nhập trên màn hình | Cửa sổ 440×390 pixel, căn giữa, không phóng to, không co giãn viền | Launch / Visual | `[ ]` | |
| **CHK-LOGIN-02** | `LoginForm` Header & Bố cục | [`LoginForm.java:42–56`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L42-L56) | Quan sát tiêu đề và phụ đề | Nền trắng, tiêu đề xanh thương hiệu in đậm, phụ đề xám căn giữa | Launch / Visual | `[ ]` | |
| **CHK-LOGIN-03** | `LoginForm` Trường nhập liệu | [`LoginForm.java:59–78`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L59-L78) | Nhập username/password, phím Tab | Tab chuyển tuần tự Username -> Password -> Nút Đăng nhập | Launch / Interaction | `[ ]` | |
| **CHK-LOGIN-04** | `LoginForm` Nút bấm & Phản hồi | [`LoginForm.java:81–95, 104–108`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L81-L95) | Nhấn đăng nhập khi ô nhập rỗng | Nút xanh chữ trắng; hiện hộp thoại cảnh báo `Vui lòng nhập...` | Launch / Interaction | `[ ]` | |
| **CHK-DASH-01** | `MainDashboardFrame` Tiêu đề & Cỡ khung | [`MainDashboardFrame.java:42–52`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L42-L52) | Quan sát khung Dashboard khi mở | Cửa sổ toàn màn hình, tiêu đề chứa số hiệu build và phiên bản tương ứng | Launch / Visual | `[ ]` | |
| **CHK-DASH-02** | `MainDashboardFrame` Doctor Ribbon | [`MainDashboardFrame.java:103–165`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L103-L165) | Kiểm tra Ribbon quyền Bác sĩ | Đủ 8 nút; nút "Điều Trị" được bôi cam mặc định; icon emoji gốc | Launch / Visual | `[ ]` | |
| **CHK-DASH-03** | `MainDashboardFrame` Reception Popup | [`MainDashboardFrame.java:176–232`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L176-L232) | Nhấp nút "Đón Tiếp" trên Ribbon | Menu xổ xuống nền trắng hiển thị đúng 6 mục chức năng | Launch / Interaction | `[ ]` | |
| **CHK-DASH-04** | `MainDashboardFrame` MDI Tab & Đồng bộ | [`MainDashboardFrame.java:85–98`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L85-L98) | Chuyển đổi qua lại giữa các tab ở đáy | Màn hình CardLayout chuyển đổi mượt mà; nút Ribbon tương ứng tự đổi trạng thái | Launch / Interaction | `[ ]` | |
| **CHK-STATUS-01** | `StatusBarPanel` 9 ô thông tin | [`StatusBarPanel.java:49–83`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/components/StatusBarPanel.java#L49-L83) | Kiểm tra thanh trạng thái chân màn hình | Cao 25px; hiển thị đủ 9 ô; đồng hồ nhảy từng giây; ô Thông báo nền vàng | Launch / Visual | `[ ]` | |
| **CHK-REC-01** | `ReceptionPanel` Bố cục phân chia | [`ReceptionPanel.java:43–49`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L43-L49) | Mở màn hình Đăng Ký Khám Bệnh | Split dọc chia tại 218px: Form phía trên, bảng phòng phía dưới | Launch / Visual | `[ ]` | |
| **CHK-REC-02** | `ReceptionTopToolbar` Nút & Gọi số | [`ReceptionTopToolbar.java:28–97`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ReceptionTopToolbar.java#L28-L97) | Quan sát thanh công cụ đỉnh | 6 nút trái; ô Cửa đón tiếp số 2 nền vàng rực; nút loa phát thanh | Launch / Visual | `[ ]` | |
| **CHK-REC-03** | `PatientRegistrationFormPanel` Cột trái | [`PatientRegistrationFormPanel.java:141–301`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java#L141-L301) | Quan sát cột hành chính bệnh nhân | Đủ 8 dòng; ngày sinh 3 ô; tuổi xanh lam; phòng khám bôi vàng nổi bật | Launch / Visual | `[ ]` | |
| **CHK-REC-04** | `PatientRegistrationFormPanel` Cột BHYT | [`PatientRegistrationFormPanel.java:316–423`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java#L316-L423) | Quan sát cột thông tin thẻ BHYT | Đủ 8 dòng; 2 checkbox đỏ; số BHYT 4 ô phân đoạn; checkbox miễn cùng chi trả | Launch / Visual | `[ ]` | |
| **CHK-REC-05** | `ClinicRoomStatusPanel` 9 Tab & Bảng phòng | [`ClinicRoomStatusPanel.java:37–70, 122–192`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ClinicRoomStatusPanel.java#L37-L70) | Kiểm tra bảng trạng thái phòng khám | 9 tab; tab 2 mặc định mở; 3 cột song song; "Phòng Tiêm Chủng" bôi ĐỎ nổi bật | Launch / Visual | `[ ]` | |
| **CHK-REC-06** | `ReceptionFooterBar` Phím tắt footer | [`ReceptionFooterBar.java:23–50`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/ReceptionFooterBar.java#L23-L50) | Quan sát thanh phím tắt chân trang | 4 nút gradient bo góc (F1, F2, F3, F4) căn lề phải | Launch / Visual | `[ ]` | |
| **CHK-DOC-01** | `DoctorStationPanel` Bố cục 2 vùng | [`DoctorStationPanel.java:91–130`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L91-L130) | Mở màn hình Bàn Khám Bác Sĩ (Điều Trị) | Bố cục 2 vùng (hàng đợi trái 250px, vùng làm việc phải); không phải 3 cột | Launch / Visual | `[ ]` | |
| **CHK-DOC-02** | `DoctorActionBar` Nút & Banner BN | [`DoctorActionBar.java:28–85`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L28-L85) | Quan sát thanh công cụ và banner đỉnh | 9 nút (nút "In" nền vàng nhạt); 3 dòng banner chữ đỏ; khối BHYT 80% xanh navy | Launch / Visual | `[ ]` | |
| **CHK-DOC-03** | `DoctorQueueSidebar` Hàng đợi BN | [`DoctorQueueSidebar.java:31–142`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L31-L142) | Quan sát hàng đợi bệnh nhân cột trái | Thanh lọc vàng nhạt đỉnh; mỗi dòng 2 hàng thông tin; dòng chọn nền xanh | Launch / Visual | `[ ]` | |
| **CHK-DOC-04** | `DoctorClinicalForm` Lưới thông tin | [`DoctorClinicalForm.java:48–112`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L48-L112) | Quan sát form lâm sàng tại tab Bệnh Nhân | 11 hàng bố trí cân đối; các trường liên kết dạng link màu xanh navy | Launch / Visual | `[ ]` | |
| **CHK-DOC-05** | `DoctorHistoryPanel` Bảng lịch sử | [`DoctorHistoryPanel.java:25–45`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L25-L45) | Quan sát bảng lịch sử khám | Bảng kẻ ô 8 cột; tiêu đề xanh `Lịch Sử Khám và Điều Trị` | Launch / Visual | `[ ]` | |
| **CHK-DOC-06** | `DoctorStationPanel` Placeholder tabs | [`DoctorStationPanel.java:121–125, 139–145`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L121-L125) | Nhấp lần lượt 4 tab: Công Khám, Thuốc, Xử Trí, Phản Hồi | Cả 4 tab hiển thị thông báo placeholder an toàn; không gây lỗi giao diện | Launch / Interaction | `[ ]` | |
| **CHK-SCALE-01** | Hiển thị màn hình 13-inch (1280×800) | [`UI_CODING_STANDARDS.md:646–657`](../../UI_CODING_STANDARDS.md#L646-L657) | Chạy thử nghiệm ở độ phân giải 1280×800 | Toàn bộ các form, thanh Ribbon, StatusBar hiển thị trọn vẹn, không vỡ layout | Launch / Scaling | `[ ]` | |
| **CHK-SCALE-02** | Hiển thị màn hình 13-inch (1440×900) | [`UI_CODING_STANDARDS.md:646–657`](../../UI_CODING_STANDARDS.md#L646-L657) | Chạy thử nghiệm ở độ phân giải 1440×900 | Bố cục co giãn hài hòa; ô Tên phòng khám trên StatusBar mở rộng vừa vặn | Launch / Scaling | `[ ]` | |

---

## 6. Báo Cáo Tại Baseline Khôi Phục `e953bc5` (Lịch Sử)

1. **Kết quả đối chiếu tĩnh:**
   - Mã nguồn các phân hệ giao diện khôi phục tại `982c1f6` (và hiện diện trên baseline `e953bc5`) khớp 100% về mặt cấu trúc thành phần, bố cục Swing, màu sắc và hình học với bản thiết kế gốc `6aabb85` trên `main`.
   - Các điểm lệch giữa tài liệu hướng dẫn `UI_CODING_STANDARDS.md` và mã nguồn gốc (Doctor Station 2 vùng thay vì 3 cột; Login không có dropdown vai trò; icon Ribbon dạng emoji) là các đặc điểm tồn tại từ đầu của thiết kế gốc và được bảo toàn nguyên vẹn.
2. **Kết quả biên dịch tại thời điểm lập checklist:**
   - Khi task này chạy trên baseline `e953bc5`, reactor dừng tại ba lỗi xác thực Desktop được ghi trong báo cáo tương thích. Đây là kết quả lịch sử, đã được cập nhật bằng kiểm tra mới ở mục 8.
   - Theo đúng phạm vi task, Antigravity chỉ sửa tài liệu checklist, không sửa Java, build file hoặc cấu hình.
   - Checklist gốc hướng dẫn Owner kiểm tra một ứng viên đã nối lại xác thực theo `SEC-01`; không dùng bản dựng baseline `e953bc5` để nghiệm thu đăng nhập.

---

## 7. Tài Liệu Hướng Dẫn Dự Án Đã Đọc (Guidance Files Read)

Quá trình thực hiện task đã đọc và tuân thủ các tài liệu quy chuẩn dự án bắt buộc:
1. `docs/PROJECT_CONVENTIONS.md` (Orchestrator baseline conventions)
2. `README.md` (Tổng quan dự án, cấu hình SQL Server môi trường phát triển)
3. `DOMAIN_ANALYSIS.md` (Báo cáo phân tích toàn diện kiến trúc miền DDD)
4. `ERD_DIAGRAM.md` (Sơ đồ thực thể quan hệ phân hệ Tiếp đón, Khám bệnh EMR, v.v.)
5. `UI_CODING_STANDARDS.md` (Quy chuẩn thiết kế giao diện Swing, tỷ lệ 13-inch, AppFonts, BaseViewPanel)
6. `docs/ke-hoach-trien-khai/00-hien-trang.md` (Hiện trạng và nguyên tắc phân tích)
7. `docs/ke-hoach-trien-khai/01-ma-tran-model-man-hinh.md` (Ma trận Domain model -> Màn hình Desktop)
8. `docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md` (Các đợt triển khai và điều kiện qua đợt)
9. `docs/ke-hoach-trien-khai/03-model-task-board.md` (Task board, Owner decisions, clinicDb boundary)
10. `PROJECT_COMPLETENESS_REPORT.md` (Đánh giá mức độ hoàn thiện)
11. `docs/ke-hoach-trien-khai/desktop-main-ui-preservation-review.md` (Báo cáo bảo toàn thiết kế gốc `6aabb85` vs `982c1f6`)
12. `docs/ke-hoach-trien-khai/desktop-main-auth-compatibility-review.md` (Báo cáo chi tiết 3 lỗi biên dịch xác thực Desktop)
13. `docs/ke-hoach-trien-khai/phase1-doctor-ui-visual-smoke-runbook.md` (Quy trình chạy smoke UI bác sĩ)
14. `docs/ke-hoach-trien-khai/phase2-sec01-baseline-acceptance.md` (Tiêu chuẩn nghiệm thu cổng bảo mật `SEC-01`)


## 8. Cập Nhật Bằng Chứng Trên Ứng Viên Hiện Tại

Kiểm tra lại trên `feature/patient-reception-workflow` tại `674f485` ngày 2026-11-07, sau khi nối lại đăng nhập/session và wiring Tiếp đón:

- `mvn -Djava.awt.headless=true clean verify`: **BUILD SUCCESS**; 69 Surefire report files, 402 tests, 0 failures, 0 errors, 4 skipped. Các bài SQL opt-in vẫn không chạy trong bộ test mặc định. Không kết nối `clinicDb`.
- Kiểm tra JAR `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar`: không có entry `db.properties`.
- Phương thức `LoginForm.initUI()` trên ứng viên hiện tại giống byte-for-byte với `main` tại source baseline. Các thay đổi ở LoginForm và Dashboard nằm ở luồng xác thực, phiên, khóa màn hình và điều hướng; chưa có xác nhận pixel hoặc thao tác trực quan từ Owner.
- Luồng Tiếp đón đã được nối lại vào handler ở Application. So sánh trực quan form, ribbon, bảng queue/phòng, hành vi nút và các màn hình gated vẫn cần Owner kiểm tra bằng giao diện chạy thật.
- Kết quả build và so sánh source không thay thế visual sign-off. Các ô checklist tương tác/hiển thị tiếp tục để trống cho Owner ghi nhận.

---

## 9. Updated Candidate Readiness Snapshot (2026-11-07 — leaderANTI)

- **Candidate branch & commit:** `feature/patient-reception-workflow` at `b7c550b` (incorporating Waves 1–10 through `f150edc`, `797cc8c`, and `b7c550b`; **466 passing reactor tests**: 100 domain + 131 application + 118 infrastructure + 117 desktop, 0 failures, 0 errors, 4 opt-in SQL Server skips; freshly packaged shaded jar `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar`).
- **Newly hardened desktop behaviors ready for Owner visual smoke:**
  1. `MainDashboardFrame` 'Người Dùng' dialog reads verified `CompletedLoginSession`, with headless lock/ribbon state sync helpers and `SessionLockOverlay` verification (`b4e29f6`, `074aa71`, `030e043`, `527ba96`).
  2. `ReceptionPanel` F1/F2 shortcuts, active room binding with duplicate room disambiguation (`TenPhong (#id)`), room summary tables with clearing on failure, 50-result search cap, worker epoch guards, safe message sanitizer with correlation IDs, and in-flight save form lock (`035e815`, `2efc3a6`, `096e8c0`, `947853d`, `1235e91`, `4e4035d`, `1239784`, `13178f9`, `81bf3a4`).
  3. Narrow platform error handling restored in `Main.java` (`PLAT-ERROR-03` `installLookAndFeel` fallback + `MainLookAndFeelTest`), `ExaminationPanel.java` (`getMenuShortcutKeyMaskEx(Toolkit)` + `ExaminationShortcutMaskTest`), and `DocxManager.java` (`PLAT-ERROR-DOCX-01` safe non-leaking save error message + `DocxManagerTest` / `DocxDocumentTest`) (`b69be2b`, `669cf3f`).
  4. `SEC-01`, `PAT-02`, and `REC-01` remain default-deny (`[~] In progress`) pending Owner visual smoke sign-off.


