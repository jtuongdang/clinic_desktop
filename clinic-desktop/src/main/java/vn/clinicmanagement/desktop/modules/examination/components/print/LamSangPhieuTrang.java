package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.components.print.LamSangYeuCauPrintPreviewManHinh.*;
import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Rectangle2D;
import java.awt.print.*;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static vn.clinicmanagement.desktop.modules.examination.components.print.LamSangYeuCauPrintPreviewManHinh.safeStr;

/** A4 renderer shared by preview, printing and PDF; all pagination uses unscaled paper coordinates. */
public final class LamSangPhieuTrang extends JPanel implements Printable {
    public static final int WIDTH=858, HEIGHT=1214, GAP=18;
    private static final int X=43, TABLE_WIDTH=767, LINE=24, MAX_BOTTOM=1000;
    private static final int[] COL={350,73,76,168,100};
    private static final Font TEXT=UngDungPhongChu.CLS_TEXT, BOLD=UngDungPhongChu.CLS_BOLD,
            ITALIC=UngDungPhongChu.CLS_ITALIC;
    private static final FontRenderContext FRC=new FontRenderContext(null,true,true);
    private record Row(int kind,List<List<String>> cells,int height) { }
    private record Page(List<Row> rows) { }
    private record Room(String department,String queue,String barcode,String location) { }
    private final ClinicalOrderData data;
    private final List<Page> pages=new ArrayList<>();
    private final List<String> patientLines=new ArrayList<>();
    private final int tableTop,patientTop;
    private double displayScale=1;

