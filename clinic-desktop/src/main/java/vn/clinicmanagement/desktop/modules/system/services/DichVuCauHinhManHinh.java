package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.application.catalog.DichVuCauHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** First tab follows the 1586px service-configuration reference, with proportional fields and scrolling. */
public final class DichVuCauHinhManHinh extends JPanel {
    private static final String[] TABS={"Thông Tin Dịch Vụ","Bảng Giá Yêu Cầu","Kết quả CLS","Chỉ số CLS","Hóa Chất Xét Nghiệm","Thông Tin Khác"};
    private static final Set<String> PRICES=Set.of("insurancePrice","hospitalPrice","servicePrice","foreignPrice");
    private final DichVuDanhMucNguon source;
    private final DichVuHang item;
    private final Consumer<DichVuCauHinh> saved;
    @FunctionalInterface interface MaterialEditor {
        void open(String definition,String legacy,Consumer<vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh> accepted);
    }
    private final MaterialEditor materialEditor;
    final Map<String,JButton> pickers=new LinkedHashMap<>();
    final JTabbedPane tabs=new JTabbedPane();
    final Form form=new Form();
    private final ChiSoClsCauHinhManHinh labIndices=new ChiSoClsCauHinhManHinh();
    private final KetQuaClsCauHinhManHinh labResults;
    private final HoaChatXetNghiemCauHinhManHinh labReagents;
    private final DichVuCauHinhTabForm prices;
    private final DichVuCauHinhTabForm otherInformation;
    final Map<String,JComponent> fields=new LinkedHashMap<>();
    final JButton save=new JButton("Lưu",saveIcon());
    final JLabel status=new JLabel("Đang tải thông tin dịch vụ…");
    private DichVuCauHinh loaded;
    private Map<String,String> baseline=Map.of();
    private boolean loading=true,saving,disposed;

