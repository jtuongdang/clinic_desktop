package vn.clinicmanagement.desktop.modules.print.examination;


import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.print.examination.ToDieuTriPrintManHinh.TreatmentSheetData;
import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

/** A4 treatment page with wrapping text and independent signatures below each column. */
public final class ToDieuTriTrangGiay extends JPanel {
    static final int WIDTH = 844, HEIGHT = 1194;
    private static final int TABLE_X = 44, TABLE_TOP = 208, DATE_WIDTH = 98, PROGRESS_WIDTH = 300, ORDERS_WIDTH = 360;
    private final TreatmentSheetData data;
    private final List<Paragraph> progress = new ArrayList<>(), orders = new ArrayList<>();
    private final FontRenderContext metrics = new FontRenderContext(null,true,true);
    private int bodyHeight, pageHeight, tableY, departmentBaseline, diagnosisTop;
    private double displayScale = 1;
    private Rectangle2D leftSignature, rightSignature;
    private record Paragraph(String prefix, String text, Font font) {}

    public ToDieuTriTrangGiay(TreatmentSheetData data) {
        this.data = data;
        setBackground(Color.WHITE);
        add(progress,"Lý do khám: ",data.reasonText(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        add(progress,"Diễn biến: ",data.diseaseProgress(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        add(progress,"Tiền sử bản thân: ",data.personalHistory(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        add(progress,"Tiền sử gia đình: ",data.familyHistory(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        add(progress,"Phương pháp điều trị: ",data.treatmentMethod(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        add(progress,"CĐ: ",data.diagnosisText(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        section("Thuốc:",data.medicationLines(),UngDungPhongChu.TO_DIEU_TRI_ITALIC);
        section("Vật tư:",data.supplyLines(),UngDungPhongChu.TO_DIEU_TRI_ITALIC);
        section("Xét nghiệm / CLS:",data.clsLines(),UngDungPhongChu.TO_DIEU_TRI_TEXT);
        measurePage(); setDisplayScale(1);
        setToolTipText("Tờ điều trị từ dữ liệu lượt khám đã lưu");
    }
    private static void add(List<Paragraph> target,String prefix,String text,Font font) {
        if(text != null && !text.isBlank()) target.add(new Paragraph(prefix,text,font));
    }
    private void section(String title,List<String> lines,Font font) {
        if(lines == null || lines.isEmpty()) return;
        if(!orders.isEmpty()) orders.add(new Paragraph(""," ",UngDungPhongChu.TO_DIEU_TRI_TEXT));
        orders.add(new Paragraph(title,"",UngDungPhongChu.TO_DIEU_TRI_BOLD));
        for(String line:lines) add(orders,"",line,font);
    }
    private void measurePage() {
        double patientHeight = paragraph(null,new Paragraph("- Họ tên người bệnh: ",data.patientName(),UngDungPhongChu.TO_DIEU_TRI_BOLD),0,0,478);
        departmentBaseline = 107 + (int)Math.ceil(patientHeight) + 24;
        diagnosisTop = departmentBaseline + 10;
        double diagnosisHeight = paragraph(null,new Paragraph("- Chẩn đoán: ",data.diagnosisText(),UngDungPhongChu.TO_DIEU_TRI_TEXT),0,0,744);
        tableY = Math.max(TABLE_TOP,diagnosisTop + (int)Math.ceil(diagnosisHeight) + 20);
        double progressHeight = renderParagraphs(null,progress,0,0,PROGRESS_WIDTH-16);
        double orderHeight = renderParagraphs(null,orders,0,0,ORDERS_WIDTH-16);
        int textY = tableY+34;
        leftSignature = new Rectangle2D.Double(TABLE_X+DATE_WIDTH+12,textY+progressHeight+8,PROGRESS_WIDTH-24,78);
        rightSignature = new Rectangle2D.Double(TABLE_X+DATE_WIDTH+PROGRESS_WIDTH+12,textY+orderHeight+8,ORDERS_WIDTH-24,78);
        bodyHeight = Math.max(372,(int)Math.ceil(Math.max(leftSignature.getMaxY(),rightSignature.getMaxY())+43-(tableY+28)));
        pageHeight = Math.max(HEIGHT,tableY+28+bodyHeight+100);
    }
    void setDisplayScale(double scale) {
        displayScale = scale;
        setPreferredSize(new Dimension((int)Math.round(WIDTH*scale),(int)Math.round(pageHeight*scale)));
        setMinimumSize(getPreferredSize());setMaximumSize(getPreferredSize());
        revalidate();repaint();
    }
    public int referenceHeight() { return pageHeight; }
    public Rectangle2D leftSignatureBounds() { return (Rectangle2D)leftSignature.clone(); }
    public Rectangle2D rightSignatureBounds() { return (Rectangle2D)rightSignature.clone(); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics.create();g.scale(displayScale,displayScale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.fillRect(0,0,WIDTH,pageHeight);
        g.setColor(new Color(160,160,160));g.drawRect(0,0,WIDTH-1,pageHeight-1);
        g.setColor(Color.BLACK);
        text(g,"SỞ Y TẾ BẮC NINH",49,43,UngDungPhongChu.TO_DIEU_TRI_AGENCY);
        text(g,"BỆNH VIỆN SẢN NHI",49,71,UngDungPhongChu.TO_DIEU_TRI_AGENCY);
        centered(g,"TỜ ĐIỀU TRỊ",410,48,UngDungPhongChu.TO_DIEU_TRI_TITLE);
        centered(g,"Số: "+(data.sheetNumber().isBlank()?"_____":data.sheetNumber()),410,82,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        text(g,"MS: 3BBV-01",598,43,UngDungPhongChu.TO_DIEU_TRI_BOLD);
        text(g,"Số vào viện: "+data.visitId(),598,71,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        paragraph(g,new Paragraph("- Họ tên người bệnh: ",data.patientName(),UngDungPhongChu.TO_DIEU_TRI_BOLD),49,107,478);
        text(g,"Tuổi: "+data.ageText(),529,125,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        text(g,data.gender(),727,125,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        text(g,"- Khoa: "+data.departmentName(),49,departmentBaseline,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        text(g,"Buồng: "+data.roomName(),438,departmentBaseline,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        text(g,"Giường: "+data.bedName(),627,departmentBaseline,UngDungPhongChu.TO_DIEU_TRI_TEXT);
        paragraph(g,new Paragraph("- Chẩn đoán: ",data.diagnosisText(),UngDungPhongChu.TO_DIEU_TRI_TEXT),49,diagnosisTop,744);
        int tableWidth=DATE_WIDTH+PROGRESS_WIDTH+ORDERS_WIDTH;
        g.setStroke(new BasicStroke(1.2f));g.drawRect(TABLE_X,tableY,tableWidth,28+bodyHeight);
        g.drawLine(TABLE_X,tableY+28,TABLE_X+tableWidth,tableY+28);
        g.drawLine(TABLE_X+DATE_WIDTH,tableY,TABLE_X+DATE_WIDTH,tableY+28+bodyHeight);
        g.drawLine(TABLE_X+DATE_WIDTH+PROGRESS_WIDTH,tableY,TABLE_X+DATE_WIDTH+PROGRESS_WIDTH,tableY+28+bodyHeight);
        paragraph(g,new Paragraph("",data.examDate(),UngDungPhongChu.TO_DIEU_TRI_TEXT),TABLE_X+7,tableY+33,DATE_WIDTH-12);
        renderParagraphs(g,progress,TABLE_X+DATE_WIDTH+8,tableY+34,PROGRESS_WIDTH-16);
        renderParagraphs(g,orders,TABLE_X+DATE_WIDTH+PROGRESS_WIDTH+8,tableY+34,ORDERS_WIDTH-16);
        signature(g,leftSignature);signature(g,rightSignature);
        g.dispose();
    }
    private void signature(Graphics2D g,Rectangle2D r) {
        if(!isPaintingForPrint()) {
            g.setColor(new Color(45,0,255));g.setStroke(new BasicStroke(1.2f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{9,6},0));
            g.draw(r); centered(g,"Nhấp đúp chuột để ký",(int)r.getCenterX(),(int)r.getY()+44,UngDungPhongChu.TO_DIEU_TRI_UI);
        }
        g.setColor(Color.BLACK);centered(g,data.doctorName(),(int)r.getCenterX(),(int)r.getMaxY()+33,UngDungPhongChu.TO_DIEU_TRI_BOLD);
    }
    private double renderParagraphs(Graphics2D g,List<Paragraph> paragraphs,double x,double y,int width) {
        double original=y;
        for(Paragraph p:paragraphs)y+=paragraph(g,p,x,y,width);
        return y-original;
    }
    private double paragraph(Graphics2D g,Paragraph p,double x,double y,int width) {
        String full=p.prefix()+p.text();double initial=y;
        int offset=0;
        for(String line:full.split("\\R",-1)) {
            if(line.isEmpty()){y+=22;offset++;continue;}
            AttributedString attributed=new AttributedString(line);
            attributed.addAttribute(TextAttribute.FONT,p.font());
            int prefixLength=Math.max(0,Math.min(line.length(),p.prefix().length()-offset));
            if(prefixLength>0)attributed.addAttribute(TextAttribute.FONT,UngDungPhongChu.TO_DIEU_TRI_BOLD,0,prefixLength);
            LineBreakMeasurer measurer=new LineBreakMeasurer(attributed.getIterator(),metrics);
            while(measurer.getPosition()<line.length()) {
                TextLayout layout=measurer.nextLayout(width);
                if(layout==null)break;
                double ascent=layout.getAscent(), height=Math.max(22,ascent+layout.getDescent()+layout.getLeading());
                if(g!=null){g.setColor(Color.BLACK);layout.draw(g,(float)x,(float)(y+ascent));}
                y+=height;
            } offset+=line.length()+1;
        }
        return y-initial;
    }
    private static void text(Graphics2D g,String value,int x,int baseline,Font font) {
        g.setFont(font);g.drawString(value==null?"":value,x,baseline);
    }
    private static void centered(Graphics2D g,String text,int center,int baseline,Font font) {
        g.setFont(font);g.drawString(text,center-g.getFontMetrics().stringWidth(text)/2,baseline);
    }
}