    public LamSangPhieuTrang(ClinicalOrderData data) {
        this.data=data;setOpaque(false);setName("cls.paper");
        if(data!=null) {
            patientLines.addAll(wrap("- Địa chỉ: "+safeStr(data.patientAddress()),TEXT,TABLE_WIDTH));
            patientLines.addAll(wrap("- Đối tượng: "+subject(),TEXT,TABLE_WIDTH));
            patientLines.addAll(wrap("- Khoa phòng: "+safeStr(data.patientDepartment())+"                 Phòng/Giường: /",TEXT,TABLE_WIDTH));
            patientLines.addAll(wrap("- Chẩn đoán: "+safeStr(data.patientDiagnosis()),TEXT,TABLE_WIDTH));
            patientLines.add("- Chẩn đoán kèm theo: -");
        }
        int firstLines=data==null?1:Math.max(wrap("- Họ tên người bệnh: "+safeStr(data.patientName()),BOLD,310).size(),
                Math.max(wrap("NS: "+safeStr(data.patientBirthYear())+" ("+safeStr(data.patientAge())+" tuổi)",TEXT,175).size(),wrap("Giới tính: "+safeStr(data.patientGender()),TEXT,205).size()));
        patientTop=239+Math.max(0,firstLines-1)*LINE;
        tableTop=patientTop+patientLines.size()*24;paginate();
        addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){
            if(e.getClickCount()!=2||data==null)return;
            double x=e.getX()/displayScale,y=e.getY()/displayScale-(pages.size()-1)*(HEIGHT+GAP);
            int tableEnd=tableTop+56+pages.get(pages.size()-1).rows().stream().mapToInt(Row::height).sum();
            if(x>=454&&x<=799&&y>=tableEnd+95&&y<=tableEnd+161)
                JOptionPane.showMessageDialog(LamSangPhieuTrang.this,"Chưa kết nối dịch vụ ký điện tử cho phiếu chỉ định CLS.","Ký tài liệu",JOptionPane.INFORMATION_MESSAGE);
        }});
    }
    public String subject() {
        if(data==null)return "";
        String badge=safeStr(data.daysLeft());
        if(badge.toUpperCase(java.util.Locale.ROOT).contains("BHYT")||badge.equalsIgnoreCase("Viện phí"))return badge;
        return safeStr(data.patientBhyt()).isEmpty()?"Viện phí":"BHYT";
    }
    public int pageCount(){return pages.size();}
    public void setDisplayScale(double value){if(displayScale!=value){displayScale=value;revalidate();}}
    @Override public Dimension getPreferredSize(){return new Dimension((int)Math.round(WIDTH*displayScale),(int)Math.round((HEIGHT*pages.size()+GAP*(pages.size()-1))*displayScale));}
    private void paginate() {
        List<Row> rows=new ArrayList<>();
        if(data!=null){
            Map<Room,Map<String,List<OrderItem>>> rooms=new LinkedHashMap<>();
            for(OrderItem item:data.items()) {
                Room room=new Room(safeStr(item.department()).isEmpty()?safeStr(item.group()):safeStr(item.department()),safeStr(item.queueNumber()),safeStr(item.barcode()),safeStr(item.location()));
                rooms.computeIfAbsent(room,k->new LinkedHashMap<>()).computeIfAbsent(safeStr(item.subgroup()),k->new ArrayList<>()).add(item);
            }
            int roomIndex=0;
            for(var entry:rooms.entrySet()) {
                Room r=entry.getKey();String title=roman(++roomIndex)+". "+r.department();
                if(!r.queue().isEmpty())title+=", STT [ "+r.queue()+" ]";
                if(!r.barcode().isEmpty())title+=", Barcode [ "+r.barcode()+" ]";
                if(!r.location().isEmpty())title+=" ( "+r.location()+" )";
                rows.add(merged(0,title));
                for(var subgroup:entry.getValue().entrySet()) {
                    if(!subgroup.getKey().isEmpty())rows.add(merged(1,subgroup.getKey()));
                    int i=0;
                    for(OrderItem item:subgroup.getValue()) {
                        String[] text={(++i)+". "+safeStr(item.name()),safeStr(item.unit()),safeStr(item.quantity()),safeStr(item.notes()),safeStr(item.performed())};
                        List<List<String>> cells=new ArrayList<>();int lines=1;
                        for(int c=0;c<5;c++){List<String> wrapped=wrap(text[c],TEXT,COL[c]-14);cells.add(wrapped);lines=Math.max(lines,wrapped.size());}
                        // Split an exceptionally long service into bounded fragments, retaining every line.
                        for(int offset=0;offset<lines;offset+=12){List<List<String>> fragment=new ArrayList<>();int count=Math.min(12,lines-offset);
                            for(List<String> cell:cells)fragment.add(offset>=cell.size()?List.of():List.copyOf(cell.subList(offset,Math.min(cell.size(),offset+count))));
                            rows.add(new Row(2,fragment,count*LINE+6));
                        }
                    }
                }
            }
        }
        List<Row> current=new ArrayList<>();int y=tableTop+56;
        for(int i=0;i<rows.size();i++) {
            Row row=rows.get(i);int required=row.height();
            if(row.kind()<2)for(int j=i+1;j<rows.size();j++){required+=rows.get(j).height();if(rows.get(j).kind()==2)break;}
            if(y+required>MAX_BOTTOM&&!current.isEmpty()){pages.add(new Page(List.copyOf(current)));current.clear();y=tableTop+56;}
            current.add(row);y+=row.height();
        }
        pages.add(new Page(List.copyOf(current)));
    }
    private static Row merged(int kind,String text){List<String> lines=wrap(text,kind==0?BOLD:ITALIC,TABLE_WIDTH-14);return new Row(kind,List.of(lines),lines.size()*LINE+6);}
    static List<String> wrap(String text,Font font,int width) {
        List<String> lines=new ArrayList<>();
        for(String paragraph:safeStr(text).split("\\R",-1)) {
            if(paragraph.isEmpty()){lines.add("");continue;}
            AttributedString attributed=new AttributedString(paragraph);attributed.addAttribute(TextAttribute.FONT,font);
            LineBreakMeasurer breaker=new LineBreakMeasurer(attributed.getIterator(),FRC);int end=paragraph.length();
            while(breaker.getPosition()<end){int start=breaker.getPosition();breaker.nextLayout(Math.max(1,width));lines.add(paragraph.substring(start,breaker.getPosition()).stripTrailing());}
        }return lines;
    }
    @Override protected void paintComponent(Graphics graphics){
        Graphics2D g=(Graphics2D)graphics.create();g.scale(displayScale,displayScale);
        for(int i=0;i<pages.size();i++){Graphics2D page=(Graphics2D)g.create();page.translate(0,i*(HEIGHT+GAP));render(page,i,true);page.dispose();}g.dispose();
    }
    private void render(Graphics2D g,int page,boolean preview) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillRect(0,0,WIDTH,HEIGHT);g.setColor(Color.BLACK);
        if(data==null){center(g,"Chưa chọn phiếu chỉ định cận lâm sàng",BOLD,0,WIDTH,220);return;}
        // No hospital logo: the upper-left paper area intentionally remains white.
        center(g,"BỆNH VIỆN SẢN NHI",BOLD,210,340,84);center(g,"Đường Bình Than, Võ Cường, TP. Bắc Ninh",TEXT,210,340,107);
        center(g,"PID: "+safeStr(data.pidCode()),TEXT,625,185,40);
        ClsMaVach.draw(g,safeStr(data.pidCode()),638,51,158,33);
        center(g,"Số BA: "+safeStr(data.medicalRecordCode()),TEXT,610,210,112);
        center(g,"PHIẾU CHỈ ĐỊNH CẬN LÂM SÀNG",UngDungPhongChu.CLS_TITLE,0,WIDTH,172);
        List<String> name=wrap("- Họ tên người bệnh: "+safeStr(data.patientName()),BOLD,310);
        drawLines(g,name,BOLD,X,204);drawLines(g,wrap("NS: "+safeStr(data.patientBirthYear())+" ("+safeStr(data.patientAge())+" tuổi)",TEXT,175),TEXT,368,204);
        drawLines(g,wrap("Giới tính: "+safeStr(data.patientGender()),TEXT,205),TEXT,547,204);
        drawLines(g,patientLines,TEXT,X,patientTop);
        int y=tableTop;g.setStroke(new BasicStroke(1.3f));
        int x=X;String[] headings={"Tên dịch vụ","Đơn vị\ntính","Số\nlượng","Ghi chú","Đã thực\nhiện"};
        for(int c=0;c<5;c++){g.drawRect(x,y,COL[c],56);List<String> lines=List.of(headings[c].split("\n"));for(int line=0;line<lines.size();line++)center(g,lines.get(line),BOLD,x,COL[c],y+(56-lines.size()*LINE)/2+20+line*LINE);x+=COL[c];}
        y+=56;
        for(Row row:pages.get(page).rows()) {
            if(row.kind()<2){g.drawRect(X,y,TABLE_WIDTH,row.height());drawLines(g,row.cells().get(0),row.kind()==0?BOLD:ITALIC,X+7+(row.kind()==1?10:0),y+3);}
            else {x=X;for(int c=0;c<5;c++){g.drawRect(x,y,COL[c],row.height());List<String> lines=row.cells().get(c);
                if(c==1||c==2)for(int i=0;i<lines.size();i++)center(g,lines.get(i),TEXT,x,COL[c],y+(row.height()-lines.size()*LINE)/2+20+i*LINE);
                else drawLines(g,lines,TEXT,x+7,y+3);x+=COL[c];}}
            y+=row.height();
        }
        if(page==pages.size()-1) {
            center(g,safeStr(data.orderDateTime()),ITALIC,444,357,y+48);center(g,"BÁC SĨ CHỈ ĐỊNH",BOLD,444,357,y+78);
            if(preview){g.setColor(Color.BLUE);g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{10,8},0));g.draw(new Rectangle2D.Double(454,y+95,345,66));center(g,"Nháy đúp chuột để ký",UngDungPhongChu.TO_DIEU_TRI_BUTTON,454,345,y+134);}
        }
        if(pages.size()>1){g.setColor(Color.BLACK);center(g,"Trang "+(page+1)+" / "+pages.size(),TEXT,0,WIDTH,HEIGHT-24);}
    }
    private static void drawLines(Graphics2D g,List<String> lines,Font font,int x,int y){g.setFont(font);for(int i=0;i<lines.size();i++)g.drawString(lines.get(i),x,y+20+i*LINE);}
    private static void center(Graphics2D g,String text,Font font,int x,int width,int baseline){g.setFont(font);g.drawString(text,x+(width-g.getFontMetrics().stringWidth(text))/2,baseline);}
    private static String roman(int n){return n<11?new String[]{"","I","II","III","IV","V","VI","VII","VIII","IX","X"}[n]:String.valueOf(n);}
    public PageFormat pageFormat(){Paper paper=new Paper();paper.setSize(595.28,841.89);paper.setImageableArea(0,0,595.28,841.89);PageFormat format=new PageFormat();format.setPaper(paper);return format;}
    @Override public int print(Graphics graphics,PageFormat format,int index) {
        if(index<0||index>=pages.size())return NO_SUCH_PAGE;
        Graphics2D g=(Graphics2D)graphics.create();g.translate(format.getImageableX(),format.getImageableY());double fit=Math.min(format.getImageableWidth()/WIDTH,format.getImageableHeight()/HEIGHT);g.scale(fit,fit);render(g,index,false);g.dispose();return PAGE_EXISTS;
    }
}
