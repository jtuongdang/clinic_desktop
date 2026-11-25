package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.modules.reports.catalog.*;
import vn.clinicmanagement.desktop.modules.reports.components.*;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.TruyVan;
import vn.clinicmanagement.infrastructure.persistence.reports.PhieuBaoCaoRepository;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import static vn.clinicmanagement.desktop.modules.reports.components.PhieuBaoCaoMoHinh.safe;

/** Catalog stays in place; report data expands below its right-hand table. */
public final class DanhSachBaoCaoManHinh extends CoSoManHinhManHinh {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final DanhMucMauBaoCaoManHinh catalog;
    private final BaoCaoDuLieuNoiTuyenManHinh data;
    private PhongKhamBaoCaoManHinh statistics;
    public DanhSachBaoCaoManHinh(){this(new BaoCaoDanhMucLuuTru(),null);}
    public DanhSachBaoCaoManHinh(Runnable ignored){this(new BaoCaoDanhMucLuuTru(),ignored);}
    public DanhSachBaoCaoManHinh(BaoCaoDanhMucLuuTru store,Runnable ignored){this(store,ignored,new PhieuBaoCaoRepository(),KhamBenhPhienNguCanh::current);}
    public DanhSachBaoCaoManHinh(BaoCaoDanhMucLuuTru store,Runnable ignored,PhieuBaoCaoReadCongKetNoi source,Supplier<PhanQuyenNguCanh> authorization){
        super("reports","Danh Sách Báo Cáo","");
        catalog=new DanhMucMauBaoCaoManHinh(store,this::showStatisticsInline);
        data=new BaoCaoDuLieuNoiTuyenManHinh(source,authorization,this::collapseData);
        catalog.setReportOpener(this::openReport);add(catalog,BorderLayout.CENTER);
    }
    public void openReport(BaoCaoMau report){
        if(statistics!=null){statistics.onHide();statistics=null;}
        catalog.getCatalogUi().showDetails(data);
        data.openReport(report);
    }
    public void collapseData(){data.cancelLoading();if(statistics!=null){statistics.onHide();statistics=null;}catalog.getCatalogUi().hideDetails();}
    public void showStatisticsInline(){
        data.cancelLoading();if(statistics==null)statistics=new PhongKhamBaoCaoManHinh();
        catalog.getCatalogUi().showDetails(statistics.createInlinePreview(this::collapseData));statistics.executeReportQuery();
    }
    @Override public void refreshData(){if(catalog.getCatalogUi().isDetailsVisible()){if(statistics!=null)statistics.refreshData();else data.reload();}else catalog.refreshData();}
    @Override public void onShow(){} // Catalog is loaded once; preserve scroll, selection, filters and open detail on tab activation.
    @Override public void onHide(){data.cancelLoading();if(statistics!=null)statistics.onHide();}
    @Override public boolean canClose(){return catalog.canClose();}
    public BaoCaoDanhMucBoCuc getCatalogUi(){return catalog.getCatalogUi();}
    public BaoCaoDanhMucMoHinh getCatalogModel(){return catalog.getCatalogModel();}
    public BaoCaoDuLieuNoiTuyenManHinh getDataPanel(){return data;}
    public boolean isLoading(){return data.isLoading();}
    public static TruyVan parseRange(String from,String to,PhanQuyenNguCanh auth){
        if(auth==null||!auth.isValid())throw new IllegalArgumentException("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
        if(!Set.of("ADMIN","DIRECTOR","DOCTOR").contains(auth.roleCode()))throw new IllegalArgumentException("Tài khoản không có quyền xem báo cáo phiếu.");
        LocalDateTime start,end;try{start=LocalDateTime.parse(from.strip(),DATE);end=LocalDateTime.parse(to.strip(),DATE);}catch(DateTimeException ex){throw new IllegalArgumentException("Nhập ngày theo định dạng HH:mm dd/MM/yyyy.");}
        if(end.isBefore(start))throw new IllegalArgumentException("Đến ngày không được trước từ ngày.");
        return new TruyVan(start,end.plusMinutes(1),auth.facilityId(),"DOCTOR".equals(auth.roleCode())?auth.departmentId():null);
    }
    public static void writeCsv(Path destination,List<String> columns,List<List<String>> rows)throws java.io.IOException{
        Path parent=destination.toAbsolutePath().getParent();Path temporary=Files.createTempFile(parent,"report-tickets-",".csv");try{try(var out=Files.newBufferedWriter(temporary,StandardCharsets.UTF_8)){out.write('\ufeff');out.write(columns.stream().map(DanhSachBaoCaoManHinh::csvCell).collect(java.util.stream.Collectors.joining(",")));out.newLine();for(var row:rows){out.write(row.stream().map(DanhSachBaoCaoManHinh::csvCell).collect(java.util.stream.Collectors.joining(",")));out.newLine();}}Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}}
    private static String csvCell(String value){String text=safe(value);String start=text.stripLeading();if(!start.isEmpty()&&"=+-@".indexOf(start.charAt(0))>=0)text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
}
