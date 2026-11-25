package vn.clinicmanagement.desktop.modules.billing.examination;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.application.billing.*;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongBienNhan;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.text.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.function.*;
import static vn.clinicmanagement.desktop.modules.billing.examination.ThanhToanKhamBenhDuLieu.*;
import static vn.clinicmanagement.desktop.modules.billing.examination.ThanhToanBieuTuong.Kind.*;

/** Selected-visit billing window, based on the 1586 x 962 reference content area. */
public final class ThanhToanKhamBenhManHinh extends JPanel {
    @FunctionalInterface
    public interface PayerChangeHandler {
        KetQua<ChangeChiTietHoaDonDoiTuongBienNhan> changePayer(
                int invoiceId, int lineId, String targetPayerType,
                BigDecimal fallbackServicePrice, BigDecimal fallbackInsurancePrice, Integer overrideCoverageRate);
    }

    private static final Color BLUE=new Color(0,113,232), DARK_RED=new Color(140,0,0);
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"), DAY=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JTextField search=new JTextField();
    private final JTree tree=new JTree();
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Ngày Y Lệnh","","Mã Tương Đương","Tên Dịch Vụ, Thuốc, Vật Tư","Loại Dịch Vụ","Đối Tượng","Nguồn Chi Trả","Giá BHYT","Đơn Vị"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
    private final JTable table=new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
    private final JPanel toolbar=new JPanel(null), sidebar=new JPanel(new BorderLayout()),workspace=new JPanel(new BorderLayout()),footer=new JPanel(new BorderLayout());
    private final JPanel searchStrip=new JPanel(new BorderLayout()),warning=new JPanel(new BorderLayout()),summary=new JPanel(null);
    private final JLabel all=new JLabel(),warningText=new JLabel(),patient=new JLabel(),insurance=new JLabel(),address=new JLabel(),payer=new JLabel();
    private final List<JButton> toolButtons=new ArrayList<>();
    private final Map<String,JLabel> totals=new LinkedHashMap<>();
    private final JPopupMenu popup=new JPopupMenu();
    private final List<JMenuItem> payerActionItems=new ArrayList<>();
    private ThanhToanKhamBenhDuLieu data;
    private Predicate<Dong> treeFilter=row->true;
    private boolean errorsOnly, rebuilding;
    private double scale=1;
    private Runnable refreshAction=()->{};
    private PayerChangeHandler payerChangeHandler;
    private final JButton refresh,errors,previous,next;
    private final JMenuItem information;
    private record Group(String text,Predicate<Dong> filter,boolean heading){@Override public String toString(){return text;}}

