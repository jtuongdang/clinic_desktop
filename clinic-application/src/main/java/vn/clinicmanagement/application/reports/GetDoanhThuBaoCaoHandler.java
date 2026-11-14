package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Xử lý truy vấn báo cáo doanh thu tài chính phòng khám (RPT-01).
 * Thẩm quyền yêu cầu: ADMIN, DIRECTOR, ACCOUNTANT.
 * Báo cáo doanh thu đa chiều: khám bệnh, cận lâm sàng, thuốc, đối soát BHYT và tiền mặt/chuyển khoản.
 */
public final class GetDoanhThuBaoCaoHandler implements QueryHandler<GetDoanhThuBaoCaoQuery, DoanhThuBaoCaoDuLieu> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DIRECTOR", "ACCOUNTANT");

    /** Trường `dataSource`: dữ liệu nguồn. */
    private final IDoanhThuBaoCaoDuLieuNguon dataSource;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetDoanhThuBaoCaoHandler(IDoanhThuBaoCaoDuLieuNguon dataSource, KiemToanWriteCongKetNoi auditWritePort) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.auditWritePort = auditWritePort;
    }

    public GetDoanhThuBaoCaoHandler(IDoanhThuBaoCaoDuLieuNguon dataSource) {
        this(dataSource, null);
    }

    public GetDoanhThuBaoCaoHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort) {
        Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.dataSource = (from, to, facId, deptId) -> {
            List<HoaDon> all = invoiceRepository.getAll();
            if (all == null) return List.of();
            return all.stream()
                    .filter(Objects::nonNull)
                    .filter(h -> !h.isDeleted())
                    .filter(h -> h.getTrangThai() != TrangThaiHoaDon.DA_HUY)
                    .filter(h -> {
                        if (h.getNgayLap() == null) return false;
                        LocalDate d = h.getNgayLap().toLocalDate();
                        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                    })
                    .filter(h -> facId == null || facId == 0 || h.getMaCoSo() == facId)
                    .toList();
        };
        this.auditWritePort = auditWritePort;
    }

    public GetDoanhThuBaoCaoHandler(IHoaDonRepository invoiceRepository) {
        this(invoiceRepository, (KiemToanWriteCongKetNoi) null);
    }

    public GetDoanhThuBaoCaoHandler(IHoaDonRepository invoiceRepository, IDoanhThuBaoCaoDuLieuNguon dataSource) {
        this(dataSource != null ? dataSource : fromRepository(invoiceRepository), null);
    }

    public GetDoanhThuBaoCaoHandler(IHoaDonRepository invoiceRepository, IDoanhThuBaoCaoDuLieuNguon dataSource, KiemToanWriteCongKetNoi auditWritePort) {
        this(dataSource != null ? dataSource : fromRepository(invoiceRepository), auditWritePort);
    }

    /** Hàm `fromRepository`: tu kho dữ liệu. */
    private static IDoanhThuBaoCaoDuLieuNguon fromRepository(IHoaDonRepository invoiceRepository) {
        Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        return (from, to, facId, deptId) -> {
            List<HoaDon> all = invoiceRepository.getAll();
            if (all == null) return List.of();
            return all.stream()
                    .filter(Objects::nonNull)
                    .filter(h -> !h.isDeleted())
                    .filter(h -> h.getTrangThai() != TrangThaiHoaDon.DA_HUY)
                    .filter(h -> {
                        if (h.getNgayLap() == null) return false;
                        LocalDate d = h.getNgayLap().toLocalDate();
                        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                    })
                    .filter(h -> facId == null || facId == 0 || h.getMaCoSo() == facId)
                    .toList();
        };
    }

    @Override
    public KetQua<DoanhThuBaoCaoDuLieu> handle(GetDoanhThuBaoCaoQuery query) {
        if (query == null) {
            return KetQua.failure("QUERY_INVALID", "Truy vấn báo cáo doanh thu không được để trống.");
        }

        // 1. Kiểm tra thẩm quyền nghiêm ngặt (ADMIN, DIRECTOR, ACCOUNTANT)
        String role = query.getEffectiveRole();
        if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role)) {
            logAudit(query.getAuditActorContext(), KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN,
                    "Từ chối quyền xem báo cáo doanh thu cho vai trò: " + role);
            return KetQua.failure("AUTHZ_DENIED",
                    "Chỉ Quản trị viên (ADMIN), Giám đốc (DIRECTOR) hoặc Kế toán (ACCOUNTANT) mới có quyền xem báo cáo doanh thu.");
        }

        // 2. Kiểm tra khoảng thời gian
        LocalDate fromDate = query.getFromDate() != null ? query.getFromDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = query.getToDate() != null ? query.getToDate() : LocalDate.now();

        if (fromDate.isAfter(toDate)) {
            return KetQua.failure("INVALID_DATE_RANGE", "Từ ngày không được lớn hơn đến ngày.");
        }

        // 3. Tra cứu hóa đơn trong kỳ
        List<HoaDon> invoices;
        try {
            invoices = dataSource.getInvoices(fromDate, toDate, query.getFacilityId(), query.getDepartmentId());
            if (invoices == null) {
                invoices = List.of();
            }
        } catch (Exception ex) {
            return KetQua.failure("DATA_FETCH_FAILED", "Không thể lấy dữ liệu hóa đơn: " + ex.getMessage());
        }

        // 4. Tính toán tổng hợp đa chiều
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal consultationRevenue = BigDecimal.ZERO;
        BigDecimal paraclinicalRevenue = BigDecimal.ZERO;
        BigDecimal medicationRevenue = BigDecimal.ZERO;
        BigDecimal otherRevenue = BigDecimal.ZERO;

        BigDecimal totalInsurance = BigDecimal.ZERO;
        BigDecimal totalPatient = BigDecimal.ZERO;
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalTransfer = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalAdvance = BigDecimal.ZERO;

        long consultationCount = 0;
        long paraclinicalCount = 0;
        long medicationCount = 0;
        long otherCount = 0;

        long paidInvoices = 0;
        long totalInvoices = invoices.size();

        for (HoaDon hd : invoices) {
            if (hd == null) continue;

            BigDecimal invoiceTotal = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(invoiceTotal);

            if (hd.getSoTienBaoHiemChiTra() != null) {
                totalInsurance = totalInsurance.add(hd.getSoTienBaoHiemChiTra());
            }
            if (hd.getSoTienBenhNhanChiTra() != null) {
                totalPatient = totalPatient.add(hd.getSoTienBenhNhanChiTra());
            }
            if (hd.getSoTienGiamGia() != null) {
                totalDiscount = totalDiscount.add(hd.getSoTienGiamGia());
            }
            if (hd.getSoTienTamUng() != null) {
                totalAdvance = totalAdvance.add(hd.getSoTienTamUng());
            }

            if (hd.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) {
                paidInvoices++;
            }

            // Đối soát hình thức thanh toán từ phiếu thu
            if (hd.getPhieuThus() != null) {
                for (PhieuThu pt : hd.getPhieuThus()) {
                    if (pt == null || pt.getSoTien() == null) continue;
                    if (pt.getPhuongThuc() == PhuongThucThanhToan.TIEN_MAT) {
                        totalCash = totalCash.add(pt.getSoTien());
                    } else if (pt.getPhuongThuc() == PhuongThucThanhToan.CHUYEN_KHOAN || pt.getPhuongThuc() == PhuongThucThanhToan.THE) {
                        totalTransfer = totalTransfer.add(pt.getSoTien());
                    } else {
                        totalCash = totalCash.add(pt.getSoTien());
                    }
                }
            }

            // Phân bổ doanh thu theo nhóm chi tiết
            List<ChiTietHoaDon> details = hd.getChiTietHoaDons();
            if (details != null && !details.isEmpty()) {
                for (ChiTietHoaDon d : details) {
                    if (d == null) continue;
                    BigDecimal qty = d.getSoLuong() != null ? d.getSoLuong() : BigDecimal.ONE;
                    BigDecimal price = d.getDonGia() != null ? d.getDonGia() : BigDecimal.ZERO;
                    BigDecimal lineAmount = qty.multiply(price);

                    String type = d.getLoaiDoiTuong() != null ? d.getLoaiDoiTuong().trim().toUpperCase(Locale.ROOT) : "";
                    String desc = d.getMoTa() != null ? d.getMoTa().trim().toLowerCase(Locale.ROOT) : "";

                    if (type.contains("KHAM") || desc.contains("khám") || type.equals("CONSULTATION")) {
                        consultationRevenue = consultationRevenue.add(lineAmount);
                        consultationCount += qty.longValue() > 0 ? qty.longValue() : 1;
                    } else if (type.contains("CLS") || type.contains("CAN_LAM_SANG") || type.contains("DICH_VU")
                            || type.contains("XET_NGHIEM") || type.contains("SIEU_AM") || type.contains("XQUANG")
                            || desc.contains("xét nghiệm") || desc.contains("siêu âm") || desc.contains("chụp")) {
                        paraclinicalRevenue = paraclinicalRevenue.add(lineAmount);
                        paraclinicalCount += qty.longValue() > 0 ? qty.longValue() : 1;
                    } else if (type.contains("THUOC") || type.contains("DUOC") || type.contains("MEDICATION")
                            || desc.contains("thuốc") || desc.contains("viên") || desc.contains("gói")) {
                        medicationRevenue = medicationRevenue.add(lineAmount);
                        medicationCount += qty.longValue() > 0 ? qty.longValue() : 1;
                    } else {
                        otherRevenue = otherRevenue.add(lineAmount);
                        otherCount += qty.longValue() > 0 ? qty.longValue() : 1;
                    }
                }
            } else {
                // Nếu hóa đơn không có dòng chi tiết, tính vào doanh thu dịch vụ chung
                otherRevenue = otherRevenue.add(invoiceTotal);
                otherCount++;
            }
        }

        // 5. Tạo danh sách RevenueReportItemDto
        List<DoanhThuBaoCaoMatHangDuLieu> items = new ArrayList<>();

        double consultPct = calcPct(consultationRevenue, totalRevenue);
        items.add(new DoanhThuBaoCaoMatHangDuLieu(1, "Doanh thu dịch vụ khám bệnh", "VNĐ", consultationCount,
                consultationRevenue, BigDecimal.ZERO, null, BigDecimal.ZERO, consultationRevenue, consultPct, "Công khám ban đầu & chuyên khoa"));

        double paraclinicalPct = calcPct(paraclinicalRevenue, totalRevenue);
        items.add(new DoanhThuBaoCaoMatHangDuLieu(2, "Doanh thu cận lâm sàng & kỹ thuật", "VNĐ", paraclinicalCount,
                paraclinicalRevenue, BigDecimal.ZERO, null, BigDecimal.ZERO, paraclinicalRevenue, paraclinicalPct, "Xét nghiệm, CĐHA, TDCN"));

        double medicationPct = calcPct(medicationRevenue, totalRevenue);
        items.add(new DoanhThuBaoCaoMatHangDuLieu(3, "Doanh thu thuốc & vật tư y tế", "VNĐ", medicationCount,
                medicationRevenue, BigDecimal.ZERO, null, BigDecimal.ZERO, medicationRevenue, medicationPct, "Dược phẩm bán lẻ & cấp phát ngoại trú"));

        if (otherRevenue.compareTo(BigDecimal.ZERO) > 0) {
            double otherPct = calcPct(otherRevenue, totalRevenue);
            items.add(new DoanhThuBaoCaoMatHangDuLieu(4, "Doanh thu dịch vụ khác", "VNĐ", otherCount,
                    otherRevenue, BigDecimal.ZERO, null, BigDecimal.ZERO, otherRevenue, otherPct, "Dịch vụ phát sinh ngoài"));
        }

        items.add(new DoanhThuBaoCaoMatHangDuLieu(items.size() + 1, "Tổng doanh thu viện phí", "VNĐ", totalInvoices,
                totalRevenue, BigDecimal.ZERO, null, totalInsurance, totalPatient, 100.0, "Đã gồm BHYT chi trả và bệnh nhân chi trả"));

        DoanhThuBaoCaoDuLieu dto = new DoanhThuBaoCaoDuLieu(
                fromDate, toDate,
                totalRevenue,
                consultationRevenue,
                paraclinicalRevenue,
                medicationRevenue,
                otherRevenue,
                totalInsurance,
                totalPatient,
                totalCash,
                totalTransfer,
                totalDiscount,
                totalAdvance,
                totalInvoices,
                paidInvoices,
                items
        );

        // 6. Ghi nhật ký kiểm toán (CATALOG_VIEW) nếu có context
        logAudit(query.getAuditActorContext(), KiemToanKetQua.SUCCESS, null, "Xem báo cáo doanh thu tài chính");

        return KetQua.success(dto);
    }

    private double calcPct(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0 || part == null || part.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return part.multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Hàm `logAudit`: nhatky kiểm toán. */
    private void logAudit(KiemToanTacNhanNguCanh ctx, KiemToanKetQua outcome, KiemToanLyDoMa reasonCode, String message) {
        if (auditWritePort == null || ctx == null) {
            return;
        }
        try {
            HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                    ctx.employeeId(),
                    ctx.roleId(),
                    ctx.roleCode(),
                    ctx.facilityId(),
                    ctx.departmentId(),
                    outcome == KiemToanKetQua.SUCCESS ? KiemToanHanhDongMa.CATALOG_VIEW : KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                    null,
                    null,
                    outcome,
                    reasonCode,
                    Instant.now(),
                    ctx.workstationId(),
                    ctx.correlationId()
            );
            auditWritePort.appendIndependently(entry);
        } catch (Exception ignored) {
            // Audit failure does not abort read-only query
        }
    }
}
