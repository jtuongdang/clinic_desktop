package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.inventory.create.CreateThuocDayDuHandler;
import vn.clinicmanagement.application.inventory.create.CreateMatHangBienNhan;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Biểu mẫu thuốc đầy đủ, các nhóm cùng nằm trên một màn hình laptop. */
public final class ThemThuocManHinh extends JPanel {
    private static final Font TEXT=new Font("Arial",Font.PLAIN,12);
    private final Map<String,JComponent> fields=new LinkedHashMap<>();
    private final JButton save=new JButton("▣  Lưu (F2)");
    private final JLabel status=new JLabel(" ");
    private final CreateThuocDayDuHandler handler;
    private final Integer editItemId;
    private final Runnable saved;
    private boolean saving;
    private final JPanel form=new JPanel();
    public ThemThuocManHinh(CreateThuocDayDuHandler handler,Runnable saved){
        this(handler,null,null,saved);
    }
    public ThemThuocManHinh(CreateThuocDayDuHandler handler,Integer editItemId,Map<String,String> initialValues,Runnable saved){
        super(new BorderLayout());this.handler=handler;this.editItemId=editItemId;this.saved=saved;
        setPreferredSize(new Dimension(1280,680));setBackground(new Color(249,249,249));
        form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));form.setBackground(getBackground());form.setBorder(new EmptyBorder(4,7,4,7));
        form.add(section("Thông tin cơ bản về thuốc"));
        form.add(row(field("maThuoc","Mã thuốc","code",1),field("maThuocBHYT","Mã thuốc BHYT","text",1),field("tenThuoc","Tên thuốc","text",4),field("donViTinh","Đơn vị tính","unit",1),field("donViSuDung","Đơn vị sử dụng","combo",1),field("hamLuong","Hàm lượng","text",1),field("nongDo","Nồng độ","text",1),field("theTich","Thể tích","text",1),field("dungMoi","Dung môi","combo",1)));
        form.add(row(field("maHoatChat","Mã hoạt chất","text",2),field("tenHoatChat","Tên hoạt chất","combo",4),field("duongDung","Đường dùng","combo",2),field("huongDan","Hướng dẫn sử dụng","text",4)));
        form.add(row(field("nuocSanXuat","Nước sản xuất","combo",2),field("hangSanXuat","Hãng sản xuất","combo",2),field("soDangKy","Số đăng ký","text",2),field("quyCach","Quy cách đóng gói","text",2),field("bietDuoc","Biệt dược","combo",1),field("soLoNhap","Số lô nhập","text",1),field("hanSuDung","Hạn sử dụng","date",2)));
        form.add(row(field("maVach","Mã vạch","text",1),field("maATC","Mã ATC","text",1),field("soQuyetDinh","Số quyết định trúng thầu","text",2),field("soGoiThau","Số gói thầu","text",2),field("nhomThau","Nhóm thầu","text",1),field("namThau","Năm thầu","text",1),field("maDuocQuocGia","Mã dược quốc gia","text",2),field("maTuongDuong","Mã tương đương","text",1),field("maDanhMucYHCT","Mã danh mục YHCT","text",1)));
        form.add(section("Thông tin kế toán, xuất nhập kho"));
        form.add(row(field("nhomDichVu","Nhóm dịch vụ","drug",2),field("loaiDichVu","Loại dịch vụ","group",2),field("nhomBHYT","Nhóm chi phí BHYT","insurance",2),field("nhomTaiKhoan","Nhóm tài khoản thu tiền","combo",2),field("nhomBaoCao","Nhóm báo cáo","combo",2),field("doiTuongMacDinh","Luôn chọn đối tượng khi chỉ định","combo",2)));
        form.add(row(field("giaBHYT","Giá BHYT","number",1),field("giaVienPhi","Giá viện phí","number",1),field("giaYeuCau","Giá yêu cầu","number",1),field("giaNuocNgoai","Giá nước ngoài","number",1),field("tyLeDungTuyen","Tỷ lệ thanh toán đúng tuyến (%)","percent",2),field("tyLeTraiTuyen","Tỷ lệ thanh toán trái tuyến (%)","percent",2),space(4)));
        form.add(row(field("giaMua","Giá mua","number",1),field("vatMua","Giá mua VAT (%)","number",1),field("giaBan","Giá bán","number",1),field("vatBan","Giá bán VAT (%)","number",1),field("canhBaoHSD","Cảnh báo HSD","integer",1),field("canhBaoTon","Cảnh báo tồn kho","integer",1),field("canhBaoKhaDung","Cảnh báo khả dụng","integer",2),field("canhBaoNgay","Cảnh báo sử dụng sau số ngày","integer",2),field("nhomHoaHong","Nhóm hoa hồng người giới thiệu","share",2)));
        form.add(section("Thông tin lâm sàng"));
        form.add(row(field("nhomQuanLy","Nhóm quản lý","combo",2),field("nhomNghienCuu","Nhóm nghiên cứu","combo",2),field("nhomBenhLy","Nhóm bệnh lý","combo",2),field("nhomPhacDo","Nhóm phác đồ","combo",2),field("nhomABCVEN","Nhóm ABC/VEN","combo",2),space(2)));
        form.add(row(field("nhomDuocLy","Nhóm dược lý","combo",2),field("tieuNhomDuocLy","Tiểu nhóm dược lý","combo",2),field("nhomQuyChe","Nhóm quy chế","combo",2),field("nhomLoiNhuan","Nhóm chia lợi nhuận","share",2),field("thuocDiKem","Thuốc đi kèm","text",2),field("hoatChatDiKem","Hoạt chất đi kèm","text",2)));
        form.add(section("Thông tin khác"));
        form.add(checkRow(new String[][]{{"khangSinh","Thuốc kháng sinh"},{"tanDuoc","Thuốc tân dược"},{"keDon","Thuốc kê đơn"},{"keDonTreEm","Thuốc kê đơn trẻ em"},{"viThuocYHCT","Vị thuốc y học cổ truyền"},{"chePhamYHCT","Chế phẩm y học cổ truyền"}}));
        form.add(checkRow(new String[][]{{"danhSTT","Đánh STT trong tờ điều trị"},{"thucPhamChucNang","Thực phẩm chức năng"},{"taiTro","Thuốc tài trợ"},{"lamTron","Tự động làm tròn số lượng"},{"keTheoLieu","Kê theo liều dùng/thể tích"}}));
        form.add(checkRow(new String[][]{{"keTheTichDiKem","Kê theo thể tích cho dịch truyền đi kèm"},{"khoa","Khóa"}}));
        JPanel upper=new JPanel(new BorderLayout());upper.setBackground(getBackground());upper.add(form,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(upper);scroll.setBorder(null);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(24);add(scroll,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setOpaque(false);footer.setPreferredSize(new Dimension(100,43));footer.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,new Color(180,180,190)),new EmptyBorder(4,7,4,7)));
        JButton menu=new JButton("☰");menu.setForeground(new Color(35,110,190));menu.setFont(TEXT.deriveFont(22f));menu.setBorderPainted(false);menu.setContentAreaFilled(false);footer.add(menu,BorderLayout.WEST);
        status.setFont(TEXT);footer.add(status,BorderLayout.CENTER);save.setFont(TEXT.deriveFont(Font.BOLD,14f));save.addActionListener(e->save());save.setEnabled(handler!=null);footer.add(save,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);
        JPopupMenu actions=new JPopupMenu();JMenuItem close=new JMenuItem("Đóng");close.addActionListener(e->{Window window=SwingUtilities.getWindowAncestor(this);if(window!=null)window.dispose();});actions.add(close);menu.addActionListener(e->{if(menu.isShowing())actions.show(menu,0,-actions.getPreferredSize().height);});
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F2"),"save");getActionMap().put("save",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){save();}});
        if(initialValues!=null&&!initialValues.isEmpty())applyValues(initialValues);
    }
    private void applyValues(Map<String,String> values){
        fields.forEach((key,input)->{
            if(!values.containsKey(key))return;
            String val=Objects.toString(values.get(key),"");
            if(input instanceof JTextField text)text.setText(val);
            else if(input instanceof JComboBox<?> combo){
                if(combo.isEditable())combo.getEditor().setItem(val);
                else combo.setSelectedItem(val);
            }else if(input instanceof JCheckBox box)box.setSelected(Boolean.parseBoolean(val));
        });
    }
    private JLabel section(String title){JLabel label=new JLabel(title);label.setFont(TEXT.deriveFont(Font.BOLD,16f));label.setBorder(new EmptyBorder(3,0,2,0));label.setAlignmentX(LEFT_ALIGNMENT);label.setMaximumSize(new Dimension(Integer.MAX_VALUE,25));label.setPreferredSize(new Dimension(100,25));return label;}
    private record Cell(JPanel panel,int width){}
    private Cell space(int width){JPanel blank=new JPanel();blank.setOpaque(false);return new Cell(blank,width);}
    private Cell field(String key,String title,String type,int width){
        JPanel cell=new JPanel(new BorderLayout(0,2));cell.setOpaque(false);cell.setBorder(new EmptyBorder(0,2,2,3));JLabel label=new JLabel(title);label.setFont(TEXT);cell.add(label,BorderLayout.NORTH);
        JComponent input;
        if(Set.of("combo","unit","group","insurance","share","drug").contains(type)){
            String[] values=switch(type){case "unit"->new String[]{"","Viên","Gói","Ống","Lọ","Chai","Hộp","Vỉ","ml","mg"};case "group"->new String[]{"","Thuốc viên","Thuốc khác","Thuốc Hen","Thuốc Hô Hấp","Thuốc Kháng Viêm","Thuốc Hạ Nhiệt - Giảm Đau","Thuốc Nhỏ Mũi","Thuốc Nhỏ Tai","Thuốc Dị Ứng","Thuốc Tiêu Hóa","Thuốc Kháng Sinh"};case "insurance"->new String[]{"Chưa xác định","Trong danh mục BHYT","Ngoài danh mục BHYT"};case "share"->new String[]{"(Không chia)"};case "drug"->new String[]{"Thuốc"};default->new String[]{""};};
            JComboBox<String> combo=new JComboBox<>(values);combo.setEditable(!Set.of("insurance","drug").contains(type));combo.setFont(TEXT.deriveFont(13f));input=combo;
        }else{
            JTextField text=new JTextField();text.setFont(TEXT.deriveFont(13f));
            if(Set.of("number","percent","integer").contains(type)){text.setHorizontalAlignment(SwingConstants.RIGHT);text.setText("percent".equals(type)?"100.00":"integer".equals(type)?"0":"0.00");}
            if("code".equals(type)){text.setText(nextCode());text.setForeground(new Color(190,0,0));text.setFont(TEXT.deriveFont(Font.BOLD,13f));}
            if("date".equals(type))text.setToolTipText("dd/MM/yyyy");
            if("hamLuong".equals(key))text.setBackground(new Color(255,255,190));
            input=text;
        }
        input.setName(key);input.setMinimumSize(new Dimension(0,24));input.setPreferredSize(new Dimension(80,24));input.getAccessibleContext().setAccessibleName(title);fields.put(key,input);
        if("code".equals(type)){JPanel editor=new JPanel(new BorderLayout(1,0));editor.setOpaque(false);JButton reset=new JButton("↻");reset.setFont(TEXT.deriveFont(14f));reset.setMargin(new Insets(0,0,0,0));reset.setPreferredSize(new Dimension(22,24));reset.setToolTipText("Tạo mã thuốc mới");reset.addActionListener(e->((JTextField)input).setText(nextCode()));editor.add(input,BorderLayout.CENTER);editor.add(reset,BorderLayout.EAST);cell.add(editor,BorderLayout.CENTER);}
        else cell.add(input,BorderLayout.CENTER);
        return new Cell(cell,width);
    }
    private JPanel row(Cell...cells){
        JPanel row=new JPanel(new GridBagLayout());row.setOpaque(false);row.setAlignmentX(LEFT_ALIGNMENT);row.setPreferredSize(new Dimension(100,46));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));row.setMinimumSize(new Dimension(0,46));
        int x=0;for(Cell cell:cells){GridBagConstraints c=new GridBagConstraints();c.gridx=x;c.gridy=0;c.gridwidth=cell.width();c.weightx=cell.width();c.weighty=1;c.fill=GridBagConstraints.BOTH;cell.panel().setMinimumSize(new Dimension(0,42));row.add(cell.panel(),c);x+=cell.width();}return row;
    }
    private JPanel checkRow(String[][] items){
        JPanel row=new JPanel(new GridLayout(1,items.length==2?3:6,3,0));row.setOpaque(false);row.setAlignmentX(LEFT_ALIGNMENT);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,26));row.setPreferredSize(new Dimension(100,26));
        for(String[] item:items){JCheckBox input=new JCheckBox(item[1]);input.setFont(TEXT);input.setOpaque(false);input.setName(item[0]);input.setToolTipText(item[1]);fields.put(item[0],input);row.add(input);}
        for(int i=items.length;i<(items.length==2?3:6);i++)row.add(new JLabel());return row;
    }
    private static String nextCode(){return "THU"+java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmssSSS"));}
    public Map<String,String> collectValues(){
        Map<String,String> result=new LinkedHashMap<>();fields.forEach((key,input)->{
            String value=input instanceof JTextField text?text.getText():input instanceof JComboBox<?> combo?Objects.toString(combo.isEditable()?combo.getEditor().getItem():combo.getSelectedItem(),""):input instanceof JCheckBox box?Boolean.toString(box.isSelected()):"";result.put(key,value.trim());
        });return Map.copyOf(result);
    }
    private void save(){
        if(saving||handler==null)return;Map<String,String> values=collectValues();var auth=KhamBenhPhienNguCanh.current();saving=true;save.setEnabled(false);status.setText("Đang lưu thuốc...");
        new SwingWorker<KetQua<CreateMatHangBienNhan>,Void>(){
            protected KetQua<CreateMatHangBienNhan> doInBackground(){return editItemId!=null?handler.handleUpdate(editItemId,values,auth):handler.handle(values,auth);}
            protected void done(){
                try{var result=get();if(result.isFailure()){status.setForeground(new Color(170,0,0));status.setText(result.getError().orElseThrow().message());}
                    else{if(saved!=null)saved.run();Window window=SwingUtilities.getWindowAncestor(ThemThuocManHinh.this);if(window!=null)window.dispose();status.setForeground(new Color(0,120,0));status.setText("Đã lưu thuốc "+values.get("maThuoc"));}}
                catch(Exception ex){status.setForeground(new Color(170,0,0));status.setText("Không thể hoàn tất lưu thuốc. Vui lòng kiểm tra danh mục trước khi thử lại.");}
                finally{saving=false;save.setEnabled(true);}
            }
        }.execute();
    }
    public JComponent getField(String key){return fields.get(key);}
    public JPanel getForm(){return form;}
    public JButton getSaveButton(){return save;}
}