    public ThanhToanKhamBenhManHinh(ThanhToanKhamBenhDuLieu data){
        setLayout(new BorderLayout());setBackground(Color.WHITE);setPreferredSize(new Dimension(1586,962));
        toolbar.setName("payment.toolbar");toolbar.setBackground(new Color(250,250,250));toolbar.setBorder(new MatteBorder(0,0,1,0,Color.LIGHT_GRAY));
        JButton menu=tool("",MENU,0,60);menu.setToolTipText("Menu");
        JButton print=tool("In",PRINT,60,60);print.addActionListener(e->printServices());
        refresh=tool("Làm Mới",REFRESH,120,80);refresh.addActionListener(e->refreshAction.run());
        JButton receipts=tool("Đợt Thanh Toán",DOCUMENT,200,122);receipts.addActionListener(e->showReceipts());
        unavailable(tool("XML 4210",XML,322,77),"Chưa kết nối dịch vụ xuất XML 4210.");
        unavailable(tool("Kiểm Tra Thẻ BH",INSURANCE,399,138),"Chưa kết nối cổng tra cứu thẻ BHYT.");
        for(JLabel l:List.of(patient,insurance,address)){l.setHorizontalAlignment(SwingConstants.RIGHT);l.setForeground(DARK_RED);toolbar.add(l);}
        patient.setFont(UngDungPhongChu.THANH_TOAN_BANNER);insurance.setFont(UngDungPhongChu.THANH_TOAN);address.setFont(UngDungPhongChu.THANH_TOAN);
        payer.setHorizontalAlignment(SwingConstants.CENTER);payer.setFont(UngDungPhongChu.THANH_TOAN_BANNER);payer.setForeground(Color.RED);payer.setBorder(new MatteBorder(0,1,0,0,Color.LIGHT_GRAY));toolbar.add(payer);add(toolbar,BorderLayout.NORTH);
        JPopupMenu mainMenu=new JPopupMenu();JMenuItem printItem=new JMenuItem("In danh sách dịch vụ");printItem.addActionListener(e->printServices());mainMenu.add(printItem);
        JMenuItem reload=new JMenuItem("Làm mới");reload.addActionListener(e->refreshAction.run());mainMenu.add(reload);
        JMenuItem close=new JMenuItem("Đóng");close.addActionListener(e->{Window w=SwingUtilities.getWindowAncestor(this);if(w instanceof ThanhToanKhamBenhHopThoai)w.dispose();});mainMenu.add(close);menu.addActionListener(e->mainMenu.show(menu,0,menu.getHeight()));
        sidebar.setName("payment.sidebar");sidebar.setBackground(Color.WHITE);sidebar.setBorder(new MatteBorder(0,1,1,1,Color.GRAY));
        all.setOpaque(true);all.setBackground(BLUE);all.setForeground(Color.WHITE);all.setFont(UngDungPhongChu.THANH_TOAN_BOLD);all.setBorder(new EmptyBorder(0,25,0,0));
        all.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){tree.clearSelection();treeFilter=row->true;applyFilter();}});sidebar.add(all,BorderLayout.NORTH);
        tree.setName("payment.tree");tree.setRootVisible(false);tree.setShowsRootHandles(true);tree.setFont(UngDungPhongChu.THANH_TOAN);tree.putClientProperty("JTree.lineStyle","Angled");
        tree.setUI(new javax.swing.plaf.metal.MetalTreeUI(){
            @Override protected void installDefaults(){super.installDefaults();setExpandedIcon(new BranchIcon(false));setCollapsedIcon(new BranchIcon(true));setLeftChildIndent(9);setRightChildIndent(11);}
            @Override protected void paintHorizontalLine(Graphics g,JComponent c,int y,int left,int right){for(int x=left;x<=right;x+=2){g.setColor(new Color(140,140,140));g.drawLine(x,y,x,y);}}
            @Override protected void paintVerticalLine(Graphics g,JComponent c,int x,int top,int bottom){for(int y=top;y<=bottom;y+=2){g.setColor(new Color(140,140,140));g.drawLine(x,y,x,y);}}
        });
        tree.setCellRenderer(new DefaultTreeCellRenderer(){@Override public Component getTreeCellRendererComponent(JTree t,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean focus){
            super.getTreeCellRendererComponent(t,value,selected,expanded,leaf,row,focus);setIcon(null);setOpenIcon(null);setClosedIcon(null);Object object=((DefaultMutableTreeNode)value).getUserObject();
            setFont((object instanceof Group group&&group.heading()?UngDungPhongChu.THANH_TOAN_BOLD:UngDungPhongChu.THANH_TOAN).deriveFont((float)(15*scale)));return this;
        }});
        tree.addTreeSelectionListener(e->{if(rebuilding)return;var node=(DefaultMutableTreeNode)tree.getLastSelectedPathComponent();treeFilter=node!=null&&node.getUserObject() instanceof Group group?group.filter():row->true;applyFilter();});
        JScrollPane treeScroll=new JScrollPane(tree);treeScroll.setBorder(null);sidebar.add(treeScroll);add(sidebar,BorderLayout.WEST);
        searchStrip.setName("payment.search");searchStrip.setBackground(new Color(255,255,204));
        search.setFont(UngDungPhongChu.THANH_TOAN_BANNER.deriveFont(Font.PLAIN));search.setBackground(new Color(255,255,204));search.setBorder(new LineBorder(new Color(150,150,130)));
        search.putClientProperty("JTextField.placeholderText","Tìm kiếm (Ctrl+F)");searchStrip.add(search);
        JButton searchButton=new JButton(new ThanhToanBieuTuong(SEARCH,22));searchButton.setPreferredSize(new Dimension(30,0));searchButton.setBorder(new LineBorder(Color.GRAY));searchButton.addActionListener(e->applyFilter());searchStrip.add(searchButton,BorderLayout.EAST);
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){applyFilter();}public void removeUpdate(DocumentEvent e){applyFilter();}public void changedUpdate(DocumentEvent e){applyFilter();}});
        table.setName("payment.table");table.setRowSorter(sorter);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setFont(UngDungPhongChu.THANH_TOAN);table.setShowGrid(false);table.setIntercellSpacing(new Dimension(0,0));table.setFillsViewportHeight(true);
        table.setSelectionBackground(BLUE);table.setSelectionForeground(Color.WHITE);table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setFont(UngDungPhongChu.THANH_TOAN_COT);
        DefaultTableCellRenderer header=new DefaultTableCellRenderer();header.setHorizontalAlignment(SwingConstants.LEFT);header.setBackground(new Color(245,245,248));header.setBorder(new EmptyBorder(0,6,0,3));table.getTableHeader().setDefaultRenderer(header);table.getTableHeader().setBackground(new Color(245,245,248));
        DefaultTableCellRenderer renderer=new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int r,int col){
            super.getTableCellRendererComponent(t,value,selected,false,r,col);setBorder(new EmptyBorder(0,7,0,5));setIcon(null);setHorizontalAlignment(col==7?SwingConstants.RIGHT:SwingConstants.LEFT);
            Dong row=rowAt(r);if(!selected){setBackground(r%2==0?Color.WHITE:new Color(243,243,246));setForeground(row!=null&&safe(row.subject()).equals("Hao phí khác")?Color.RED:Color.BLACK);}
            if(col==1&&row!=null&&row.confirmed())setIcon(new ThanhToanBieuTuong(CHECK,px(20)));
            setToolTipText(col==7?"Giá BHYT từ danh mục hiện tại; số tiền BHYT đã chốt hiển thị ở tổng hóa đơn.":value==null?null:value.toString());return this;
        }};
        table.setDefaultRenderer(Object.class,renderer);table.setComponentPopupMenu(popup);
        table.addMouseListener(new MouseAdapter(){private void select(MouseEvent e){if(e.isPopupTrigger()||SwingUtilities.isRightMouseButton(e)){int row=table.rowAtPoint(e.getPoint());if(row>=0)table.setRowSelectionInterval(row,row);else table.clearSelection();boolean hasRow=table.getSelectedRow()>=0;information.setEnabled(hasRow);for(JMenuItem item:payerActionItems)item.setEnabled(hasRow);}}
            @Override public void mousePressed(MouseEvent e){select(e);}@Override public void mouseReleased(MouseEvent e){select(e);}});
        JScrollPane tableScroll=new JScrollPane(table);tableScroll.setColumnHeaderView(table.getTableHeader());tableScroll.setBorder(new MatteBorder(0,0,0,1,Color.LIGHT_GRAY));
        workspace.add(searchStrip,BorderLayout.NORTH);workspace.add(tableScroll);workspace.add(footer,BorderLayout.SOUTH);add(workspace);
        warning.setBorder(new MatteBorder(1,0,1,0,Color.GRAY));warning.setBackground(new Color(250,250,250));
        previous=new JButton("Lần Trước",new ThanhToanBieuTuong(PREVIOUS,28));previous.setEnabled(false);next=new JButton("Lần Sau",new ThanhToanBieuTuong(NEXT,28));next.setEnabled(false);
        JPanel left=new JPanel(new BorderLayout());left.setOpaque(false);left.add(previous,BorderLayout.WEST);warningText.setForeground(Color.RED);warningText.setFont(UngDungPhongChu.THANH_TOAN_BANNER.deriveFont(Font.PLAIN));left.add(warningText);warning.add(left);
        JPanel warningActions=new JPanel(new BorderLayout());warningActions.setOpaque(false);errors=new JButton("✓ Danh Sách Dịch Vụ Lỗi");errors.setBackground(new Color(255,222,94));errors.addActionListener(e->{errorsOnly=!errorsOnly;errors.setText(errorsOnly?"Tất Cả Dịch Vụ":"✓ Danh Sách Dịch Vụ Lỗi");applyFilter();});warningActions.add(errors);warningActions.add(next,BorderLayout.EAST);warning.add(warningActions,BorderLayout.EAST);
        footer.add(warning,BorderLayout.NORTH);summary.setBackground(Color.WHITE);footer.add(summary);
        String[] keys={"Tổng chi phí:","Tạm ứng:","BHYT:","Đã thu:","Miễn giảm dịch vụ:","Miễn giảm hóa đơn:","Nguồn khác:","Còn nợ:","Bệnh nhân:"};
        for(String key:keys){JLabel label=new JLabel(key);label.setFont(UngDungPhongChu.THANH_TOAN_BOLD);summary.add(label);JLabel value=new JLabel("",SwingConstants.RIGHT);value.setFont(UngDungPhongChu.THANH_TOAN_TIEN);value.setForeground(key.equals("Tạm ứng:")||key.equals("Đã thu:")?Color.BLUE:new Color(190,0,0));totals.put(key,value);summary.add(value);}
        submenu("Chọn Hình Y Tế",IMAGE,"Chọn ảnh từ hồ sơ");
        payerSubmenu("Bảo Hiểm Y Tế + Dịch Vụ",SHIELD,
                new PayerOption("Chuyển đối tượng BHYT","BHYT"),
                new PayerOption("Chuyển BHYT + Dịch vụ (thu chênh lệch)","BHYT_DICH_VU"));
        payerSubmenu("Thu Phí",COINS,new PayerOption("Thu phí dịch vụ","THU_PHI"));
        payerSubmenu("Dịch Vụ",CART,new PayerOption("Thêm dịch vụ","THU_PHI"));
        payerSubmenu("Miễn Phí",GIFT,new PayerOption("Miễn phí dịch vụ","MIEN_PHI"));
        disabledItem("Hủy Dịch Vụ",CANCEL);submenu("Thay Đổi Dịch Vụ",DOCUMENT,"Chọn dịch vụ thay thế");disabledItem("Sao Chép Dịch Vụ",COPY);disabledItem("Đổi Dịch Vụ",SWAP);disabledItem("Tách Dịch Vụ",SPLIT);disabledItem("Gộp Dịch Vụ",MERGE);popup.addSeparator();
        submenu("Giảm Giá",PERCENT,"Giảm theo phần trăm","Giảm theo số tiền");disabledItem("Thay Đổi Giá",TAG);disabledItem("Cập Nhật Lại Chi Phí",CALCULATOR);popup.addSeparator();disabledItem("Ghi Chú Dịch Vụ",EDIT);popup.addSeparator();submenu("Lịch Sử Dịch Vụ",HISTORY,"Xem nhật ký thay đổi");popup.addSeparator();
        information=new JMenuItem("Thông Tin Dịch Vụ",new ThanhToanBieuTuong(INFO,20));information.addActionListener(e->showServiceInfo());popup.add(information);popup.addSeparator();disabledItem("In Tem Dịch Vụ",PRINT);disabledItem("In Barcode Dịch Vụ",BARCODE);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F,InputEvent.CTRL_DOWN_MASK),"find");getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F,InputEvent.META_DOWN_MASK),"find");getActionMap().put("find",new AbstractAction(){public void actionPerformed(ActionEvent e){search.requestFocusInWindow();search.selectAll();}});
        for(Component c:popup.getComponents())if(c instanceof JMenuItem item){item.setFont(UngDungPhongChu.THANH_TOAN_MENU);item.setDisabledIcon(item.getIcon());}
        bindData(data);
    }
    private record PayerOption(String label,String targetPayer){}
    private record BranchIcon(boolean collapsed) implements Icon {
        public int getIconWidth(){return 9;}public int getIconHeight(){return 9;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){graphics.setColor(Color.WHITE);graphics.fillRect(x,y,8,8);graphics.setColor(Color.GRAY);graphics.drawRect(x,y,8,8);graphics.setColor(new Color(80,80,80));graphics.drawLine(x+2,y+4,x+6,y+4);if(collapsed)graphics.drawLine(x+4,y+2,x+4,y+6);}
    }
    private JButton tool(String title,ThanhToanBieuTuong.Kind icon,int x,int width){JButton b=new JButton(title,new ThanhToanBieuTuong(icon,32));b.putClientProperty("payment.icon",icon);b.putClientProperty("payment.bounds",new Rectangle(x,0,width,64));b.setVerticalTextPosition(SwingConstants.BOTTOM);b.setHorizontalTextPosition(SwingConstants.CENTER);b.setFont(UngDungPhongChu.THANH_TOAN);b.setIconTextGap(2);b.setMargin(new Insets(0,0,0,0));b.setBorderPainted(false);b.setContentAreaFilled(false);b.setFocusPainted(false);toolButtons.add(b);toolbar.add(b);return b;}
    private static void unavailable(AbstractButton button,String reason){button.setEnabled(false);button.setToolTipText(reason);}
    private void disabledItem(String title,ThanhToanBieuTuong.Kind icon){JMenuItem item=new JMenuItem(title,new ThanhToanBieuTuong(icon,20));unavailable(item,"Chưa kết nối nghiệp vụ này cho từng dịch vụ tại màn thanh toán.");popup.add(item);}
    private void submenu(String title,ThanhToanBieuTuong.Kind icon,String... entries){JMenu menu=new JMenu(title);menu.setIcon(new ThanhToanBieuTuong(icon,20));for(String name:entries){JMenuItem entry=new JMenuItem(name);unavailable(entry,"Chưa kết nối nghiệp vụ này cho từng dịch vụ tại màn thanh toán.");menu.add(entry);}popup.add(menu);}
    private void payerSubmenu(String title,ThanhToanBieuTuong.Kind icon,PayerOption... options){
        JMenu menu=new JMenu(title);menu.setIcon(new ThanhToanBieuTuong(icon,20));
        for(PayerOption opt:options){
            JMenuItem entry=new JMenuItem(opt.label());
            entry.setName("payer."+opt.targetPayer()+"."+opt.label());
            entry.addActionListener(e->changeSelectedLinePayer(opt.targetPayer(),opt.label()));
            payerActionItems.add(entry);
            menu.add(entry);
        }
        popup.add(menu);
    }
    public void setRefreshAction(Runnable action){refreshAction=Objects.requireNonNull(action);}
    public void setPayerChangeHandler(PayerChangeHandler handler){this.payerChangeHandler=handler;}
    public void setLoading(boolean loading){refresh.setEnabled(!loading);warningText.setText(loading?"Đang tải dữ liệu thanh toán...":warningText.getText());}
    public void showError(String message){refresh.setEnabled(true);warningText.setText(message);}

    public void changeSelectedLinePayer(String targetPayerType,String optionLabel){
        Dong row=rowAt(table.getSelectedRow());
        if(row==null||data==null||data.invoice()==null){
            if(!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(this,"Vui lòng chọn một dòng dịch vụ cần chuyển đối tượng.","Thông báo",JOptionPane.WARNING_MESSAGE);
            return;
        }
        int invoiceId=data.invoice().id();
        int lineId=row.id();
        BigDecimal svcPrice=row.catalogServicePrice()!=null&&row.catalogServicePrice().signum()>0?row.catalogServicePrice():row.unitPrice();
        BigDecimal insPrice=row.insurancePrice();
        Integer rate=data.insuranceRate()>0?data.insuranceRate():(!safe(data.insuranceNumber()).isEmpty()?80:null);

        if(payerChangeHandler!=null){
            if(GraphicsEnvironment.isHeadless()){
                var result=payerChangeHandler.changePayer(invoiceId,lineId,targetPayerType,svcPrice,insPrice,rate);
                handlePayerChangeResult(row,optionLabel,result);
                return;
            }
            setLoading(true);
            new SwingWorker<KetQua<ChangeChiTietHoaDonDoiTuongBienNhan>,Void>(){
                @Override protected KetQua<ChangeChiTietHoaDonDoiTuongBienNhan> doInBackground(){
                    return payerChangeHandler.changePayer(invoiceId,lineId,targetPayerType,svcPrice,insPrice,rate);
                }
                @Override protected void done(){
                    setLoading(false);
                    try{
                        handlePayerChangeResult(row,optionLabel,get());
                    }catch(Exception ex){
                        showError("Không thể chuyển đối tượng dịch vụ: "+ex.getMessage());
                    }
                }
            }.execute();
            return;
        }
        applyLocalPayerChange(row,targetPayerType,svcPrice,insPrice,rate!=null?rate:0);
    }

    private void handlePayerChangeResult(Dong row,String optionLabel,KetQua<ChangeChiTietHoaDonDoiTuongBienNhan> result){
        if(result==null||result.isFailure()){
            String msg=result!=null&&result.getError().isPresent()?result.getError().get().message():"Chuyển đối tượng dịch vụ thất bại.";
            showError(msg);
            if(!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(this,msg,"Không thể chuyển đối tượng",JOptionPane.ERROR_MESSAGE);
            return;
        }
        var receipt=result.valueOrThrow();
        refreshAction.run();
        if(!GraphicsEnvironment.isHeadless()){
            StringBuilder sb=new StringBuilder();
            sb.append("Đã thực hiện \"").append(optionLabel).append("\" cho dịch vụ:\n")
              .append("• ").append(safe(row.description())).append("\n")
              .append("• Tổng chi phí mới: ").append(money(receipt.totalAmount())).append(" VNĐ\n")
              .append("• Quỹ BHYT chi trả: ").append(money(receipt.insurancePaidAmount())).append(" VNĐ\n")
              .append("• Miễn giảm dịch vụ: ").append(money(receipt.serviceDiscountAmount())).append(" VNĐ\n")
              .append("• Bệnh nhân phải trả: ").append(money(receipt.patientPaidAmount())).append(" VNĐ\n")
              .append("• Đã thu (ròng): ").append(money(receipt.amountPaid())).append(" VNĐ");
            if(receipt.pendingRefundAmount()!=null&&receipt.pendingRefundAmount().signum()>0){
                sb.append("\n\n→ Phát sinh tiền dư CẦN HOÀN TRẢ cho bệnh nhân: ").append(money(receipt.pendingRefundAmount())).append(" VNĐ (chi hoàn tại quầy Thu ngân).");
            }else if(receipt.remainingAmount()!=null&&receipt.remainingAmount().signum()>0){
                sb.append("\n\n→ Phát sinh CÒN NỢ cần thu bổ sung: ").append(money(receipt.remainingAmount())).append(" VNĐ.");
            }
            JOptionPane.showMessageDialog(this,sb.toString(),"Chuyển đối tượng thành công",JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void applyLocalPayerChange(Dong targetRow,String targetPayerType,BigDecimal svcPrice,BigDecimal insPrice,int coverageRate){
        if("BHYT".equalsIgnoreCase(targetPayerType)||"BHYT_DICH_VU".equalsIgnoreCase(targetPayerType)){
            if(insPrice==null||insPrice.signum()<=0){
                showError("Dịch vụ/thuốc \""+safe(targetRow.description())+"\" không có đơn giá BHYT trong danh mục.");
                return;
            }
            if(coverageRate<=0){
                showError("Bệnh nhân chưa có thông tin thẻ BHYT hợp lệ để chuyển sang đối tượng Bảo hiểm.");
                return;
            }
        }
        HoaDon domainInv=new HoaDon();
        HoaDonDuLieu oldInv=data.invoice();
        domainInv.setId(oldInv.id());
        domainInv.setSoHoaDon(oldInv.invoiceNumber());
        domainInv.setMaLuotKham(oldInv.visitId());
        domainInv.setMaBenhNhan(oldInv.patientId());
        domainInv.setMaCoSo(oldInv.facilityId());
        domainInv.setNgayLap(oldInv.invoiceDate());
        domainInv.setSoTienTamUng(zero(oldInv.advanceAmount()));
        domainInv.setSoTienGiamGia(zero(oldInv.discountAmount()));
        domainInv.setChietKhauPhanTram(zero(oldInv.discountPercentage()));
        domainInv.setLyDoGiamGia(safe(oldInv.discountReason()));
        domainInv.setDaThanhToan(zero(oldInv.amountPaid()));
        domainInv.setSoTienHoanTra(zero(oldInv.refundAmount()));
        domainInv.setTrangThai(oldInv.status());
        domainInv.setGhiChu(oldInv.notes());

        List<Dong> updatedRows=new ArrayList<>();
        List<HoaDonDongDuLieu> updatedLines=new ArrayList<>();
        for(Dong r:data.rows()){
            ChiTietHoaDon ct=new ChiTietHoaDon();
            ct.setId(r.id());
            ct.setMoTa(r.description());
            ct.setSoLuong(r.quantity()!=null?r.quantity():BigDecimal.ONE);
            ct.setDonGia(r.unitPrice()!=null?r.unitPrice():BigDecimal.ZERO);
            ct.setDoiTuongThanhToan(r.payerCode());
            ct.setGiaBHYT(r.insurancePrice()!=null?r.insurancePrice():BigDecimal.ZERO);
            ct.setSoTienBHYT(r.insuranceAmount());
            ct.setSoTienMienGiam(r.discountAmount());
            ct.setSoTienBenhNhan(r.patientAmount());
            if(r.id()==targetRow.id()){
                ct.applyPayerRule(targetPayerType,svcPrice,insPrice,coverageRate);
            }
            domainInv.addChiTiet(ct);
        }
        domainInv.recalculateAfterPayerChange();
        boolean paid=domainInv.getTrangThai()==vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon.DA_THANH_TOAN;
        for(int i=0;i<data.rows().size();i++){
            Dong r=data.rows().get(i);
            ChiTietHoaDon ct=domainInv.getChiTietHoaDons().get(i);
            String rawPayer=ct.getDoiTuongThanhToan();
            String subj=switch(rawPayer){
                case "BHYT"->"BHYT";
                case "BHYT_DICH_VU"->"BHYT + Dịch vụ";
                case "MIEN_PHI"->"Miễn phí";
                default->"Thu phí";
            };
            String src=switch(rawPayer){
                case "BHYT"->ct.getTyLeHuong()>0?"Quỹ BHYT ("+ct.getTyLeHuong()+"%)":"Quỹ BHYT / Đồng chi trả";
                case "BHYT_DICH_VU"->ct.getTyLeHuong()>0?"Quỹ BHYT ("+ct.getTyLeHuong()+"%) + BN tự trả":"Quỹ BHYT + BN tự trả chênh lệch";
                case "MIEN_PHI"->"Miễn giảm dịch vụ";
                default->"Bệnh nhân tự trả";
            };
            updatedRows.add(new Dong(r.id(),r.orderedAt(),r.equivalentCode(),r.description(),r.serviceType(),
                    subj,src,r.insurancePrice(),r.unit(),ct.getSoLuong(),ct.getDonGia(),ct.getThanhTien(),
                    r.category(),r.department(),r.doctor(),paid,
                    rawPayer,r.catalogServicePrice(),ct.getSoTienBHYT(),ct.getSoTienMienGiam(),ct.getSoTienBenhNhan()));
            updatedLines.add(new HoaDonDongDuLieu(ct.getId(),"",0,ct.getMoTa(),ct.getSoLuong(),ct.getDonGia(),ct.getThanhTien(),
                    rawPayer,ct.getGiaBHYT(),ct.getTyLeHuong(),ct.getSoTienBHYT(),ct.getSoTienMienGiam(),ct.getSoTienBenhNhan()));
        }
        HoaDonDuLieu newInv=new HoaDonDuLieu(
                oldInv.id(),oldInv.invoiceNumber(),oldInv.visitId(),oldInv.patientId(),oldInv.facilityId(),oldInv.invoiceDate(),
                domainInv.getTongTien(),domainInv.getSoTienBaoHiemChiTra(),domainInv.getSoTienTamUng(),domainInv.getSoTienGiamGia(),
                domainInv.getChietKhauPhanTram(),domainInv.getLyDoGiamGia(),domainInv.getSoTienBenhNhanChiTra(),domainInv.getDaThanhToan(),
                domainInv.getRemainingAmount(),domainInv.getTrangThai(),domainInv.getGhiChu(),updatedLines,oldInv.receipts(),
                domainInv.getTotalServiceDiscountAmount(),domainInv.getSoTienHoanTra(),domainInv.getPendingRefundAmount(),oldInv.refunds());
        bindData(new ThanhToanKhamBenhDuLieu(
                data.patientName(),data.patientCode(),data.birthYear(),data.age(),data.gender(),data.address(),
                data.insuranceNumber(),data.insuranceRate(),data.insuranceRemaining(),data.visitCode(),
                newInv,updatedRows,data.collectors(),"",data.subject()));
    }

    public void bindData(ThanhToanKhamBenhDuLieu value){
        data=value;model.setRowCount(0);treeFilter=row->true;errorsOnly=false;errors.setText("✓ Danh Sách Dịch Vụ Lỗi");
        if(value!=null)for(Dong row:value.rows())model.addRow(new Object[]{row.orderedAt()==null?"":row.orderedAt().format(TIME),"",safe(row.equivalentCode()),safe(row.description()),safe(row.serviceType()),safe(row.subject()),safe(row.paymentSource()),money(row.insurancePrice()),safe(row.unit())});
        patient.setText(value==null?"":safe(value.patientName())+" | "+safe(value.patientCode())+" | "+safe(value.birthYear())+" ("+safe(value.age())+" tuổi) | "+safe(value.gender()));
        insurance.setText(value==null?"":"Mã VP : "+safe(value.visitCode())+(safe(value.insuranceNumber()).isEmpty()?" | "+safe(value.subject()):" | "+safe(value.insuranceNumber()))+(safe(value.insuranceRemaining()).isEmpty()?"":" | "+safe(value.insuranceRemaining())));address.setText(value==null?"":safe(value.address()));
        boolean covered=value!=null&&"BHYT".equalsIgnoreCase(safe(value.subject()));payer.setText(value==null?"":covered?"<html><center>BHYT<br><font color='blue'>"+(value.insuranceRate()>0?value.insuranceRate()+"%":"")+"</font></center></html>":safe(value.subject()));
        rebuildTree();refreshTotals();applyFilter();if(table.getRowCount()>0)table.setRowSelectionInterval(0,0);refresh.setEnabled(true);revalidate();repaint();
    }
    private void rebuildTree(){
        rebuilding=true;DefaultMutableTreeNode root=new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode subject=heading(root,"Đối Tượng Dịch Vụ"),category=heading(root,"Loại Chi Phí"),date=heading(root,"Ngày Y Lệnh"),department=heading(root,"Khoa Chỉ Định"),doctor=heading(root,"Người Chỉ Định"),receipts=heading(root,"Phiếu Thu");
        if(data!=null){
            group(subject,row->orDefault(row.subject(),"Chưa xác định đối tượng"));
            group(category,row->orDefault(row.category(),"Khác"));
            group(date,row->row.orderedAt()==null?"Chưa có ngày y lệnh":row.orderedAt().format(DAY));
            group(department,row->orDefault(row.department(),"Chưa xác định khoa"));
            group(doctor,row->orDefault(row.doctor(),"Chưa gán bác sĩ"));
            if(data.invoice()!=null){
                if(data.invoice().receipts()!=null)for(var receipt:data.invoice().receipts()){String who=safe(data.collectors().get(receipt.cashierId()));if(who.isEmpty())who="Phiếu thu #"+receipt.id();receipts.add(new DefaultMutableTreeNode(new Group(who+" ("+money(receipt.amount())+")",row->true,false)));}
                if(data.invoice().refunds()!=null)for(var refund:data.invoice().refunds()){String who=safe(data.collectors().get(refund.cashierId()));if(who.isEmpty())who="Hoàn trả #"+refund.id();else who="Hoàn trả - "+who;receipts.add(new DefaultMutableTreeNode(new Group(who+" (-"+money(refund.amount())+")",row->true,false)));}
            }
        }
        tree.setModel(new DefaultTreeModel(root));for(int i=0;i<tree.getRowCount();i++)tree.expandRow(i);rebuilding=false;
    }
    private DefaultMutableTreeNode heading(DefaultMutableTreeNode root,String name){var node=new DefaultMutableTreeNode(new Group(name,row->true,true));root.add(node);return node;}
    private static String orDefault(String value,String fallback){String cleaned=safe(value);return cleaned.isEmpty()?fallback:cleaned;}
    private void group(DefaultMutableTreeNode parent,Function<Dong,String> key){Map<String,BigDecimal> grouped=new LinkedHashMap<>();for(Dong row:data.rows()){String value=safe(key.apply(row));grouped.merge(value,zero(row.amount()),BigDecimal::add);}grouped.forEach((name,amount)->parent.add(new DefaultMutableTreeNode(new Group(name+" ("+money(amount)+")",row->safe(key.apply(row)).equals(name),false))));}
    private void refreshTotals(){
        HoaDonDuLieu invoice=data==null?null:data.invoice();for(JLabel label:totals.values())label.setText("");
        if(invoice!=null){
            setTotal("Tổng chi phí:",invoice.totalAmount());
            setTotal("Tạm ứng:",invoice.advanceAmount());
            setTotal("BHYT:",invoice.insurancePaidAmount());
            setTotal("Đã thu:",invoice.amountPaid());
            if(invoice.serviceDiscountAmount()!=null&&invoice.serviceDiscountAmount().signum()>0){
                setTotal("Miễn giảm dịch vụ:",invoice.serviceDiscountAmount());
            }
            setTotal("Miễn giảm hóa đơn:",invoice.discountAmount());
            if(invoice.pendingRefundAmount()!=null&&invoice.pendingRefundAmount().signum()>0){
                setTotal("Nguồn khác:",invoice.pendingRefundAmount());
                totals.get("Nguồn khác:").setToolTipText("Số tiền dư cần hoàn trả bệnh nhân tại Thu ngân.");
            }else if(invoice.refundAmount()!=null&&invoice.refundAmount().signum()>0){
                setTotal("Nguồn khác:",invoice.refundAmount());
                totals.get("Nguồn khác:").setToolTipText("Tổng số tiền Thu ngân đã chi hoàn trả cho bệnh nhân.");
            }else{
                totals.get("Nguồn khác:").setToolTipText("Chưa có số liệu nguồn chi trả khác.");
            }
            setTotal("Còn nợ:",invoice.remainingAmount());
            setTotal("Bệnh nhân:",invoice.patientPaidAmount());
        }
        totals.get("Miễn giảm dịch vụ:").setToolTipText("Tổng số tiền miễn giảm từ các dịch vụ đối tượng Miễn phí.");
        all.setText("Tất Cả Dịch Vụ ("+(invoice==null?"":money(invoice.totalAmount()))+")");
    }
    private void setTotal(String key,BigDecimal n){totals.get(key).setText(money(n));}
    private void applyFilter(){
        String query=normalize(search.getText());sorter.setRowFilter(new RowFilter<>(){@Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){if(data==null||entry.getIdentifier()>=data.rows().size())return false;Dong row=data.rows().get(entry.getIdentifier());if(!treeFilter.test(row)||errorsOnly&&!missingPrice(row))return false;return query.isEmpty()||normalize(safe(row.description())+" "+safe(row.equivalentCode())+" "+safe(row.subject())+" "+safe(row.category())).contains(query);}});
        long missing=data==null?0:data.rows().stream().filter(this::missingPrice).count();
        HoaDonDuLieu inv=data==null?null:data.invoice();
        BigDecimal pendingRefund=inv!=null&&inv.pendingRefundAmount()!=null?inv.pendingRefundAmount():BigDecimal.ZERO;
        String message=data==null?"Chưa tải thông tin thanh toán."
                :!safe(data.message()).isEmpty()?data.message()
                :missing>0?"[1/"+missing+"] Cảnh báo : Có "+missing+" dịch vụ không có đơn giá !"
                :pendingRefund.signum()>0?"Thông báo : Hóa đơn có khoản tiền dư cần hoàn trả bệnh nhân tại Thu ngân: "+money(pendingRefund)+" VNĐ"
                :"";
        warningText.setText(message);errors.setEnabled(missing>0);
    }
    private boolean missingPrice(Dong row){return row.unitPrice()==null||row.unitPrice().signum()<=0;}
    private Dong rowAt(int viewRow){return data==null||viewRow<0?null:data.rows().get(table.convertRowIndexToModel(viewRow));}
    private void showServiceInfo(){Dong row=rowAt(table.getSelectedRow());if(row==null)return;JTextArea text=new JTextArea("Dịch vụ: "+safe(row.description())+"\nNhóm: "+safe(row.category())+"\nĐối tượng: "+safe(row.subject())+"\nNguồn chi trả: "+safe(row.paymentSource())+"\nSố lượng: "+(row.quantity()==null?"":row.quantity().stripTrailingZeros().toPlainString())+"\nĐơn giá hóa đơn: "+money(row.unitPrice())+"\nGiá BHYT danh mục: "+money(row.insurancePrice())+"\nThành tiền: "+money(row.amount())+"\nQuỹ BHYT trả: "+money(row.insuranceAmount())+"\nMiễn giảm: "+money(row.discountAmount())+"\nBệnh nhân trả: "+money(row.patientAmount())+"\nKhoa chỉ định: "+safe(row.department())+"\nNgười chỉ định: "+safe(row.doctor()));text.setEditable(false);text.setFont(UngDungPhongChu.BODY);JOptionPane.showMessageDialog(this,text,"Thông tin dịch vụ",JOptionPane.INFORMATION_MESSAGE);}
    private void showReceipts(){
        DefaultTableModel receipts=new DefaultTableModel(new String[]{"Chứng từ","Loại","Ngày giờ","Người thực hiện","Số tiền","Ghi chú / Lý do"},0){@Override public boolean isCellEditable(int r,int c){return false;}};
        if(data!=null&&data.invoice()!=null){
            if(data.invoice().receipts()!=null)for(var receipt:data.invoice().receipts())receipts.addRow(new Object[]{"PT-"+receipt.id(),"Phiếu thu (+)",receipt.paymentTime()==null?"":receipt.paymentTime().format(TIME),data.collectors().getOrDefault(receipt.cashierId(),""),"+"+money(receipt.amount()),receipt.notes()});
            if(data.invoice().refunds()!=null)for(var refund:data.invoice().refunds())receipts.addRow(new Object[]{"HT-"+refund.id(),"Phiếu hoàn (-)",refund.refundTime()==null?"":refund.refundTime().format(TIME),data.collectors().getOrDefault(refund.cashierId(),""),"-"+money(refund.amount()),refund.reason()});
        }
        JTable view=new JTable(receipts);view.setFont(UngDungPhongChu.BODY);JScrollPane scroll=new JScrollPane(view);scroll.setPreferredSize(new Dimension(850,320));JOptionPane.showMessageDialog(this,receipts.getRowCount()==0?"Chưa có phiếu thu/hoàn trả cho hóa đơn này.":scroll,"Chứng từ thanh toán & hoàn trả",JOptionPane.PLAIN_MESSAGE);
    }
    private void printServices(){
        if(data==null||data.rows().isEmpty()||GraphicsEnvironment.isHeadless())return;
        if(data.invoice()!=null && data.invoice().visitId()>0){
            Window owner=SwingUtilities.getWindowAncestor(this);
            int vid=data.invoice().visitId();
            new SwingWorker<vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.BillingSlipData,Void>(){
                @Override protected vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.BillingSlipData doInBackground(){
                    return vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.loadFromDatabase(vid);
                }
                @Override protected void done(){
                    try{
                        var slip=get();
                        if(slip==null && data!=null && data.invoice()!=null){
                            slip=vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.fromInvoice(data.invoice(),null);
                        }
                        if(slip!=null){
                            new vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuHopThoai(owner,slip).setVisible(true);
                            return;
                        }
                    }catch(Exception ignored){}
                    if(data!=null && data.invoice()!=null){
                        var fallbackSlip=vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuManHinh.fromInvoice(data.invoice(),null);
                        if(fallbackSlip!=null){
                            new vn.clinicmanagement.desktop.modules.print.billing.YTeVienPhiPhieuHopThoai(owner,fallbackSlip).setVisible(true);
                            return;
                        }
                    }
                    JOptionPane.showMessageDialog(ThanhToanKhamBenhManHinh.this,"Không tải được mẫu Bảng kê chi phí khám bệnh.","Lỗi xem phiếu in",JOptionPane.ERROR_MESSAGE);
                }
            }.execute();
            return;
        }
        try{table.print(JTable.PrintMode.FIT_WIDTH,new MessageFormat("Dịch vụ - "+safe(data.patientName())+" - "+safe(data.visitCode())),new MessageFormat("Trang {0}"));}
        catch(PrinterException ex){JOptionPane.showMessageDialog(this,"Không thể in danh sách dịch vụ.","Lỗi in",JOptionPane.ERROR_MESSAGE);}
    }
    @Override public void doLayout(){
        scale=Math.max(.5,getWidth()/1586.0);toolbar.setPreferredSize(new Dimension(0,px(64)));sidebar.setPreferredSize(new Dimension(px(284),0));all.setPreferredSize(new Dimension(0,px(26)));searchStrip.setPreferredSize(new Dimension(0,px(30)));footer.setPreferredSize(new Dimension(0,px(130)));warning.setPreferredSize(new Dimension(0,px(35)));
        scaleFonts(this);scaleFonts(popup);tree.setRowHeight(px(23));table.setRowHeight(px(25));table.getTableHeader().setPreferredSize(new Dimension(0,px(30)));
        for(JButton b:toolButtons){Rectangle r=(Rectangle)b.getClientProperty("payment.bounds");b.setBounds(px(r.x),0,px(r.width),px(r.height));b.setIcon(new ThanhToanBieuTuong((ThanhToanBieuTuong.Kind)b.getClientProperty("payment.icon"),px(32)));}
        int bannerStart=px(550),right=getWidth()-px(85);patient.setBounds(bannerStart,px(0),Math.max(0,right-bannerStart),px(24));insurance.setBounds(bannerStart,px(24),Math.max(0,right-bannerStart),px(21));address.setBounds(bannerStart,px(44),Math.max(0,right-bannerStart),px(20));payer.setBounds(getWidth()-px(77),0,px(77),px(64));
        super.doLayout();int width=getWidth()-px(284);int[] widths={149,26,129,385,144,154,157,99,60};for(int c=0;c<widths.length;c++)table.getColumnModel().getColumn(c).setPreferredWidth(Math.max(1,(int)Math.round(widths[c]*width/1303.0)));
        int[] x={10,10,244,244,427,427,690,690,908},y={5,32,5,32,5,32,5,32,5},labelWidths={105,105,72,72,177,177,102,102,105},valueWidths={115,115,105,105,75,75,95,95,105};
        for(int i=0;i<totals.size();i++){Component label=summary.getComponent(i*2),value=summary.getComponent(i*2+1);label.setBounds(px(x[i]),px(y[i]),px(labelWidths[i]),px(27));value.setBounds(px(x[i]+labelWidths[i]),px(y[i]),px(valueWidths[i]),px(27));}
        for(Component c:popup.getComponents())if(c instanceof JMenuItem item)item.setPreferredSize(new Dimension(px(266),px(26)));
    }
    private void scaleFonts(Component c){if(c instanceof JComponent jc){Font base=(Font)jc.getClientProperty("payment.font");if(base==null){base=jc.getFont();if(base!=null)jc.putClientProperty("payment.font",base);}if(base!=null)jc.setFont(base.deriveFont((float)(base.getSize2D()*scale)));}if(c instanceof Container ct)for(Component child:ct.getComponents())scaleFonts(child);}
    private int px(int value){return Math.max(1,(int)Math.round(value*scale));}
    private static String money(BigDecimal value){return value==null?"":new DecimalFormat("#,##0",DecimalFormatSymbols.getInstance(Locale.US)).format(value);}
    private static String normalize(String text){return Normalizer.normalize(safe(text),Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);}
    public JTable getServiceTable(){return table;}public JTree getCostTree(){return tree;}public JTextField getSearchField(){return search;}public JPopupMenu getServicePopup(){return popup;}public ThanhToanKhamBenhDuLieu getData(){return data;}
}
