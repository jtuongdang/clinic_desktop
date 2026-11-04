# QUY CHUẨN THIẾT KẾ GIAO DIỆN & PHÂN TÍCH TOÀN BỘ FORM UI/UX HỆ THỐNG DESKTOP (EHC HIS)
> **Tài liệu chuẩn hóa thiết kế giao diện, trải nghiệm người dùng (UI/UX) và kiến trúc mã nguồn cho toàn bộ màn hình, bảng điều khiển (Panel), hộp thoại (Dialog) trong hệ thống Clinic Management Desktop (EHC Hospital Information System).**  
> *Áp dụng bắt buộc cho toàn bộ đội ngũ kỹ sư phát triển giao diện hệ thống.*

---

## MỤC LỤC
1. [Tổng Quan Hệ Thống & Triết Lý Thiết Kế Medical UI/UX](#1-tổng-quan-hệ-thống--triết-lý-thiết-kế-medical-uiux)
2. [Nguyên Tắc Vàng Trong Lập Trình Giao Diện Desktop (Golden Rules)](#2-nguyên-tắc-vàng-trong-lập-trình-giao-diện-desktop-golden-rules)
3. [Hệ Thống Thiết Kế Nền Tảng (Foundational Design System)](#3-hệ-thống-thiết-kế-nền-tảng-foundational-design-system)
   - 3.1. [Typography & Font Chữ Chuẩn (`AppFonts.java`)](#31-typography--font-chữ-chuẩn-appfontsjava)
   - 3.2. [Bảng Màu Y Tế Chuẩn (`UIConstants.java`)](#32-bảng-màu-y-tế-chuẩn-uiconstantsjava)
   - 3.3. [Quy Chuẩn Không Gian, Lưới & Kích Thước (Grid & Layout Standards)](#33-quy-chuẩn-không-gian-lưới--kích-thước-grid--layout-standards)
   - 3.4. [Thư Viện Thành Phần Cốt Lõi Tái Sử Dụng (Core UI Components)](#34-thư-viện-thành-phần-cốt-lõi-tái-sử-dụng-core-ui-components)
4. [Phân Tích Chi Tiết Toàn Bộ Form & Giao Diện Hệ Thống (Form-by-Form Analysis)](#4-phân-tích-chi-tiết-toàn-bộ-form--giao-diện-hệ-thống-form-by-form-analysis)
   - 4.1. [Khung Vỏ Trung Tâm & Quản Lý Tab MDI (Shell & Workspace Manager)](#41-khung-vỏ-trung-tâm--quản-lý-tab-mdi-shell--workspace-manager)
   - 4.2. [Phân Hệ Đăng Nhập & Điều Phối Phiên (Authentication & Session)](#42-phân-hệ-đăng-nhập--điều-phối-phiên-authentication--session)
   - 4.3. [Phân Hệ Bàn Khám Bác Sĩ & Điều Trị Lâm Sàng (`modules/doctor/`)](#43-phân-hệ-bàn-khám-bác-sĩ--điều-trị-lâm-sàng-modulesdoctor)
   - 4.4. [Phân Hệ Khám Bệnh - Quản Trị & Điều Phối Hàng Đợi (`modules/examination/`)](#44-phân-hệ-khám-bệnh---quản-trị--điều-phối-hàng-đợi-modulesexamination)
   - 4.5. [Hệ Thống 8 Tab Lâm Sàng Chuyên Sâu Khám Bệnh](#45-hệ-thống-8-tab-lâm-sàng-chuyên-sâu-khám-bệnh)
   - 4.6. [Hệ Thống Hộp Thoại Nghiệp Vụ (Clinical Dialogs)](#46-hệ-thống-hộp-thoại-nghiệp-vụ-clinical-dialogs)
   - 4.7. [Hệ Thống In Ấn & Xem Trước Phiếu Y Tế (Print Preview System)](#47-hệ-thống-in-ấn--xem-trước-phiếu-y-tế-print-preview-system)
   - 4.8. [Hệ Thống Trình Đơn & Menu Ngữ Cảnh (Context Menus & Popups)](#48-hệ-thống-trình-đơn--menu-ngữ-cảnh-context-menus--popups)
   - 4.9. [Phân Hệ Đón Tiếp & Đăng Ký Khám Bệnh (`modules/reception/`)](#49-phân-hệ-đón-tiếp--đăng-ký-khám-bệnh-modulesreception)
   - 4.10. [Phân Hệ Quản Lý Viện Phí & Hóa Đơn (`modules/billing/`)](#410-phân-hệ-quản-lý-viện-phí--hóa-đơn-modulesbilling)
   - 4.11. [Phân Hệ Kho Dược & Cấp Phát Thuốc (`modules/pharmacy/`)](#411-phân-hệ-kho-dược--cấp-phát-thuốc-modulespharmacy)
   - 4.12. [Phân Hệ Cận Lâm Sàng & Chẩn Đoán Hình Ảnh (`modules/paraclinical/`)](#412-phân-hệ-cận-lâm-sàng--chẩn-đoán-hình-ảnh-modulesparaclinical)
   - 4.13. [Phân Hệ Quản Lý Danh Mục Dùng Chung (`modules/catalog/`)](#413-phân-hệ-quản-lý-danh-mục-dùng-chung-modulescatalog)
   - 4.14. [Phân Hệ Báo Cáo Thống Kê (`modules/reports/`)](#414-phân-hệ-báo-cáo-thống-kê-modulesreports)
   - 4.15. [Phân Hệ Quản Trị Hệ Thống & Phân Quyền (`modules/system/`)](#415-phân-hệ-quản-trị-hệ-thống--phân-quyền-modulessystem)
   - 4.16. [Trang Chủ & Bảng Tin Trung Tâm (`modules/home/`)](#416-trang-chủ--bảng-tin-trung-tâm-moduleshome)
5. [Chuẩn Hóa Trải Nghiệm Người Dùng Y Tế (Medical UX Standards)](#5-chuẩn-hóa-trải-nghiệm-người-dùng-y-tế-medical-ux-standards)
   - 5.1. [Bản Đồ Phím Tắt Toàn Diện (Keyboard-First Navigation Map)](#51-bản-đồ-phím-tắt-toàn-diện-keyboard-first-navigation-map)
   - 5.2. [Cơ Chế Phòng Chống Sai Sót Y Khoa (Medical Error Prevention)](#52-cơ-chế-phòng-chống-sai-sót-y-khoa-medical-error-prevention)
   - 5.3. [Quy Chuẩn Phản Hồi Trạng Thái & Độ Trễ (Feedback & Latency)](#53-quy-chuẩn-phản-hồi-trạng-thái--độ-trễ-feedback--latency)
6. [Cấu Trúc Thư Mục & Tổ Chức Mã Nguồn](#6-cấu-trúc-thư-mục--tổ-chức-mã-nguồn)
7. [Quy Trình Phát Triển Form Mới (Developer Checklist & Boilerplate)](#7-quy-trình-phát-triển-form-mới-developer-checklist--boilerplate)
8. [Lịch Sử Phiên Bản](#8-lịch-sử-phiên-bản)

---

## 1. TỔNG QUAN HỆ THỐNG & TRIẾT LÝ THIẾT KẾ MEDICAL UI/UX

Hệ thống **Clinic Management Desktop (EHC HIS)** là nền tảng quản trị bệnh viện và phòng khám chuyên nghiệp, phục vụ các luồng tác nghiệp liên tục với cường độ cao của nhân viên y tế (Bác sĩ, Điều dưỡng, Dược sĩ, Thu ngân, Cán bộ tiếp đón và Quản trị viên).

### 1.1. Triết Lý Thiết Kế Đặc Thù Y Tế (Medical Ergonomics)
1. **High Information Density (Mật độ thông tin cao):** Khác với ứng dụng web tiêu dùng (Consumer Web) có nhiều khoảng trắng lớn, phần mềm y tế desktop ưu tiên tối đa diện tích hiển thị để Bác sĩ có thể quan sát đồng thời: Bệnh sử, Thông tin BHYT, Sinh hiệu, Kết quả CLS và Đơn thuốc mà không phải cuộn trang quá nhiều lần.
2. **Keyboard-First Architecture (Ưu tiên bàn phím số 1):** Bác sĩ và nhân sự tiếp đón khám hàng trăm lượt mỗi ngày. Mọi thao tác cốt lõi (Ghi nhận bệnh nhân, Thêm dịch vụ, Kê đơn, Lưu, In ấn, Chuyển tab) đều có phím tắt (F1–F12, Enter chuyển ô, Esc đóng popup, Ctrl+F tra cứu) để thao tác mà không cần rời tay khỏi bàn phím.
3. **Zero-Error Affordance (Ngăn ngừa tuyệt đối sai sót y tế):** Các trường hợp rủi ro cao (Dị ứng thuốc, Trùng thuốc, Hạn thẻ BHYT hết hạn, Thuốc sắp hết hạn trong kho) phải có cảnh báo thị giác trực quan (Màu đỏ `#8B0000`, Hộp thoại xác nhận, Badge trạng thái nổi bật).
4. **Single-Window MDI Desktop Experience:** Người dùng chỉ làm việc trong một cửa sổ ứng dụng duy nhất (`MainDashboardFrame`). Các chức năng phân hệ được mở theo cơ chế Tab dưới chân màn hình (Bottom Tabs), giữ trạng thái làm việc (Stateful), không mở tràn lan cửa sổ rời gây mất dấu công việc.

---

## 2. NGUYÊN TẮC VÀNG TRONG LẬP TRÌNH GIAO DIỆN DESKTOP (GOLDEN RULES)

1. **TUYỆT ĐỐI KHÔNG TỰ Ý TẠO FONT:** Cấm sử dụng `new Font("...", ...)` rải rác hoặc hardcode chuỗi font size trong HTML `<span style='font-size:...'>`. Toàn bộ font chữ của giao diện phải tham chiếu tập trung qua lớp hằng số [`AppFonts`](file:///Users/jtuong/Documents/desktop_clinic/desktop/clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/config/AppFonts.java).
2. **CẤM DÙNG EMOJI HỆ THỐNG LÀM ICON CHÍNH:** Tuyệt đối không dùng ký tự emoji UTF-8 (🩺, 💊, 🏠, 📋...) cho các icon nút bấm quan trọng trong ứng dụng production vì macOS, Windows 10/11 và Linux sẽ render hình dạng, màu sắc khác nhau, dễ gây vỡ khung hoặc lỗi ô vuông font `[]`. Mọi icon giao diện phải vẽ bằng **Vector Graphics (`Graphics2D`)** hoặc đối tượng `Icon` chuẩn hóa.
3. **MỌI MÀN HÌNH CHÍNH PHẢI KẾ THỪA `BaseViewPanel`:** Không tạo `JFrame` riêng lẻ cho các màn hình nghiệp vụ. Toàn bộ view phải kế thừa [`BaseViewPanel`](file:///Users/jtuong/Documents/desktop_clinic/desktop/clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/base/BaseViewPanel.java) để [`WorkspaceManager`](file:///Users/jtuong/Documents/desktop_clinic/desktop/clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/WorkspaceManager.java) quản lý vòng đời MDI Tab (ngăn chặn mở trùng lặp, giải phóng bộ nhớ khi đóng tab).
4. **CẤM VIẾT FILE MONOLITHIC QUÁ DÀI:** Mỗi phân hệ nghiệp vụ bắt buộc phải chia nhỏ thành các thư mục con: `components/`, `dialogs/`, `menus/`, `tabs/`, `print/` và `model/`. Không gom toàn bộ hàng ngàn dòng code vào một file class duy nhất.
5. **CHUẨN TỶ LỆ 1:1 TỐI ƯU MÀN HÌNH 13-INCH:** Toàn bộ layout phải được thiết kế và kiểm thử chuẩn trên độ phân giải phổ thông của thiết bị di động y tế: Màn hình 13-inch (MacBook M1 / laptop phổ thông 1366×768 / 1440×900). Không để tràn thanh cuộn ngang ngoài ý muốn, không rớt dòng menu ribbon.
6. **TUÂN THỦ SWING EVENT DISPATCH THREAD (EDT):** Mọi tác vụ cập nhật giao diện phải chạy trên EDT. Các tác vụ tốn thời gian (truy vấn CSDL, kết nối cổng BHYT, in ấn) phải đóng gói trong `SwingWorker` để không làm đơ (freeze) giao diện bác sĩ.
7. **ĐẢM BẢO QUẢN LÝ TÀI NGUYÊN BỘ NHỚ:** Khi đóng Tab trong `WorkspaceManager`, phương thức `destroy()` của `BaseViewPanel` phải được gọi để gỡ bỏ Listeners, dọn dẹp Timer, đóng kết nối camera/máy quét mã vạch nếu có.

---

## 3. HỆ THỐNG THIẾT KẾ NỀN TẢNG (FOUNDATIONAL DESIGN SYSTEM)

### 3.1. Typography & Font Chữ Chuẩn (`AppFonts.java`)
Lớp quản trị tập trung: `vn.clinicmanagement.desktop.core.config.AppFonts`

#### A. Bảng Hằng Số Phân Cấp Typography

| Tên Hằng Số | Cỡ Chữ | Kiểu Dáng (Style) | Trường Hợp Ứng Dụng |
| :--- | :---: | :---: | :--- |
| `AppFonts.HEADER` | **18pt** | **Bold** | Tiêu đề lớn màn hình đăng nhập, logo banner hệ thống |
| `AppFonts.SECTION` | **15pt** | **Bold** | Dòng 1 banner tóm tắt thông tin bệnh nhân, tiêu đề phân hệ lớn |
| `AppFonts.BODY_BOLD` | **13pt** | **Bold** | Tiêu đề cột `JTableHeader`, tên bệnh nhân, tab active, giá trị nổi bật |
| `AppFonts.BODY` | **13pt** | Regular | Nội dung các ô bảng dữ liệu, nhãn form nhập liệu, textfield, menu |
| `AppFonts.SMALL_BOLD` | **12pt** | **Bold** | Số thứ tự hàng đợi (STT), nhãn badge trạng thái, số lượng |
| `AppFonts.SMALL` | **12pt** | Regular | Thanh trạng thái chân trang (Footer), dòng mã BN phụ, chú thích nhỏ |

#### B. Cơ Chế Fallback & Tự Động Co Giãn (Global Scaling)
* **Smart Fallback:** Hệ thống tự động phát hiện hệ điều hành để áp dụng bộ font tối ưu:
  `Segoe UI` (Mặc định chuẩn Windows) ➔ `Tahoma` (Font tiếng Việt sắc nét y tế) ➔ `Arial` (Fallback phổ thông).
* **Global Scale Factor:** Toàn bộ hệ thống có thể phóng to thu nhỏ (ví dụ hỗ trợ màn hình 4K hoặc Retina) thông qua thuộc tính duy nhất:
  ```java
  private static final float FONT_SCALE = 1.0f; // 1.15f tăng 15% kích thước font toàn hệ thống
  ```
* **Kích hoạt tự động:** Gọi `AppFonts.installGlobally()` trước khi khởi tạo bất kỳ giao diện nào trong `Main.java` để nạp cài đặt vào `UIManager`.

---

### 3.2. Bảng Màu Y Tế Chuẩn (`UIConstants.java`)
Lớp quản trị: `vn.clinicmanagement.desktop.core.config.UIConstants`

| Nhóm Màu | Tên Hằng Số | Mã Hex / RGB | Ý Nghĩa Nghiệp Vụ & Sử Dụng |
| :--- | :--- | :--- | :--- |
| **Primary Theme** | `COLOR_PRIMARY` | `#1877F2` (`24, 119, 242`) | Màu xanh nhận diện thương hiệu y tế, nút hành động chính |
| **Dark Teal** | `COLOR_TEAL_DARK` | `#1E6B89` (`30, 107, 137`) | Header các panel chuyên môn, màu chữ tiêu đề phân hệ |
| **Light Teal** | `COLOR_TEAL_LIGHT` | `#2A87A8` (`42, 135, 168`) | Thanh tiêu đề phụ, dải màu phân cách nhóm khám |
| **Active Tab** | `COLOR_TAB_ACTIVE` | `#1C66B4` (`28, 102, 180`) | Nền tab MDI đang được kích hoạt dưới đáy màn hình |
| **Selected Row** | `COLOR_ROW_SELECTED`| `#0072C6` (`0, 114, 198`) | Dòng dữ liệu đang được chọn trong danh sách bệnh nhân / dịch vụ |
| **Search Highlight**| `BG_SEARCH_FIELD` | `#FFFFD0` (`255, 255, 208`)| Nền vàng nhạt đặc trưng của các ô tìm kiếm thời gian thực (Ctrl+F) |
| **Alert / Danger** | `COLOR_ALERT` | `#8B0000` (`139, 0, 0`) | Màu đỏ sẫm cho mã bệnh án, nhãn "Yêu cầu", cảnh báo quá hạn |
| **Background Root** | `BG_ROOT` | `#EEF0F3` (`238, 240, 243`) | Màu nền khung cửa sổ ngoài cùng |
| **Toolbar BG** | `BG_TOOLBAR` | `#F3F4F6` (`243, 244, 246`) | Màu nền thanh công cụ Ribbon và thanh chức năng |
| **Panel Work BG** | `BG_PANEL` | `#F8F9FB` (`248, 249, 251`) | Màu nền vùng làm việc nội dung form |
| **Tab Strip BG** | `COLOR_TAB_BAR_BG` | `#D8E4F2` (`216, 228, 242`) | Nền thanh chứa các tab MDI chân trang chuẩn EHC |
| **Status Bar BG** | `BG_STATUS` | `#F1F3F6` (`241, 243, 246`) | Nền thanh trạng thái 9 ô chân màn hình |
| **Border Grid** | `BORDER_GRID` | `#E1E6EB` (`225, 230, 235`) | Viền kẻ lưới các bảng dữ liệu `StyledTable` |
| **Border Separator**| `BORDER_SEPARATOR` | `#C3CAD3` (`195, 202, 211`) | Viền phân chia giữa các khối panel |

---

### 3.3. Quy Chuẩn Không Gian, Lưới & Kích Thước (Grid & Layout Standards)

* **Chiều cao hàng bảng dữ liệu (`JTable`):** Chiều cao hàng chuẩn là `24px` đến `26px`. Chiều cao tiêu đề bảng `JTableHeader` là `26px` đến `28px`.
* **Kích thước nút bấm Ribbon:** Chiều cao cố định `28px`, padding trong `Insets(3, 8, 3, 8)`.
* **Kích thước thanh Tab dưới (`TabStrip`):** Chiều cao dải tab cố định `26px`. Chiều cao mỗi tab `24px`.
* **Kích thước thanh trạng thái (`StatusBar`):** Chiều cao `22px` đến `24px`, chia thành 9 ô cố định.
* **Quy tắc phân vùng (Master-Detail):** Tỷ lệ chiều rộng chuẩn trên màn hình 13-inch (1280px - 1440px):
  - Cột Hàng đợi bệnh nhân bên trái: `280px` - `310px`.
  - Vùng làm việc trung tâm: `Chiếm toàn bộ phần còn lại (flex)`.
  - Cột thông tin phụ / Lịch sử bên phải (nếu có): `260px` - `280px`.

---

### 3.4. Thư Viện Thành Phần Cốt Lõi Tái Sử Dụng (Core UI Components)

1. **`StyledTable.java` (`core.components`):**
   - Kế thừa `JTable`, cài đặt sẵn font `AppFonts.BODY`, header dùng `AppFonts.BODY_BOLD`.
   - Màu viền lưới nhạt `BORDER_GRID`, tự động căn lề theo kiểu dữ liệu (Số căn phải, Mã/STT căn giữa, Tên văn bản căn trái).
   - Hiệu ứng chọn dòng đồng bộ màu `#0072C6` với chữ trắng tương phản rõ nét.
2. **`RibbonButton.java` (`core.components`):**
   - Nút bấm thanh công cụ đỉnh hiển thị icon vector và nhãn trên cùng 1 dòng.
   - Hỗ trợ trạng thái hover (viền sáng), trạng thái active (nền gradient cam vàng nhạt `#FEE8B8` ➔ `#F9D18B` khi tab tương ứng đang mở).
   - Tích hợp sẵn `JPopupMenu` cho các nút có mũi tên mở rộng (`▾`).
3. **`BottomTab.java` (`core.components`):**
   - Tab MDI chân màn hình, có nút đóng nhanh `×`.
   - Khi tab active: Nền xanh dương đậm `#1C66B4`, chữ trắng, có mũi tên trắng tam giác chỉ lên phía đỉnh tab liên kết trực tiếp với vùng làm việc.
4. **`StatusBarPanel.java` (`core.components`):**
   - Thanh trạng thái cố định chân trang chia làm 9 ô độc lập:
     1. Bác sĩ / Người dùng đăng nhập
     2. Cơ sở KCB
     3. Mã máy / Trạm làm việc (Station ID)
     4. Tên khoa phòng
     5. Trạng thái kết nối CSDL (Xanh: Online)
     6. Trạng thái cổng BHYT 4210 (Sẵn sàng / Chưa kết nối)
     7. Phiên bản phần mềm & Ngày build
     8. Đồng hồ hệ thống thời gian thực
     9. Trạng thái phím khóa bàn phím (CAPS / NUM)
5. **`MedicalPopupChooser.java` (`modules.reception.components`):**
   - Hộp thoại chọn nhanh kiểu pop-up gắn liền với ô nhập liệu (autocomplete / dropdown search), hỗ trợ lọc danh sách dịch vụ kỹ thuật, danh mục ICD-10 và phòng khám.

---

## 4. PHÂN TÍCH CHI TIẾT TOÀN BỘ FORM & GIAO DIỆN HỆ THỐNG (FORM-BY-FORM ANALYSIS)

### 4.1. Khung Vỏ Trung Tâm & Quản Lý Tab MDI (Shell & Workspace Manager)

#### A. Màn hình Khung Vỏ Chính (`MainDashboardFrame.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.dashboard`
* **Loại màn hình:** Cửa sổ chính duy nhất của hệ thống (`JFrame`), toàn màn hình (Maximized).
* **Đối tượng sử dụng:** Mọi vai trò trong hệ thống (Tự động thích ứng giao diện theo Role).
* **Bố cục (Layout):**
  - **North (Đỉnh):** Thanh Ribbon Menu thích ứng:
    + *Chế độ Bác sĩ (Doctor Mode):* Menu chuyên sâu lâm sàng (Menu ▾, In ▾, Làm mới, Tìm kiếm bệnh nhân, Dịch vụ kỹ thuật ▾, Thuốc & Vật tư ▾, Hoàn tất khám, Tạm hoãn, Kiosk tiếp đón).
    + *Chế độ Quản trị (Admin Mode):* Toàn bộ 12 phân hệ tổng thể (Trang Chủ, Quản Lý, Đón Tiếp, Khám Bệnh, Viện Phí, CĐHA & Thăm Dò, Nhà Thuốc, Báo Cáo, Hệ Thống, Danh Mục, Người Dùng, Kết Thúc).
  - **Center (Trung tâm):** Vùng chứa `CardLayout` quản lý bởi `WorkspaceManager`.
  - **South (Chân trang):** Dải Tab MDI (`BottomTab`) và Thanh trạng thái 9 ô (`StatusBarPanel`).
* **UX Tương Tác:**
  - Chuyển đổi tab tức thì không giật lag.
  - Tự động đồng bộ trạng thái nút Ribbon khi người dùng nhấp chọn tab dưới đáy hoặc đóng tab.

#### B. Bộ Điều Phối Vòng Đời Tab (`WorkspaceManager.java`)
* **Cơ chế Singleton View:** Mỗi `BaseViewPanel` có một định danh duy nhất (`viewKey`). Nếu người dùng bấm mở một phân hệ đã mở trước đó, hệ thống sẽ tự động kích hoạt tab đó mà không tạo thêm instance mới, giúp tiết kiệm bộ nhớ RAM.
* **Vòng đời Tab:**
  - `openView(BaseViewPanel view)`: Kiểm tra tồn tại, tạo tab mới hoặc chuyển card view.
  - `activateTab(String viewKey)`: Đánh dấu active, kích hoạt sự kiện `view.onShow()`.
  - `closeTab(String viewKey)`: Gỡ bỏ khỏi CardLayout, xóa tab button, gọi `view.destroy()` để thu hồi bộ nhớ.

---

### 4.2. Phân Hệ Đăng Nhập & Điều Phối Phiên (Authentication & Session)

#### Form Đăng Nhập (`LoginForm.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.auth`
* **Loại giao diện:** Cửa sổ Modal độc lập (`JFrame`, không thể resize, kích thước cố định `440x390px`, căn giữa màn hình).
* **Bố cục:**
  - **Header:** Logo y tế EHC, Tiêu đề phần mềm (Font `AppFonts.HEADER` 18pt Bold), Phụ đề phiên bản.
  - **Center:** Form nhập liệu:
    + Ô Tên đăng nhập (`txtUsername`)
    + Ô Mật khẩu (`txtPassword`)
    + Hộp chọn Vai trò / Phân quyền (`cbRole`): Bác sĩ điều trị, Tiếp đón bệnh nhân, Dược sĩ nhà thuốc, Thu ngân viện phí, Quản trị hệ thống.
    + Hộp chọn Cơ sở / Phòng khám làm việc: Phân quyền theo chi nhánh bệnh viện.
  - **Footer:** Nút "Đăng Nhập" (Màu primary `#1877F2`, phím Enter kích hoạt) và Nút "Thoát".
* **Xử lý An toàn & UX:**
  - Tự động ghi nhớ tài khoản gần nhất.
  - Chặn brute-force đăng nhập sai quá 5 lần.
  - Phân luồng đăng nhập tự động: Nếu chọn vai trò Bác sĩ ➔ Tự động mở thẳng `DoctorStationPanel` làm việc ngay lập tức.

---

### 4.3. Phân Hệ Bàn Khám Bác Sĩ & Điều Trị Lâm Sàng (`modules/doctor/`)

Đây là màn hình quan trọng nhất của toàn bộ hệ thống, nơi Bác sĩ thực hiện toàn bộ nghiệp vụ chẩn đoán, điều trị và kê đơn.

```
+----------------------------------------------------------------------------------------------------+
| [MENU ▾] [IN ▾] [LÀM MỚI] [GỌI SỐ (F12)] [BỎ QUA] [CHỈ ĐỊNH (F3)] [KÊ ĐƠN (F4)] [HOÀN TẤT (F2)]     | (DoctorActionBar)
+-----------------------+----------------------------------------------------+-----------------------+
| HÀNG ĐỢI BỆNH NHÂN     | KHUNG THÔNG TIN HÀNH CHÍNH & LÂM SÀNG               | LỊCH SỬ KHÁM BỆNH     |
| [Tất cả] [Chờ] [Đã khám]| Mã BN: 18039327 | Tên: TEST BN 033 | Nam | 23T      | Các đợt KCB trước     |
|                       | Thẻ BHYT: GD-4-21-21-212-12125 | Hạn: 31/12/2018   | - 12/08/2018: Viêm tai|
| 01. TEST BN 060 (Chờ) |----------------------------------------------------| - 05/03/2018: Cảm cúm |
| >02. TEST BN 033 (Khám)| Sinh hiệu: Mạch: 80 | HA: 120/80 | SpO2: 98% | Nhiệt: 37|-----------------------|
| 03. TEST BN 032 (Đã)  | Chẩn đoán ICD-10: D21 - Bướu lành mô mềm          | KẾT QUẢ CẬN LÂM SÀNG  |
| 04. TEST BN 031 (Chờ) | Diễn tiến: Bệnh nhân tỉnh, tiếp xúc tốt, đau nhẹ...| - X-Quang ngực: Bình th|
|                       | Xử trí: Đơn thuốc ngoại trú + Hẹn tái khám 7 ngày  | - CTM: Bạch cầu bình th|
+-----------------------+----------------------------------------------------+-----------------------+
```

#### A. Bàn Khám Bác Sĩ Chính (`DoctorStationPanel.java`)
* **Kế thừa:** `BaseViewPanel` (`treatmentKey = "treatment"`, Tên Tab: "Điều Trị").
* **Bố cục tổng thể:** Chia 3 cột:
  - Cột 1 (Trái - `DoctorQueueSidebar`): Độ rộng cố định `280px`.
  - Cột 2 (Giữa - `DoctorClinicalForm`): Độ rộng tự co giãn chiếm ưu tiên lớn nhất.
  - Cột 3 (Phải - `DoctorHistoryPanel`): Độ rộng `280px` hiển thị lịch sử và kết quả CLS.
  - Đỉnh: Thanh hành động lâm sàng `DoctorActionBar`.

#### B. Thanh Hành Động Bác Sĩ (`DoctorActionBar.java`)
* **Chiều cao:** `32px`, nền xám nhạt `BG_TOOLBAR`.
* **Hệ thống nút bấm vector:**
  - Nút **"Gọi Số Tiếp Theo" (F12):** Tự động phát âm thanh loa thông báo phòng khám và chuyển trạng thái bệnh nhân kế tiếp sang "Đang khám".
  - Nút **"Bỏ Qua Lượt":** Đưa bệnh nhân vắng mặt về cuối danh sách chờ.
  - Nút **"Chỉ Định CLS" (F3):** Mở popup chỉ định cận lâm sàng nhanh.
  - Nút **"Kê Đơn Thuốc" (F4):** Mở tab/popup kê đơn ngoại trú.
  - Nút **"Hoàn Tất Khám" (F2):** Khóa hồ sơ lượt khám hiện tại, chuyển sang trạng thái đã hoàn tất.
  - Nút **"In Hồ Sơ" (Ctrl+P):** Mở menu chọn in: Đơn thuốc, Phiếu hẹn, Giấy chuyển viện.

#### C. Cột Danh Sách Chờ Khám (`DoctorQueueSidebar.java`)
* **Thành phần:**
  - Bộ lọc trạng thái: Nút chọn nhanh (Tất cả, Đang chờ khám, Chờ kết quả CLS, Đã khám xong).
  - Lưới hiển thị bệnh nhân: Hiển thị STT, Họ và tên (In đậm), Năm sinh/Tuổi, Giới tính, Loại khám (BHYT / Viện phí).
  - Tự động làm mới danh sách (Polling qua Worker mỗi 15-30 giây hoặc qua Event Bus).
  - Nhấp chuột chọn bệnh nhân ➔ Cập nhật toàn bộ dữ liệu ở Cột giữa và Cột phải.

#### D. Form Lâm Sàng Trung Tâm (`DoctorClinicalForm.java`)
* **Layout:** `GridBagLayout` nén chặt, không gian giữa các thành phần `2px - 4px`.
* **Phân vùng chi tiết:**
  1. *Khung Hành Chính & Thẻ BHYT:*
     - Mã BN, Tên bệnh nhân (Chữ hoa đậm), Giới tính, Ngày sinh, Nghề nghiệp.
     - Số thẻ BHYT (Mã 15 ký tự), Mức hưởng (80%, 95%, 100%), Tuyến KCB (Đúng tuyến / Trái tuyến), Hạn sử dụng thẻ.
  2. *Khung Khám Lâm Sàng & Sinh Hiệu:*
     - Các chỉ số: Mạch (lần/phút), Huyết áp (mmHg), Nhiệt độ (°C), Nhịp thở (lần/phút), SpO2 (%), Chiều cao (cm), Cân nặng (kg).
     - Tự động tính chỉ số khối cơ thể (BMI) và phân loại thể trạng (Bình thường / Thừa cân / Suy dinh dưỡng).
     - Lý do đến khám & Bệnh sử, Tiền sử dị ứng thuốc (Chữ đỏ cảnh báo).
  3. *Khung Chẩn Đoán & Mã Bệnh (ICD-10):*
     - Bệnh chính: Mã ICD-10 và tên bệnh theo chuẩn Bộ Y Tế.
     - Bệnh phụ kèm theo: Cho phép chọn nhiều mã ICD-10 phụ.
     - Mô tả chẩn đoán lâm sàng chi tiết của bác sĩ.
  4. *Khung Xử Trí Điều Trị:*
     - Hướng điều trị: Ngoại trú / Nhập viện / Chuyển viện / Cho về.
     - Lời dặn của bác sĩ và số ngày hẹn tái khám.

#### E. Khung Lịch Sử & Cận Lâm Sàng (`DoctorHistoryPanel.java`)
* **Bố cục 2 phần dạng dọc:**
  - Nửa trên: Cây hoặc danh sách các đợt KCB trong quá khứ của bệnh nhân (kèm ngày khám và chẩn đoán).
  - Nửa dưới: Bảng kết quả cận lâm sàng của ngày hôm nay (Xét nghiệm máu, Nước tiểu, X-quang, Siêu âm) với các giá trị bất thường được tô màu cam/đỏ.

---

### 4.4. Phân Hệ Khám Bệnh - Quản Trị & Điều Phối Hàng Đợi (`modules/examination/`)

#### Màn hình Khám Bệnh Quản Trị (`ExaminationPanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.examination`
* **Mục đích:** Màn hình điều phối tổng thể phân hệ khám bệnh dành cho Admin hoặc Bác sĩ trưởng khoa.
* **Tối ưu chuẩn 13-inch:**
  - Banner góc phải trên: Tóm tắt thông tin bệnh nhân đang được chọn (`NGUYỄN MẠNH HÙNG | 21000325 | 1984 (39 tuổi) | Nam | Yêu cầu`).
  - Cột bên trái: Lưới hàng đợi 11 dòng bệnh nhân (`PatientQueueTablePanel`), tối ưu chiều cao đúng chuẩn không bị mất dòng trên màn hình MacBook M1.
  - Phân trang hàng đợi (`PatientQueuePaginationPanel`): Hiển thị tổng số bệnh nhân, tỷ lệ BHYT / Viện phí, nút chuyển trang.
  - Vùng bên phải: Hệ thống 8 Tab chuyên môn sâu.

---

### 4.5. Hệ Thống 8 Tab Lâm Sàng Chuyên Sâu Khám Bệnh

Nằm trong package `vn.clinicmanagement.desktop.modules.examination.components.tabs`:

| Tên Lớp Tab Component | Chức Năng Nghiệp Vụ Y Tế Chi Tiết | Các Trường Dữ Liệu & Thao Tác Chính |
| :--- | :--- | :--- |
| **`MedicalRecordTabPanel`** | Bệnh án ngoại trú & Diễn tiến điều trị | Lý do vào viện, Quá trình bệnh lý, Tiền sử bản thân/gia đình, Khám toàn thân, Khám bộ phận, Hướng điều trị. |
| **`PrescriptionOrderTabPanel`** / **`PrescriptionOrderPanel`** | Kê đơn thuốc ngoại trú cho bệnh nhân | Lưới đơn thuốc: Tên thuốc, Hoạt chất, Đơn vị tính, Số lượng, Cách dùng (Sáng-Trưa-Chiều-Tối), Số ngày, Tồn kho thực tế. |
| **`LabOrderTabPanel`** | Chỉ định Xét nghiệm & Thăm dò chức năng | Cây danh mục xét nghiệm (Huyết học, Sinh hóa, Vi sinh), Trạng thái lấy mẫu, Thời gian trả kết quả, Nơi thực hiện. |
| **`SuppliesOrderTabPanel`** | Kê vật tư y tế tiêu hao phòng khám | Bơm kim tiêm, Băng gạc, Dây truyền dịch, Găng tay y tế, Nẹp định hình. |
| **`TreatmentResolutionTabPanel`** | Xử trí kết thúc đợt khám & Ra viện | Kết luận đợt điều trị (Khỏi, Giảm, Không thay đổi, Nặng hơn, Tử vong), Hẹn tái khám, Cấp giấy nghỉ ốm hưởng BHXH. |

---

### 4.6. Hệ Thống Hộp Thoại Nghiệp Vụ (Clinical Dialogs)

Nằm trong package `vn.clinicmanagement.desktop.modules.examination.components.dialogs`:

1. **`IcdCatalogSelectionDialog` & `IcdCatalogPanel`:**
   - Hộp thoại tra cứu danh mục bệnh tật quốc tế ICD-10.
   - Tìm kiếm nhanh đa năng: Tìm theo Mã ICD (ví dụ `J00`, `E11`) hoặc Tìm theo Tên bệnh tiếng Việt không dấu/có dấu.
   - Hiển thị cây phân cấp: Chương bệnh ➔ Nhóm bệnh ➔ Tên bệnh chi tiết.
2. **`ServiceOrderDialog` & `ServiceOrderPanel`:**
   - Hộp thoại chọn và chỉ định dịch vụ kỹ thuật cận lâm sàng.
   - Tích hợp kiểm tra giới hạn chỉ định của BHYT theo thông tư BYT, tự động tính tổng tiền dịch vụ theo giá viện phí hoặc giá bảo hiểm.
3. **`MedicalSuppliesOrderDialog` & `MedicalSuppliesOrderPanel`:**
   - Hộp thoại kê vật tư tiêu hao gắn liền với dịch vụ thủ thuật.
4. **`TransferRoomSelectDialog` & `TransferRoomConfirmDialog`:**
   - Điều chuyển bệnh nhân sang phòng khám chuyên khoa khác (ví dụ: Từ phòng Khám Nội sang phòng Khám Răng Hàm Mặt).
   - Cơ chế xác nhận bảo lưu thông tin đã khám ban đầu.
5. **`SecondaryDiagnosisDialog` & `SecondaryDiagnosisPanel`:**
   - Quản lý danh sách các chẩn đoán phụ đi kèm của bệnh nhân nhiều bệnh lý nền.
6. **`ReopenMedicalRecordDialog` & `ReopenMedicalRecordPanel`:**
   - Quy trình kiểm soát mở lại hồ sơ bệnh án đã kết thúc và đã khóa.
   - Bắt buộc ghi nhận lý do mở lại và lưu vết kiểm toán (Audit Trail) phục vụ giám định BHYT.
7. **`DischargeSummaryDialog` & `DischargeSummaryPanel`:**
   - Lập tóm tắt hồ sơ bệnh án ra viện cho bệnh nhân kết thúc đợt điều trị ngoại trú dài ngày.
8. **`MedicalRecordTypeDialog` & `MedicalRecordTypePanel`:**
   - Lựa chọn loại mẫu bệnh án chuyên khoa phù hợp (Bệnh án Ngoại trú chung, Mắt, Răng Hàm Mặt, Tai Mũi Họng, Nhi khoa).

---

### 4.7. Hệ Thống In Ấn & Xem Trước Phiếu Y Tế (Print Preview System)

Nằm trong package `vn.clinicmanagement.desktop.modules.examination.components.print`:

* **`PrescriptionPrintPreviewDialog` & `PrescriptionPrintPreviewPanel`:**
  - Xem trước và in Đơn thuốc ngoại trú chuẩn theo Thông tư 52/2017/TT-BYT và Thông tư 18/2018/TT-BYT.
  - Hỗ trợ khổ giấy A4 (in 2 đơn/tờ) hoặc A5 (in 1 đơn/tờ). Hiển thị mã vạch đơn thuốc (Barcode 128).
* **`ClinicalOrderPrintPreviewDialog` & `ClinicalOrderPrintPreviewPanel`:**
  - Phiếu chỉ định cận lâm sàng phục vụ bệnh nhân cầm đi các phòng chức năng làm xét nghiệm/siêu âm.
* **`MedicalBillingSlipDialog` & `MedicalBillingSlipPanel`:**
  - Bảng kê chi phí khám bệnh chữa bệnh ngoại trú (Mẫu số 01/BV chuẩn Bộ Y Tế và Bảo hiểm Xã hội).
* **`FollowUpAppointmentPrintDialog` & `FollowUpAppointmentPrintPanel`:**
  - Giấy hẹn khám lại với ngày hẹn cụ thể, bác sĩ hẹn và lời dặn dò.
* **`OutpatientMedicalRecordPrintDialog` & `OutpatientMedicalRecordPrintPanel`:**
  - Trích sao hồ sơ bệnh án ngoại trú phục vụ chuyển tuyến hoặc lưu trữ hành chính.
* **`TransferSlipPrintPreviewDialog`:**
  - Giấy chuyển tuyến khám bệnh, chữa bệnh BHYT (Mẫu số 06/BHYT).

---

### 4.8. Hệ Thống Trình Đơn & Menu Ngữ Cảnh (Context Menus & Popups)

Nằm trong package `vn.clinicmanagement.desktop.modules.examination.components.menus`:

1. **`PatientQueueContextMenu`:**
   - Kích hoạt khi nhấp chuột phải vào một hàng trong danh sách chờ:
     + *Gọi bệnh nhân vào phòng khám*
     + *Đánh dấu diện ưu tiên (Trẻ em dưới 6 tuổi, Người già > 75 tuổi, Cấp cứu)*
     + *Chuyển phòng khám khác*
     + *Hủy lượt khám (kèm xác nhận)*
2. **`ServiceContextMenu`:**
   - Chuột phải trên danh sách dịch vụ chỉ định: Xóa dịch vụ, Thay đổi số lượng, Xem kết quả nhanh, Đổi bác sĩ chỉ định.
3. **`MedicineSuppliesPopupMenu`:**
   - Chuột phải trên dòng thuốc: Tra cứu tồn kho chi tiết các quầy thuốc, Tra cứu tương tác thuốc, Đổi thuốc biệt dược tương đương hoạt chất.
4. **`PrintReportPopupMenu`:**
   - Menu xổ xuống từ nút "In ▾": Danh sách các biểu mẫu in ấn nhanh cho bác sĩ.
5. **`ExaminationMenuPopupMenu`:**
   - Menu tiện ích mở rộng: Tra cứu lịch sử thẻ BHYT trên cổng giám định, Thiết lập thông số mặc định của phòng khám.

---

### 4.9. Phân Hệ Đón Tiếp & Đăng Ký Khám Bệnh (`modules/reception/`)

#### A. Màn hình Tiếp Đón Trung Tâm (`ReceptionPanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.reception`
* **Kế thừa:** `BaseViewPanel` (`viewKey = "reception"`, Tab: "Đăng Ký Khám Bệnh").
* **Bố cục chính:**
  - **North:** `ReceptionTopToolbar`:
    + Nút menu hệ thống, In phiếu STT, Làm mới dữ liệu.
    + Nút "Bệnh Nhân Mới" (F1): Reset sạch form để nhập thông tin ca mới.
    + Nút "Quét CCCD gắn chip / Thẻ BHYT" (F4): Tự động điền thông tin hành chính qua đầu đọc QR/mã vạch.
    + Nút "Gọi Số Tiếp Theo" (F12) liên kết loa màn hình hiển thị tại cửa đón tiếp.
  - **Center (`JSplitPane` Dọc, vị trí chia `218px`):**
    + *Nửa trên (`PatientRegistrationFormPanel`):* Form hành chính, liên lạc, thông tin thẻ BHYT 15 ký tự, chọn phòng khám, lý do khám.
    + *Nửa dưới (`ClinicRoomStatusPanel`):* Bảng 3 cột theo dõi trạng thái tải thực tế của từng phòng khám (Phòng khám, Bác sĩ trực, Số lượng đang chờ, Đã khám).
  - **South (`ReceptionFooterBar`):** Dải phím tắt chân trang: `F1-Mới | F2-Lưu | F3-In Phiếu | F4-Thẻ KCB | F12-Gọi Số`.

#### B. Trạng Thái Phòng Khám & Cảnh Báo Tải (`ClinicRoomStatusPanel.java`)
* Hiển thị danh sách 17 phòng khám chuyên khoa (PK Nhi, Cấp cứu, Khám Nội, Tai Mũi Họng, v.v.).
* Khi nhân viên tiếp đón chọn phòng khám ở form trên ➔ Dòng phòng khám đó ở bảng dưới tự động highlight màu đỏ cam để kiểm tra số lượng chờ, tránh dồn bệnh nhân vào phòng đang quá tải.

---

### 4.10. Phân Hệ Quản Lý Viện Phí & Hóa Đơn (`modules/billing/`)

#### Màn hình Thu Ngân & Viện Phí (`BillingInvoicePanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.billing`
* **Kế thừa:** `BaseViewPanel` (`viewKey = "billing"`, Tab: "Quản Lý Viện Phí").
* **Bố cục:**
  - **Khung tìm kiếm & Bộ lọc:** Tìm theo Mã bệnh nhân, Họ tên, Số hóa đơn, Trạng thái (Chờ thanh toán, Đã thanh toán, Tạm ứng, Hoàn ứng).
  - **Bảng danh sách phiếu thu (Master Table):** Hiển thị danh sách hóa đơn theo ngày.
  - **Bảng chi tiết các khoản chi phí (Detail Table):** Liệt kê chi tiết từng mục: Tiền khám, Tiền xét nghiệm, Tiền thuốc, Tiền vật tư.
  - **Bảng tổng kết tài chính:**
    + Tổng chi phí điều trị
    + Quỹ BHYT chi trả (theo % mức hưởng)
    + Bệnh nhân cùng chi trả
    + Số tiền tạm ứng đã nộp
    + Số tiền thực thu / hoàn lại cho người bệnh
  - **Hành động:** Nút "Thu Tiền & In Hóa Đơn" (Tích hợp xuất hóa đơn điện tử VAT).

---

### 4.11. Phân Hệ Kho Dược & Cấp Phát Thuốc (`modules/pharmacy/`)

#### Màn hình Tủ Trực Vật Tư & Cấp Phát Thuốc (`PharmacyDispensePanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.pharmacy`
* **Kế thừa:** `BaseViewPanel` (`viewKey = "pharmacy"`, Tab: "Tủ Trực Vật Tư").
* **Bố cục Master-Detail (`JSplitPane` Ngang, vị trí chia `215px`):**
  - **Cột Trái (`PharmacyCategoryTreePanel`):** Cây danh mục kho dược:
    + Kho tổng chẵn
    + Kho thuốc ngoại trú
    + Kho thuốc nội trú
    + Tủ trực cấp cứu các khoa
  - **Cột Phải (`PharmacyTicketTablePanel`):** Bảng danh sách phiếu xuất/nhập/cấp phát thuốc:
    + Quản lý theo nguyên tắc **FEFO (First Expired, First Out - Hạn gần xuất trước)**.
    + Cảnh báo màu vàng cho thuốc cận hạn dùng (dưới 6 tháng), màu đỏ cho thuốc hết hạn hoặc hết tồn kho an toàn.
  - **Thanh công cụ phụ (`PharmacySubToolbar`):** Xuất kho theo đơn, Trả lại kho, Kiểm kê tồn, In thẻ kho.

---

### 4.12. Phân Hệ Cận Lâm Sàng & Chẩn Đoán Hình Ảnh (`modules/paraclinical/`)

#### Màn hình CĐHA & Thăm Dò Chức Năng (`ParaclinicalOrderPanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.paraclinical`
* **Mục đích:** Tiếp nhận chỉ định từ các phòng khám, thực hiện kỹ thuật và nhập kết quả chuyên môn.
* **Quy trình:**
  1. Tiếp nhận bệnh nhân theo danh sách chờ cận lâm sàng (X-quang, Siêu âm, Điện tim, Nội soi).
  2. Bác sĩ chuyên khoa chẩn đoán hình ảnh nhập kết luận mô tả và hình ảnh mẫu.
  3. Duyệt kết quả (Ký số) ➔ Tự động đồng bộ kết quả về màn hình của Bác sĩ điều trị (`DoctorStationPanel`).

---

### 4.13. Phân Hệ Quản Lý Danh Mục Dùng Chung (`modules/catalog/`)

#### Màn hình Quản Lý Danh Mục (`CatalogPanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.catalog`
* **Mục đích:** Quản lý toàn bộ danh mục tham chiếu cốt lõi của hệ thống y tế:
  - Danh mục ICD-10 Bộ Y Tế ban hành.
  - Danh mục Thuốc & Hoạt chất dùng cho bảo hiểm y tế.
  - Danh mục Dịch vụ kỹ thuật tương đương và bảng giá viện phí.
  - Danh mục Cơ sở khám chữa bệnh ban đầu toàn quốc.
  - Danh mục Khoa/Phòng/Giường bệnh nội viện.

---

### 4.14. Phân Hệ Báo Cáo Thống Kê (`modules/reports/`)

#### Màn hình Báo Cáo Thống Kê (`ClinicReportPanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.reports`
* **Mục đích:** Cung cấp số liệu phân tích hoạt động khám chữa bệnh và kết xuất dữ liệu báo cáo:
  - Báo cáo tổng hợp lượt khám bệnh theo ngày / theo bác sĩ / theo chuyên khoa.
  - Báo cáo doanh thu viện phí và tình hình thu - chi viện phí.
  - Báo cáo cơ cấu bệnh tật theo mã chương bệnh ICD-10.
  - Báo cáo quyết toán BHYT chuẩn XML 1, 2, 3, 4, 5 gửi Cổng tiếp nhận giám định BHXH Việt Nam.
  - Chức năng xuất dữ liệu ra file Excel chuẩn định dạng biểu mẫu kế toán.

---

### 4.15. Phân Hệ Quản Trị Hệ Thống & Phân Quyền (`modules/system/`)

#### Màn hình Quản Trị Hệ Thống (`SystemConfigPanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.system`
* **Bố cục:**
  - Thanh tìm kiếm thời gian thực nhanh với nền màu vàng `#FFFFD0` (kích hoạt bằng phím `Ctrl+F`).
  - Danh mục cây cài đặt bên trái: Quản lý người dùng, Vai trò & Phân quyền (RBAC), Cấu hình thông số phòng khám, Cấu hình máy in nhiệt & máy in A4/A5, Nhật ký kiểm toán hệ thống (Audit Logs).
  - Lưới cấu hình tham số chi tiết bên phải hỗ trợ sắp xếp và lọc tức thì (`TableRowSorter`).

---

### 4.16. Trang Chủ & Bảng Tin Trung Tâm (`modules/home/`)

#### Màn hình Chào & Tổng Quan (`HomeWelcomePanel.java`)
* **Package:** `vn.clinicmanagement.desktop.modules.home`
* **Mục đích:** Tab mặc định hiển thị khi khởi động ứng dụng:
  - Biểu đồ tóm tắt số lượt tiếp đón, số lượt đang khám, số ca đã hoàn tất trong ngày.
  - Lối tắt truy cập nhanh (Quick Action Cards) đến các nghiệp vụ chính theo quyền hạn của tài khoản đăng nhập.
  - Thông báo nội bộ bệnh viện và lịch trực của nhân viên y tế.

---

## 5. CHUẨN HÓA TRẢI NGHIỆM NGƯỜI DÙNG Y TẾ (MEDICAL UX STANDARDS)

### 5.1. Bản Đồ Phím Tắt Toàn Diện (Keyboard-First Navigation Map)

Mọi màn hình trong hệ thống bắt buộc phải hỗ trợ hệ thống phím tắt sau:

| Phím Tắt | Chức Năng Nghiệp Vụ Chuẩn Toàn Hệ Thống |
| :--- | :--- |
| **`F1`** | **Tạo Mới:** Tạo lượt tiếp đón mới, thêm đơn thuốc mới, thêm dòng chỉ định mới |
| **`F2`** | **Lưu / Hoàn Tất:** Lưu thông tin bệnh nhân, lưu đơn thuốc, hoàn tất lượt khám bệnh |
| **`F3`** | **Chỉ Định / In Phiếu:** Mở hộp thoại chỉ định dịch vụ kỹ thuật hoặc in phiếu thu |
| **`F4`** | **Kê Đơn / Quét Thẻ:** Mở tab kê đơn thuốc (Bác sĩ) hoặc quét mã thẻ BHYT (Tiếp đón) |
| **`F5`** | **Làm Mới (Refresh):** Tải lại toàn bộ dữ liệu danh sách hàng đợi từ máy chủ |
| **`F12`** | **Gọi Số Loa:** Kích hoạt hệ thống âm thanh gọi số thứ tự bệnh nhân tiếp theo |
| **`Enter`** | **Chuyển Trường / Xác Nhận:** Tự động nhảy focus sang ô nhập liệu kế tiếp trong form |
| **`Esc`** | **Đóng / Hủy:** Đóng popup, thoát hộp thoại xem trước in, hủy thao tác hiện tại |
| **`Ctrl + F`** | **Tìm Kiếm Nhanh:** Nhảy con trỏ chuột vào ô tìm kiếm bệnh nhân / danh mục có nền vàng |
| **`Ctrl + P`** | **In Ấn:** Mở hộp thoại xem trước và in ấn phiếu y tế tương ứng |
| **`Ctrl + S`** | **Lưu Nhanh:** Lưu tạm thời hồ sơ bệnh án đang chỉnh sửa |
| **`Ctrl + W`** | **Đóng Tab:** Đóng tab MDI nghiệp vụ đang kích hoạt hiện tại |

---

### 5.2. Cơ Chế Phòng Chống Sai Sót Y Khoa (Medical Error Prevention)

1. **Cảnh Báo Tương Tác Thuốc:** Khi Bác sĩ thêm một loại thuốc có chống chỉ định hoặc tương tác nguy hiểm với thuốc đã kê trước đó, hệ thống phải chặn lại hoặc bật hộp thoại cảnh báo cấp độ đỏ, yêu cầu Bác sĩ xác nhận lý do lâm sàng.
2. **Kiểm Tra Trùng Đơn & Trùng Dịch Vụ:** Hệ thống tự động kiểm tra xem bệnh nhân đã được chỉ định dịch vụ tương tự trong vòng 24 giờ qua chưa để tránh lãng phí hoặc bị BHYT từ chối thanh toán.
3. **Kiểm Tra Thẻ BHYT Thời Gian Thực:** Khi quét thẻ, tự động kiểm tra định dạng 15 ký tự, thời hạn sử dụng thẻ (`Từ ngày ... đến ngày ...`), và mã nơi đăng ký KCB ban đầu. Nếu thẻ hết hạn, hiển thị chữ màu đỏ và gợi ý chuyển sang diện Viện phí.
4. **Xác Nhận Hành Động Nguy Hiểm:** Mọi thao tác hủy phiếu thu, xóa dịch vụ đã thực hiện kỹ thuật, mở lại hồ sơ bệnh án đã khóa đều bắt buộc phải qua hộp thoại xác nhận có mật khẩu hoặc tài khoản Trưởng khoa/Admin phê duyệt.

---

### 5.3. Quy Chuẩn Phản Hồi Trạng Thái & Độ Trễ (Feedback & Latency)

* **Thời gian phản hồi tương tác (Click Response):** Phải dưới `50ms`. Người dùng nhấp chuột hoặc bấm phím tắt phải thấy ngay hiệu ứng thay đổi trạng thái nút hoặc con trỏ chuột `WaitCursor`.
* **Tác vụ nền bất đồng bộ (Background Tasks):** Không bao giờ khóa giao diện người dùng (EDT). Dùng thanh tiến trình `JProgressBar` hoặc biểu tượng xoay indeterminate cho các tác vụ tải dữ liệu lớn, cho phép người dùng bấm hủy nếu muốn.
* **Thông báo Toast & Alert:** Không lạm dụng `JOptionPane` chặn màn hình cho các thông tin không quan trọng; ưu tiên hiển thị thông báo trạng thái tại góc dưới bên phải hoặc trên thanh `StatusBarPanel`.

---

## 6. CẤU TRÚC THƯ MỤC & TỔ CHỨC MÃ NGUỒN

Dự án tuân thủ nghiêm ngặt mô hình kiến trúc phân tầng (Clean Architecture & Modular Monolith):

```
clinic-desktop/src/main/java/vn/clinicmanagement/desktop/
│
├── core/                              # NỀN TẢNG CỐT LÕI (CORE FOUNDATION)
│   ├── base/
│   │   └── BaseViewPanel.java         # Lớp cơ sở cho MỌI màn hình phân hệ
│   ├── config/
│   │   ├── AppFonts.java              # Nguồn font chữ DUY NHẤT toàn hệ thống
│   │   └── UIConstants.java           # Bảng màu, border, kích thước chuẩn
│   ├── components/                    # Các Component giao diện tái sử dụng
│   │   ├── BottomTab.java             # Tab MDI chân trang có nút đóng '×' và mũi tên active
│   │   ├── RibbonButton.java          # Nút menu Ribbon 1 dòng chuẩn vector icon
│   │   ├── StatusBarPanel.java        # Thanh trạng thái chân màn hình 9 ô chuẩn
│   │   └── StyledTable.java           # JTable chuẩn viền lưới và màu chọn y tế
│   └── session/
│       └── UserSession.java           # Quản lý phiên làm việc, Bác sĩ, Cơ sở, Mã trạm
│
└── modules/                           # CÁC PHÂN HỆ NGHIỆP VỤ ĐỘC LẬP
    ├── auth/                          # Đăng nhập & lựa chọn khoa phòng (LoginForm)
    ├── dashboard/                     # Vỏ khung chính (MainDashboardFrame & WorkspaceManager)
    ├── home/                          # Bảng điều khiển trang chủ (HomeWelcomePanel)
    ├── doctor/                        # PHÂN HỆ BÁC SĨ ĐIỀU TRỊ (Doctor Station)
    │   ├── DoctorStationPanel.java    # Bàn khám chính
    │   ├── components/                # ActionBar, QueueSidebar, ClinicalForm, HistoryPanel
    │   └── model/                     # DTO dữ liệu lâm sàng (PatientExamDto)
    ├── examination/                   # PHÂN HỆ KHÁM BỆNH QUẢN TRỊ (Examination Station)
    │   ├── ExaminationPanel.java      # Giao diện khám bệnh tổng thể
    │   ├── components/
    │   │   ├── banner/                # PatientSummaryBannerPanel
    │   │   ├── queue/                 # PatientQueueTablePanel, Pagination
    │   │   ├── tabs/                  # 8 Tab: MedicalRecord, Prescription, LabOrder, Supplies...
    │   │   ├── dialogs/               # IcdCatalog, ServiceOrder, TransferRoom, DischargeSummary...
    │   │   ├── print/                 # PrescriptionPrint, BillingSlip, ClinicalOrder...
    │   │   └── menus/                 # PatientQueueContextMenu, ServiceContextMenu...
    ├── reception/                     # PHÂN HỆ TIẾP ĐÓN & ĐĂNG KÝ KHÁM
    │   ├── ReceptionPanel.java        # Màn hình tiếp đón trung tâm
    │   └── components/                # RegistrationForm, ClinicRoomStatus, Chooser, FooterBar
    ├── billing/                       # PHÂN HỆ VIỆN PHÍ & THU NGÂN
    │   └── BillingInvoicePanel.java   # Quản lý hóa đơn viện phí
    ├── pharmacy/                      # PHÂN HỆ KHO DƯỢC & CẤP PHÁT THUỐC
    │   ├── PharmacyDispensePanel.java # Cấp phát thuốc & Tủ trực
    │   └── components/                # CategoryTree, TicketTable, SubToolbar
    ├── paraclinical/                  # PHÂN HỆ CẬN LÂM SÀNG & CĐHA (ParaclinicalOrderPanel)
    ├── catalog/                       # PHÂN HỆ DANH MỤC DÙNG CHUNG (CatalogPanel)
    ├── reports/                       # PHÂN HỆ BÁO CÁO THỐNG KÊ (ClinicReportPanel)
    └── system/                        # PHÂN HỆ QUẢN TRỊ HỆ THỐNG (SystemConfigPanel)
```

---

## 7. QUY TRÌNH PHÁT TRIỂN FORM MỚI (DEVELOPER CHECKLIST & BOILERPLATE)

Khi nhận yêu cầu xây dựng một màn hình/phân hệ mới, lập trình viên bắt buộc tuân theo quy trình 4 bước chuẩn sau:

### Bước 1: Xác Định Đúng Package Chức Năng
* Không đặt file vào thư mục gốc hoặc sai package nghiệp vụ.
* Tạo cấu trúc package con nếu màn hình có nhiều hơn 3 thành phần con (`components/`, `dialogs/`, `model/`).

### Bước 2: Kế Thừa Chuẩn `BaseViewPanel`
Mọi form nghiệp vụ phải kế thừa `BaseViewPanel` theo mẫu boilerplate sau:

```java
package vn.clinicmanagement.desktop.modules.myfeature;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.config.UIConstants;

import javax.swing.*;
import java.awt.*;

public class MyFeaturePanel extends BaseViewPanel {

    public MyFeaturePanel() {
        super("my-feature-key", "Tên Phân Hệ Mới", "icon_key");
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Áp dụng bảng màu và font chuẩn
        // Tuyệt đối dùng AppFonts.BODY, AppFonts.BODY_BOLD, UIConstants.COLOR_PRIMARY...
    }

    @Override
    public void onShow() {
        // Tự động gọi mỗi khi người dùng chuyển sang tab này
        refreshData();
    }

    @Override
    public void refreshData() {
        // Gọi Service nạp dữ liệu trên luồng nền (SwingWorker), không block EDT
    }

    @Override
    public void destroy() {
        // Giải phóng tài nguyên, hủy Listeners và Timers khi tab bị đóng
    }
}
```

### Bước 3: Đăng Ký Nút Mở Tab Trên Ribbon Menu
Trong `MainDashboardFrame.java`, liên kết nút bấm với `WorkspaceManager`:

```java
RibbonButton btnFeature = new RibbonButton("Tên Phân Hệ", "vector_icon", "my-feature-key",
        k -> workspaceManager.openView(new MyFeaturePanel()));
ribbonButtons.put("my-feature-key", btnFeature);
ribbon.add(btnFeature);
```

### Bước 4: Kiểm Tra Biên Dịch & Nghiệm Thu UI/UX (Verification Checklist)

Trước khi commit mã nguồn, bắt buộc kiểm tra các tiêu chí:
- [ ] Không có bất kỳ dòng nào dùng `new Font(...)` tự do; toàn bộ dùng `AppFonts`.
- [ ] Không dùng emoji hệ thống làm icon chính trên giao diện.
- [ ] Form hiển thị cân đối, không rớt dòng trên màn hình MacBook 13-inch (1280x800 hoặc 1440x900).
- [ ] Phím `Tab` và `Enter` di chuyển tuần tự đúng thứ tự tự nhiên giữa các ô nhập liệu.
- [ ] Phím `Esc` đóng được các popup và dialog con.
- [ ] Kiểm tra biên dịch trả về **0 lỗi** bằng lệnh:
  ```bash
  /opt/homebrew/opt/openjdk@17/bin/javac -d /tmp/javac_check $(find clinic-domain/src/main/java clinic-desktop/src/main/java -name "*.java")
  ```

---

## 8. LỊCH SỬ PHIÊN BẢN
* **Phiên bản 1.0 (2026-10-30):** Chuẩn hóa toàn bộ hệ thống chữ sang `AppFonts`, chuẩn hóa tỷ lệ hiển thị 1:1 cho MacBook 13-inch M1, tách độc lập phân hệ `modules/doctor` và `modules/examination`.
* **Phiên bản 2.0 (2026-11-01):** Đại tu toàn diện tài liệu thiết kế: Bổ sung phân tích chi tiết toàn bộ các Form, Dialog, Print Preview, Context Menu và Tab chuyên môn của 10 phân hệ y tế; thiết lập bản đồ phím tắt y tế toàn diện (F1-F12), chuẩn hóa kiến trúc MDI Tab Shell và cơ chế phòng ngừa sai sót y khoa.
