package vn.clinicmanagement.application.catalog;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A service consumption definition; selecting materials never issues physical stock. */
public record VatTuHaoPhiCauHinh(Stage stage,boolean rounding,List<Line> lines) {
    public enum Stage {
        NONE(""),ORDER("Kê khi chỉ định"),START("Kê khi bắt đầu thực hiện"),RESULT("Kê khi trả kết quả"),
        ISSUE("Kê khi xuất kho"),EXECUTION_TIME("Kê khi nhập thời gian thực hiện");
        private final String title;Stage(String title){this.title=title;}public String title(){return title;}
    }
    public record Line(int itemId,int warehouseId,String code,String name,String unit,String packing,String warehouseName,BigDecimal quantity){
        public Line {if(itemId<=0||warehouseId<=0||quantity==null||quantity.signum()<=0||quantity.scale()>4||quantity.precision()-quantity.scale()>16)throw new IllegalArgumentException("Vật tư, kho và số lượng hao phí không hợp lệ.");}
    }
    public VatTuHaoPhiCauHinh {
        stage=stage==null?Stage.NONE:stage;lines=List.copyOf(lines);
        if(lines.size()>100||lines.stream().map(l->l.itemId()+":"+l.warehouseId()).distinct().count()!=lines.size())throw new IllegalArgumentException("Danh sách có vật tư trùng hoặc quá 100 dòng.");
    }
    public static VatTuHaoPhiCauHinh empty(){return new VatTuHaoPhiCauHinh(Stage.NONE,false,List.of());}
    public void validate(){if(!lines.isEmpty()&&stage==Stage.NONE)throw new IllegalArgumentException("Chọn thời điểm kê vật tư hao phí.");}
    public String encode(){
        Properties p=new Properties();p.setProperty("version","1");p.setProperty("stage",stage.name());p.setProperty("rounding",String.valueOf(rounding));p.setProperty("count",String.valueOf(lines.size()));
        for(int i=0;i<lines.size();i++){var l=lines.get(i);String key="line."+i+".";
            p.setProperty(key+"item",String.valueOf(l.itemId()));p.setProperty(key+"warehouse",String.valueOf(l.warehouseId()));p.setProperty(key+"code",l.code());p.setProperty(key+"name",l.name());
            p.setProperty(key+"unit",l.unit());p.setProperty(key+"packing",l.packing());p.setProperty(key+"warehouseName",l.warehouseName());p.setProperty(key+"quantity",l.quantity().toPlainString());
        }
        try(var bytes=new ByteArrayOutputStream()){p.storeToXML(bytes,"Vật tư hao phí",StandardCharsets.UTF_8);return bytes.toString(StandardCharsets.UTF_8);}
        catch(IOException ex){throw new IllegalStateException("Không tạo được cấu hình hao phí.",ex);}
    }
    public static VatTuHaoPhiCauHinh decode(String value){
        if(value==null||value.isBlank())return empty();Properties p=new Properties();
        try(var bytes=new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8))){p.loadFromXML(bytes);
            if(!"1".equals(p.getProperty("version")))throw new IllegalArgumentException("Phiên bản cấu hình chưa hỗ trợ.");
            int count=Integer.parseInt(p.getProperty("count"));if(count<0||count>100)throw new IllegalArgumentException("Số dòng không hợp lệ.");
            List<Line> lines=new ArrayList<>();for(int i=0;i<count;i++){String k="line."+i+".";lines.add(new Line(Integer.parseInt(p.getProperty(k+"item")),Integer.parseInt(p.getProperty(k+"warehouse")),p.getProperty(k+"code",""),p.getProperty(k+"name",""),p.getProperty(k+"unit",""),p.getProperty(k+"packing",""),p.getProperty(k+"warehouseName",""),new BigDecimal(p.getProperty(k+"quantity"))));}
            return new VatTuHaoPhiCauHinh(Stage.valueOf(p.getProperty("stage")),Boolean.parseBoolean(p.getProperty("rounding")),lines);
        }catch(IOException|RuntimeException ex){throw new IllegalArgumentException("Không đọc được cấu hình vật tư hao phí. Nội dung cũ được giữ nguyên.",ex);}
    }
    public String summary(){
        if(lines.isEmpty())return "";
        String flag=switch(stage){case ORDER->"KeKhiChiDinh";case START->"KeKhiBatDauThucHien";case RESULT->"KeKhiTraKetQua";case ISSUE->"KeKhiXuatKho";case EXECUTION_TIME->"KeKhiNhapThoiGianThucHien";default->"ChuaChonThoiDiem";};
        return "{\""+flag+"\":1,\"WarehouseID\":"+lines.get(0).warehouseId()+",\"LamTron\":"+(rounding?1:0)+",\"Items\":["+
                String.join(",",lines.stream().map(l->"{\"ItemID\":"+l.itemId()+",\"WarehouseID\":"+l.warehouseId()+",\"Quantity\":"+l.quantity().toPlainString()+"}").toList())+"]}";
    }
}
