# Báo Cáo Rà Soát Giao Diện ReceptionPanel (Source-Only)

> **Mục tiêu:** Rà soát source-only mã nguồn `TiepDonManHinh.java` đối chiếu với chuẩn `UI_CODING_STANDARDS.md` trên nhánh `feature/patient-reception-workflow`.  
> **Phạm vi kiểm tra:** Duy nhất 2 file source/standard được chỉ định; không kiểm tra test/báo cáo khác/.git/main; không chạy lệnh.  
> **Trạng thái Parity với main:** **UNKNOWN** (chưa xác minh).  
> **Kiểm thử hiển thị / Nghiệm thu:** **Không có visual smoke / Chưa có Owner acceptance**.

---

## 1. Các thành phần, Layout Constants & Labels hiện có

### 1.1. Cấu trúc tổng thể & Kế thừa
- **Kế thừa & Định danh view:** `TiepDonManHinh` kế thừa `CoSoManHinhManHinh` ([L57](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L57)), gọi `super("reception", "Đăng Ký Khám Bệnh", "📋")` ([L109](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L109)).
- **Bố cục chính:** `BorderLayout` ([L110](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L110)), nền `Color.WHITE` ([L111](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L111)).
- **Vùng North:** `TiepDonTrenThanhCongCu` ([L121-122](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L121-L122)).
- **Vùng Center (JSplitPane dọc):** Chứa `BenhNhanDangKyBieuMauManHinh` (trên) và `PhongKhamPhongTrangThaiManHinh` (dưới) ([L125-137](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L125-L137)).
  - Hằng số chia: `splitPane.setDividerLocation(218)` ([L132](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L132)).
  - Độ dày vạch chia: `splitPane.setDividerSize(4)` ([L133](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L133)).
  - Bật `continuousLayout(true)` và xóa viền ranh giới ([L134-135](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L134-L135)).
- **Vùng South:** `TiepDonChanTrangThanh` ([L140-141](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L140-L141)).

### 1.2. Liên kết dữ liệu & Sự kiện điều khiển
- **Đồng bộ Form ➔ Phòng khám:** `formPanel.setOnClinicRoomChanged(room -> statusPanel.setSelectedClinicRoom(room))` ([L129](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L129)).
- **Gắn hành vi thanh công cụ & chân trang:**
  - `topToolbar.setOnRefresh(this::refreshData)` ([L144](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L144)).
  - `topToolbar.setOnNew(this::clearForm)` ([L145](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L145)).
  - `footerBar.setOnSave(this::saveRegistration)` ([L146](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L146)).
  - `footerBar.setOnNew(this::clearForm)` ([L147](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L147)).
  - `formPanel.setOnSearch(this::searchPatients)` ([L148](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L148)).
- **Luồng luân chuyển nền (EDT & SwingWorker):**
  - Tải dữ liệu hàng đợi/phòng: `SwingWorker` trong `refreshData()` ([L191-235](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L191-L235)).
  - Lưu tiếp đón: `SwingWorker` trong `saveRegistration()` ([L501-540](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L501-L540)).
  - Tìm kiếm bệnh nhân: `SwingWorker` trong `searchPatients()` ([L562-610](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L562-L610)).
  - Hủy worker đang bay và dọn tài nguyên khi đóng tab: `destroy()` ([L399-404](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L399-L404)).

### 1.3. Chuỗi nhãn / Tiêu đề giao diện (Labels & Dialogs)
- Tiêu đề tab: `"Đăng Ký Khám Bệnh"` ([L109](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L109)).
- Thông báo phiên / lỗi: `"Phiên làm việc không hợp lệ hoặc đã hết hạn."` ([L488, L558](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L488)).
- Thông báo thành công: `"Đăng ký thành công"`, `"Đã đăng ký người bệnh {patientCode} · Số tiếp đón {queueNumberDisplay}"` ([L524-525](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L524-L525)).
- Thông báo trùng CCCD: `"Trùng số CCCD"` ([L614](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L614)).
- Hộp thoại chọn bệnh nhân: `"Kết quả tìm kiếm"`, `"Chọn hồ sơ người bệnh hiện có (Hủy để giữ thông tin đang nhập):"` ([L622-627](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L622-L627)).
- Tiêu đề hộp thoại lỗi: `"Thông báo lỗi"` ([L642](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L642)).

---

## 2. Bảng đối soát chuẩn UI (`UI_CODING_STANDARDS.md`)