    public DichVuCauHinhManHinh(DichVuHang item,DichVuDanhMucNguon source,Consumer<DichVuCauHinh> saved){
        this(item,source,saved,null);
    }
    DichVuCauHinhManHinh(DichVuHang item,DichVuDanhMucNguon source,Consumer<DichVuCauHinh> saved,MaterialEditor materialEditor){
        super(new BorderLayout());this.item=item;this.source=source;this.saved=saved;
        this.materialEditor=materialEditor!=null?materialEditor:(definition,legacy,accepted)->{
            if(!GraphicsEnvironment.isHeadless())new VatTuHaoPhiHopThoai(SwingUtilities.getWindowAncestor(this),source,definition,legacy,accepted).setVisible(true);
        };
        setName("serviceConfiguration");setBackground(new Color(249,249,249));buildForm();
        labResults=new KetQuaClsCauHinhManHinh(fields);
        labReagents=new HoaChatXetNghiemCauHinhManHinh(this::openMaterialEditor);
        prices=DichVuCauHinhTabForm.prices(fields);
        otherInformation=DichVuCauHinhTabForm.otherInformation(fields);
        tabs.setUI(new ReferenceTabs());tabs.setFont(UngDungPhongChu.DICH_VU_CAU_HINH_BOLD.deriveFont(java.awt.geom.AffineTransform.getScaleInstance(.90,1)));
        tabs.addTab(TABS[0],new JScrollPane(form,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        ((JScrollPane)tabs.getComponentAt(0)).setBorder(null);
        tabs.addTab(TABS[1],prices);
        tabs.addTab(TABS[2],labResults);
        tabs.addTab(TABS[3],labIndices);
        tabs.addTab(TABS[4],labReagents);
        tabs.addTab(TABS[5],otherInformation);
        add(tabs);
        JPanel footer=new JPanel(new BorderLayout());footer.setBackground(new Color(249,249,249));
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,new Color(160,160,160)));footer.setPreferredSize(new Dimension(100,62));
        status.setFont(UngDungPhongChu.BODY);status.setForeground(new Color(170,45,25));status.setBorder(new EmptyBorder(5,10,5,10));footer.add(status);
        save.setName("saveServiceConfiguration");save.setFont(UngDungPhongChu.DICH_VU_CAU_HINH);save.setContentAreaFilled(false);
        save.setBorder(new EmptyBorder(6,12,6,36));save.setPreferredSize(new Dimension(130,60));save.setFocusPainted(false);save.setEnabled(false);
        save.addActionListener(e->save());footer.add(save,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control S"),"saveService");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta S"),"saveService");
        getActionMap().put("saveService",new AbstractAction(){public void actionPerformed(ActionEvent e){save();}});
        setControlsEnabled(false);load();
    }

    private void buildForm(){
        fields.put("consumedMaterialsDefinition",new JTextField());fields.put("consumedMaterialsLegacy",new JTextField());
        text("code","Mã dịch vụ",10,8,151,30);text("shortcut","Phím tắt",170,8,147,30);
        text("name","Tên dịch vụ bệnh viện",326,8,618,30);text("serviceNumber","STT dịch vụ",954,8,148,30);
        text("unit","Đơn vị tính",1111,8,149,30);text("insuranceCode","Mã dịch vụ BHYT",1270,8,150,30);text("specialtyCode","Mã khám CK",1430,8,146,30);
        text("insuranceMaterialCode","Mã vật tư đi kèm BHYT",10,67,307,88);text("insuranceName","Tên dịch vụ BHYT",326,67,303,88);
        text("insuranceNumber","STT dịch vụ BHYT",637,67,149,88);text("sortNumber","STT sắp xếp",795,67,149,88);
        combo("category","Nhóm dịch vụ",954,67,307,88,"Khám bệnh","Xét nghiệm","Chẩn đoán hình ảnh","Chuyên khoa");combo("group","Loại dịch vụ",1270,67,306,88);
        text("indexCode","Mã chỉ số",10,128,151,149);text("indexName","Tên chỉ số",170,128,459,149);
        combo("ministryType","Loại dịch vụ (Bộ y tế)",10,190,307,213);combo("forcedPatientType","Luôn chọn đối tượng này khi chỉ định",326,190,303,213);
        combo("insuranceCostGroup","Nhóm chi phí BHYT",637,190,307,213);combo("reportGroup","Nhóm báo cáo",954,190,307,213);combo("accountGroup","Nhóm tài khoản thu tiền",1270,190,306,213);
        text("note","Ghi chú",10,253,307,277);combo("effectiveDate","Ngày áp giá",326,253,303,277);
        combo("priceBasis","Hình thức áp giá",637,253,307,277,"Ngày nhập viện","Ngày chỉ định","Ngày thực hiện");
        picker("paymentExemptServices","Miễn thanh toán khi có chỉ định dịch vụ",954,253,307,277);picker("insuredPatientTypes","Chỉ đối tượng này mới được BHTT",1270,253,306,277);
        text("insurancePrice","Giá BHYT",10,316,151,341);text("hospitalPrice","Giá viện phí",170,316,147,341);text("servicePrice","Giá dịch vụ",326,316,148,341);text("foreignPrice","Giá nước ngoài",481,316,148,341);
        text("inNetworkPercent","% TT đúng tuyến",637,316,149,341);text("outNetworkPercent","% TT trái tuyến",795,316,149,341);
        text("insuranceLimit","Định mức BH trả",954,316,153,341);text("materialLimit","Định mức vật tư",1114,316,147,341);text("drugLimit","Định mức thuốc",1270,316,149,341);
        picker("blockedPatientConversion","Không cho phép chuyển đối tượng sau",10,380,307,405);text("defaultQuantity","Số lượng mặc định",326,380,148,405);text("filmQuantity","Số lượng phim",481,380,148,405);text("reuseDays","Số ngày tái sử dụng",637,380,149,405);
        text("duration","Thời gian thực hiện",795,380,105,405);label("phút",908,405,40,30);
        text("minimumDuration","Thời gian tối thiểu",954,380,105,405);label("phút",1066,405,40,30);combo("icd9","Mã ICD9",1114,380,148,405);
        picker("orderRooms","Phòng chỉ định",10,445,307,469);picker("executionRooms","Phòng thực hiện",326,445,303,469);
        picker("receptionRooms","Phòng đón tiếp, lấy mẫu, tiêm xạ hình",637,445,307,469);combo("executionArea","Khu thực hiện",954,445,307,469);picker("bundledServices","Dịch vụ đi kèm (thanh toán gộp)",1270,445,306,469);
        picker("separateServices","Dịch vụ đi kèm (thanh toán riêng)",10,508,307,533);picker("consumedDrugs","Thuốc hao phí",326,508,303,533);
        picker("separateDrugs","Thuốc đi kèm (thanh toán riêng)",637,508,307,533);picker("consumedMaterials","Vật tư hao phí",954,508,307,533);picker("separateMaterials","Vật tư đi kèm (thanh toán riêng)",1270,508,306,533);
        combo("surgeryType","Loại phẫu thuật, thủ thuật",10,572,307,592);combo("surgeryLevel","Tuyến phẫu thuật, thủ thuật",326,572,303,592);combo("surgeryTeam","Ê-Kíp phẫu thuật, thủ thuật",637,572,307,592);
        combo("profitGroup","Nhóm chia lợi nhuận",954,572,307,592,"(Không chia)");combo("referralGroup","Nhóm chia hoa hồng người giới thiệu",1270,572,306,592,"(Không chia)");
        picker("orderTemplate","Mẫu phiếu chỉ định",10,628,307,650);picker("resultTemplate","Mẫu phiếu trả kết quả",326,628,303,650);
        combo("resultType","Loại mẫu kết quả",637,628,307,650,"Mặc định");text("imagingName","Tên CĐHA",954,628,167,650);text("labAbbreviation","Viết tắt XN",1132,628,129,650);picker("serviceStages","Công đoạn dịch vụ",1270,628,306,650);

        check("servicePackage","Gói dịch vụ kỹ thuật",10,694,225);check("printOnTreatment","In trên phiếu điều trị",10,724,225);
        check("allowLateResults","Cho phép thời gian trả kết quả lớn hơn thời gian ra viện",10,754,506);check("consultationRecord","Yêu cầu biên bản hội chẩn",10,784,232);
        check("noSeparateResult","Không in phiếu kết quả riêng",245,694,273);check("editPriceAtExecution","Cho sửa giá dịch vụ khi thực hiện",245,724,273);check("epidemiology","Cho phép nhập thông tin dịch tễ",245,784,273);
        check("editExecutionQuantity","Cho sửa số lượng ở phòng thực hiện",523,694,320);check("allowDiscount","Cho phép miễn giảm",523,724,320);check("multiplePayments","Thu tiền nhiều lần",523,754,178);
        check("consultation","Cần hội chẩn",704,754,140);check("biometricReception","Yêu cầu xác thực sinh trắc khi đón tiếp",523,784,320);
        check("editPriceAtOrder","Cho sửa giá dịch vụ khi chỉ định",847,694,356);check("dischargeBeforeResults","Cho phép xử trí ra viện khi chưa có kết quả",847,724,356);
        check("quantityByDuration","Tính số lượng theo thời gian thực hiện",847,754,356);check("skipTimeOverlapCheck","Không kiểm tra trùng thời gian thực hiện",847,784,356);
        check("birthCertificate","Cấp giấy chứng sinh",1207,694,190);check("allowReplacement","Cho thay thế dịch vụ",1401,694,175);
        check("blockCancellation","Không cho phép hủy dịch vụ khi đã chỉ định",1207,724,369);check("requireResultImages","Chặn trả kết quả khi chưa có hình ảnh",1207,754,369);
        check("locked","Khóa dịch vụ",1207,784,140);check("covidTest","Test COVID19",1353,784,222);
        for(String key:List.of("insurancePrice","hospitalPrice","servicePrice","foreignPrice","inNetworkPercent","outNetworkPercent","insuranceLimit","materialLimit","drugLimit","defaultQuantity","filmQuantity","reuseDays","duration","minimumDuration"))
            ((JTextField)fields.get(key)).setHorizontalAlignment(SwingConstants.RIGHT);
        for(String key:List.of("insurancePrice","hospitalPrice","servicePrice","foreignPrice","inNetworkPercent","outNetworkPercent")){
            var input=(JTextField)fields.get(key);input.putClientProperty("regularPrice",true);input.setForeground(new Color(100,100,100));
        }
    }

    private void label(String title,int x,int y,int w,int h){var label=new JLabel(title);label.setForeground(Color.BLACK);label.setFont(UngDungPhongChu.DICH_VU_CAU_HINH);form.place(label,x,y,w,h);}
    private JTextField text(String key,String title,int x,int y,int w,int fieldY){
        label(title,x,y,Set.of("duration","minimumDuration").contains(key)?153:w,22);JTextField input=new JTextField();input.setForeground(Color.BLACK);input.setFont(UngDungPhongChu.DICH_VU_CAU_HINH_BOLD);
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(130,133,136)),new EmptyBorder(0,3,0,3)));
        input.setName(key);input.getAccessibleContext().setAccessibleName(title);fields.put(key,input);form.place(input,x,fieldY,w,30);return input;
    }
    private void combo(String key,String title,int x,int y,int w,int fieldY,String...options){
        label(title,x,y,w,22);JComboBox<String> input=new JComboBox<>();input.addItem("");for(String option:options)input.addItem(option);
        input.setEditable(!key.equals("category"));input.setFont(UngDungPhongChu.DICH_VU_CAU_HINH);input.setBackground(Color.WHITE);input.setName(key);
        input.getAccessibleContext().setAccessibleName(title);fields.put(key,input);form.place(input,x,fieldY,w,30);
    }
    private void picker(String key,String title,int x,int y,int w,int fieldY){
        JTextField input=text(key,title,x,y,w-23,fieldY);JButton more=new JButton("…");more.setFont(UngDungPhongChu.BODY_BOLD);
        more.setName(key+"Picker");pickers.put(key,more);
        more.setMargin(new Insets(0,0,0,0));more.setFocusable(false);more.setToolTipText("Chỉnh sửa "+title.toLowerCase(Locale.ROOT));
        more.addActionListener(e->{
            if("consumedMaterials".equals(key)){
                openMaterialEditor();
                return;
            }
            JTextArea editor=new JTextArea(input.getText(),8,55);editor.setLineWrap(true);editor.setWrapStyleWord(true);
            if(JOptionPane.showConfirmDialog(this,new JScrollPane(editor),title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)input.setText(editor.getText());});
        if("consumedMaterials".equals(key))input.setEditable(false);
        form.place(more,x+w-23,fieldY,23,30);
    }
    private void openMaterialEditor() {
        if (loading || saving || disposed || !source.canSaveDetail()) return;
        var definition = (JTextField) fields.get("consumedMaterialsDefinition");
        var legacy = (JTextField) fields.get("consumedMaterialsLegacy");
        var summary = (JTextField) fields.get("consumedMaterials");
        String oldSummary = legacy.getText().isBlank() ? summary.getText() : legacy.getText();
        materialEditor.open(definition.getText(), oldSummary, value -> {
            if (definition.getText().isBlank() && legacy.getText().isBlank() && !summary.getText().isBlank()) {
                legacy.setText(summary.getText());
            }
            definition.setText(value.encode());
            summary.setText(value.summary());
            summary.setCaretPosition(0);
            refreshReagents();
            status.setText("Đã chọn " + value.lines().size() + " vật tư hao phí. Nhấn Lưu dịch vụ để ghi nhận cấu hình.");
        });
    }
    private void refreshReagents() {
        var definition = (JTextField) fields.get("consumedMaterialsDefinition");
        var legacy = (JTextField) fields.get("consumedMaterialsLegacy");
        var summary = (JTextField) fields.get("consumedMaterials");
        labReagents.showDefinition(definition.getText(), legacy.getText().isBlank() ? summary.getText() : legacy.getText());
    }
    private void check(String key,String title,int x,int y,int w){
        JCheckBox input=new JCheckBox(title);input.setForeground(Color.BLACK);input.setFont(UngDungPhongChu.DICH_VU_CAU_HINH);input.setOpaque(false);input.setMargin(new Insets(0,0,0,0));
        input.setIcon(checkIcon(false));input.setSelectedIcon(checkIcon(true));input.setIconTextGap(5);input.setName(key);fields.put(key,input);form.place(input,x,y,w,25);
    }
    private void load(){
        new SwingWorker<DichVuCauHinh,Void>(){
            protected DichVuCauHinh doInBackground(){return source.loadDetail(item);}
            protected void done(){if(disposed)return;try{loaded=get();apply(loaded);loading=false;setControlsEnabled(true);save.setEnabled(source.canSaveDetail());status.setText(" ");}
                catch(Exception ex){loading=false;status.setText("Không tải được cấu hình. Đóng và mở lại dịch vụ để thử lại.");}}
        }.execute();
    }
    private void apply(DichVuCauHinh value){
        labIndices.load(value);
        for(var entry:fields.entrySet()){
            String text=value.value(entry.getKey());JComponent component=entry.getValue();
            if(PRICES.contains(entry.getKey())&&!text.isBlank()){
                var number=NumberFormat.getNumberInstance(Locale.US);number.setMaximumFractionDigits(2);text=number.format(DichVuCauHinh.number(text));
            }
            if(component instanceof JTextComponent input)input.setText(text);
            else if(component instanceof JComboBox<?> input)((JComboBox<String>)input).setSelectedItem(text);
            else if(component instanceof JCheckBox input)input.setSelected(Boolean.parseBoolean(text));
        }
        refreshReagents();
        baseline=snapshotValues();
    }
    Map<String,String> snapshotValues(){
        Map<String,String> values=new LinkedHashMap<>(loaded==null?Map.of():loaded.values());
        fields.forEach((key,component)->{
            String text=component instanceof JTextComponent input?input.getText():component instanceof JCheckBox check?String.valueOf(check.isSelected()):Objects.toString(((JComboBox<?>)component).isEditable()?((JComboBox<?>)component).getEditor().getItem():((JComboBox<?>)component).getSelectedItem(),"");
            if (!(component instanceof JTextArea) || !text.isBlank() || values.containsKey(key)) values.put(key,text.trim());
        });
        labIndices.copyValuesTo(values);
        return values;
    }
    public void save(){
        if(loading||saving||disposed||loaded==null||!source.canSaveDetail())return;
        if(!labIndices.finishEditing())return;
        DichVuCauHinh value=new DichVuCauHinh(loaded.id(),loaded.version(),snapshotValues());
        try{value.validate();}catch(IllegalArgumentException ex){status.setText(ex.getMessage());return;}
        var actor=KhamBenhPhienNguCanh.current();saving=true;save.setEnabled(false);setControlsEnabled(false);status.setText("Đang lưu cấu hình…");
        new SwingWorker<vn.clinicmanagement.application.common.KetQua<DichVuCauHinh>,Void>(){
            protected vn.clinicmanagement.application.common.KetQua<DichVuCauHinh> doInBackground(){return source.saveDetail(value,actor);}
            protected void done(){if(disposed)return;saving=false;setControlsEnabled(true);save.setEnabled(source.canSaveDetail());
                try{var result=get();if(result.isSuccess()){loaded=result.valueOrThrow();apply(loaded);status.setText("Đã lưu thông tin dịch vụ.");saved.accept(loaded);}
                    else status.setText(result.getError().orElseThrow().message());}
                catch(Exception ex){status.setText("Không lưu được cấu hình. Nội dung đang nhập vẫn được giữ.");}
            }
        }.execute();
    }
    private void setControlsEnabled(boolean enabled){
        for(Component child:form.getComponents())if(!(child instanceof JLabel))child.setEnabled(enabled);
        boolean writable=enabled&&source.canSaveDetail();
        prices.setEditable(writable);
        labResults.setEditable(writable);
        labIndices.setEditable(writable);
        labReagents.setEditable(writable);
        otherInformation.setEditable(writable);
    }
    public boolean isDirty(){return loaded!=null&&!baseline.equals(snapshotValues());}
    public boolean canClose(){
        if(saving){status.setText("Đang lưu cấu hình. Vui lòng chờ hoàn tất.");return false;}
        if(!labIndices.finishEditing())return false;
        return !isDirty()||(!GraphicsEnvironment.isHeadless()&&JOptionPane.showConfirmDialog(this,"Đóng và bỏ những thay đổi chưa lưu?","Cập Nhật Dịch Vụ",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);
    }
    public void dispose(){disposed=true;}

    static final class Form extends JPanel implements Scrollable {
        final Map<JComponent,Rectangle> positions=new LinkedHashMap<>();
        Form(){super(null);setBackground(new Color(249,249,249));}
        void place(JComponent component,int x,int y,int width,int height){positions.put(component,new Rectangle(x,y,width,height));add(component);}
        double scale(){return Math.max(.68,Math.min(1.25,getWidth()/1586.0));}
        @Override public void doLayout(){double scale=scale();positions.forEach((c,r)->{
            c.setBounds((int)Math.round(r.x*scale),(int)Math.round(r.y*scale),(int)Math.round(r.width*scale),(int)Math.round(r.height*scale));
            if(c.getFont()!=null){
                Font base=c instanceof JTextField&&!Boolean.TRUE.equals(c.getClientProperty("regularPrice"))?UngDungPhongChu.DICH_VU_CAU_HINH_BOLD:UngDungPhongChu.DICH_VU_CAU_HINH;
                Font font=base.deriveFont((float)Math.max(12,17*scale));
                if(c instanceof JLabel||c instanceof JCheckBox)font=font.deriveFont(java.awt.geom.AffineTransform.getScaleInstance(.90,1));
                c.setFont(font);
            }
        });}
        @Override public Dimension getPreferredSize(){int width=getParent() instanceof JViewport v?v.getWidth():1586;return new Dimension(width,(int)Math.ceil(856*Math.max(.68,Math.min(1.25,width/1586.0))));}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(1586,856);}
        public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 25;}
        public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return r.height-25;}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private static final class ReferenceTabs extends BasicTabbedPaneUI {
        @Override protected void installDefaults(){super.installDefaults();tabInsets=new Insets(6,15,6,15);contentBorderInsets=new Insets(0,0,0,0);tabAreaInsets=new Insets(4,0,0,0);selectedTabPadInsets=new Insets(0,0,0,0);}
        @Override protected int calculateTabWidth(int placement,int index,FontMetrics metrics){return super.calculateTabWidth(placement,index,metrics)+(index==0?24:0);}
        @Override protected void paintTabBackground(Graphics graphics,int placement,int index,int x,int y,int w,int h,boolean selected){
            Graphics2D g=(Graphics2D)graphics.create();g.setPaint(new GradientPaint(0,y,selected?new Color(250,252,254):new Color(222,239,253),0,y+h,new Color(177,205,228)));
            if(index==0)g.fillPolygon(new int[]{x,x+28,x+w,x+w},new int[]{y+h,y,y,y+h+6},4);
            else g.fillRoundRect(x,y,w,h+6,9,9);g.dispose();
        }
        @Override protected void paintTabBorder(Graphics g,int placement,int index,int x,int y,int w,int h,boolean selected){g.setColor(new Color(125,151,172));
            if(index==0){g.drawLine(x,y+h,x+28,y);g.drawLine(x+28,y,x+w-1,y);g.drawLine(x+w-1,y,x+w-1,y+h);}
            else g.drawRoundRect(x,y,w-1,h+6,9,9);}
        @Override protected void paintContentBorder(Graphics g,int placement,int selected){g.setColor(new Color(142,160,174));g.drawLine(0,calculateTabAreaHeight(placement,runCount,maxTabHeight),tabPane.getWidth(),calculateTabAreaHeight(placement,runCount,maxTabHeight));}
    }
    private static Icon checkIcon(boolean selected){return new Icon(){public int getIconWidth(){return 14;}public int getIconHeight(){return 14;}
        public void paintIcon(Component c,Graphics g,int x,int y){g.setColor(Color.WHITE);g.fillRect(x,y,13,13);g.setColor(new Color(145,145,145));g.drawRect(x,y,13,13);
            if(selected){Graphics2D copy=(Graphics2D)g.create();copy.setColor(Color.BLACK);copy.setStroke(new BasicStroke(2));copy.drawLine(x+2,y+7,x+5,y+11);copy.drawLine(x+5,y+11,x+12,y+2);copy.dispose();}}};}
    static Icon saveIcon(){return new Icon(){public int getIconWidth(){return 38;}public int getIconHeight(){return 38;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.setColor(new Color(112,55,145));g.fillRoundRect(2,2,34,34,2,2);
            g.setColor(Color.WHITE);g.fillRect(8,3,19,12);g.fillRect(8,22,22,12);g.setColor(new Color(112,55,145));g.fillRect(20,5,4,8);g.dispose();}};}
}