| Mục kiểm tra theo chuẩn | Bằng chứng mã nguồn (`TiepDonManHinh.java`) | Đánh giá | Ghi chú chi tiết |
| :--- | :--- | :---: | :--- |
| **Kế thừa `CoSoManHinhManHinh`** (Mục 2.3, 4.9.A) | `extends CoSoManHinhManHinh` ([L57](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L57)), viewKey `"reception"` ([L109](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L109)) | **PASS** | Đúng tên phân hệ và viewKey quản lý bởi `KhongGianLamViecQuanLy`. |
| **Bố cục JSplitPane dọc chia 218px** (Mục 4.9.A) | `JSplitPane.VERTICAL_SPLIT`, `setDividerLocation(218)` ([L131-132](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L131-L132)) | **PASS** | Khớp chính xác 100% quy định 218px. |
| **Phân rã thành phần con** (Mục 2.4, 4.9.A) | Tách riêng `topToolbar`, `formPanel`, `statusPanel`, `footerBar` ([L59-62](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L59-L62)) | **PASS** | Không gộp mã monolithic, tuân thủ kiến trúc phân rã module. |
| **Tuân thủ Swing EDT & Worker** (Mục 2.6) | Tách tác vụ IO vào `SwingWorker`, có `runOnEdt()` và kiểm tra EDT ([L165, L191, L406-412, L480, L501, L543, L562](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L165)) | **PASS** | Không chặn UI thread khi gọi handler/use case. |
| **Giải phóng tài nguyên `destroy()`** (Mục 2.7) | Triển khai `destroy()` hủy in-flight workers và hủy listener session ([L399-404](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L399-L404)) | **PASS** | Bảo đảm an toàn bộ nhớ khi tab bị đóng. |
| **Cấm dùng Emoji làm icon chính** (Mục 2.2) | `super("reception", "Đăng Ký Khám Bệnh", "📋")` ([ReceptionPanel.java:L109](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L109)); Golden Rule 2 ([UI_CODING_STANDARDS.md:L57](../../UI_CODING_STANDARDS.md#L57)) | **FAIL** | Icon tab đang dùng emoji hệ thống; cần xác nhận asset thay thế với thiết kế gốc trước khi thay đổi để tránh làm lệch giao diện đã duyệt. |
| **Bảng màu giao diện chuẩn** (Mục 3.2 và mẫu 4.9.A) | `setBackground(Color.WHITE)` ([ReceptionPanel.java:L111](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L111)); standards define `BG_PANEL` ([UI_CODING_STANDARDS.md:L93-L105](../../UI_CODING_STANDARDS.md#L93)) but its boilerplate also uses `Color.WHITE` ([UI_CODING_STANDARDS.md:L612](../../UI_CODING_STANDARDS.md#L612)) | **UNKNOWN** | Tài liệu đang tự mâu thuẫn về màu nền; không nên đổi màu màn hình cho đến khi chốt chuẩn và xác nhận màu thiết kế gốc. |
| **Đóng gói & Không can thiệp Reflection** (Chuẩn mã nguồn sạch) | Can thiệp private field `YTeBatLenBoChon.list` bằng `getDeclaredField` ([L355-363](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L355-L363)) | **FAIL** | Phá vỡ tính bao đóng, dễ gãy ngầm khi refactor component. |
| **Đăng ký phím tắt bàn phím F1-F12 tại panel** (Mục 5.1) | Chỉ liên kết nút bấm qua callback ([L144-147](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L144-L147)) | **UNKNOWN** | `TiepDonManHinh` không trực tiếp bind `InputMap`/`ActionMap` cho F1-F12; có thể đã xử lý ngầm bên trong toolbar/footer hoặc chưa có. |
| **Đối soát tính tương thích với nhánh `main`** | Không đọc/checkout nhánh `main` | **UNKNOWN** | Chưa xác minh parity với `main`. |
| **Kiểm thử hiển thị & Nghiệm thu người dùng** | Source-only review, không chạy ứng dụng | **UNKNOWN** | Không có visual smoke, chưa có Owner acceptance. |

---

## 3. Đề xuất tối thiểu khắc phục lỗi

1. **Icon Emoji ([ReceptionPanel.java:L109](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L109)):**
   - Chuẩn hiện hành đánh dấu emoji là không phù hợp; xác nhận vector/icon asset với thiết kế gốc trước khi thay, vì report này chưa kiểm chứng visual parity.
2. **Màu nền ([ReceptionPanel.java:L111](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L111)):**
   - Chưa đề xuất sửa source: chuẩn màu mục 3.2 và boilerplate mẫu 4.9.A mâu thuẫn. Chốt chuẩn và màu thiết kế gốc trước khi đổi `Color.WHITE`.
3. **Loại bỏ Reflection ([ReceptionPanel.java:L355-L363](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L355)):**
   - Thay bằng API component có kiểu rõ ràng, giữ nguyên giao diện và lựa chọn hiện tại; kiểm tra liên kết mã phòng riêng để không làm hỏng luồng lưu.
4. **Rà soát phím tắt F1-F12:**
   - Xác nhận cơ chế bắt phím tắt (Key Bindings) tại `TiepDonChanTrangThanh` / `TiepDonTrenThanhCongCu` để đảm bảo hoạt động khi con trỏ đang ở bất kỳ trường nhập liệu nào.
