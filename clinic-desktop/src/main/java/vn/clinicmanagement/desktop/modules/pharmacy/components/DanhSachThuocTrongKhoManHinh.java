package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.inventory.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Thuoc;
import vn.clinicmanagement.desktop.modules.pharmacy.NhaThuocGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Danh mục thuốc mở từ Kho Nhà Thuốc; bố cục gọn, tự phân bổ cột theo chiều rộng màn hình. */
public final class DanhSachThuocTrongKhoManHinh extends JPanel {
    private static final Font TEXT = new Font("Arial",Font.PLAIN,14);
    private static final int[] MIN_COLUMN_WIDTHS={68,92,140,110,72,64,54,64,80,64,78,60,72};
    private static final int MIN_TABLE_WIDTH=Arrays.stream(MIN_COLUMN_WIDTHS).sum();
    private int fittedWidth=-1;
    private static final Color GRID = new Color(207,212,220), BLUE = new Color(0,143,255);
    private static final List<String> GROUPS = List.of("Thuốc viên","Thuốc khác","Thuốc Hen","Thuốc Hô Hấp","Thuốc Kháng Viêm",
            "Thuốc Hạ Nhiệt - Giảm Đau","Thuốc Nhỏ Mũi","Thuốc Nhỏ Tai","Thuốc Dị Ứng","Thuốc Tiêu Hóa","Thuốc Kháng Sinh");
    private final NhaThuocDanhSachReadCongKetNoi read;
    private final NhaThuocGhepNoiGoc.Services services;
    private final Integer warehouseId;
    private final JPanel sidebar = new JPanel();
    private final JTextField search = new JTextField();
    private final JCheckBox summary = new JCheckBox("Hiển thị tổng hợp");
    private final JComboBox<String> warehouseFilter = new JComboBox<>(new String[]{"Tất cả kho"});
    private final JComboBox<String> expiryFilter = new JComboBox<>(new String[]{"Tất cả HSD","Sắp hết hạn (≤ 90 ngày)","Đã hết hạn","Hết tồn (= 0)"});
    private final JLabel notice = new JLabel();
    private final JButton refresh, add, edit, stockCard, exportBtn, mapModern, mapTraditional;
    private final DefaultTableModel model;
    private final JTable table;
    private final List<Thuoc> displayed = new ArrayList<>();
    private List<Thuoc> all = List.of();
    private String selectedGroup;
    private String insuranceFilter;
    private boolean groupsExpanded=true,insuranceExpanded=true;
    private long generation;
    private SwingWorker<List<Thuoc>,Void> loadWorker;

    public DanhSachThuocTrongKhoManHinh(NhaThuocDanhSachReadCongKetNoi read,NhaThuocGhepNoiGoc.Services services,Integer warehouseId){
        super(new BorderLayout());this.read=read;this.services=services;this.warehouseId=warehouseId;
        setPreferredSize(new Dimension(1240,680));setBackground(Color.WHITE);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        toolbar.setBackground(new Color(248,248,248));toolbar.setPreferredSize(new Dimension(100,56));
        JButton menu=tool("Menu","menu",48);refresh=tool("Làm Mới","refresh",70);add=tool("Thêm","add",56);
        edit=tool("Sửa","edit",52);stockCard=tool("Thẻ Kho","card",66);exportBtn=tool("Xuất CSV","export",72);
        mapModern=tool("<html><center>Ánh Xạ Danh Mục<br>Thuốc Tân Dược</center></html>","map",150);
        mapTraditional=tool("<html><center>Ánh Xạ Danh Mục<br>Học Cổ Truyền</center></html>","map",154);
        toolbar.add(menu);toolbar.add(refresh);toolbar.add(add);toolbar.add(edit);toolbar.add(stockCard);toolbar.add(exportBtn);toolbar.add(mapModern);toolbar.add(mapTraditional);
        summary.setFont(TEXT);summary.setOpaque(false);summary.setBorder(new CompoundBorder(new LineBorder(Color.GRAY),new EmptyBorder(7,4,7,4)));
        summary.setPreferredSize(new Dimension(145,44));summary.setBorderPainted(true);toolbar.add(summary);notice.setFont(TEXT.deriveFont(12f));toolbar.add(notice);
        JPopupMenu actions=new JPopupMenu();
        JMenuItem miEdit=new JMenuItem("Sửa thông tin thuốc đầy đủ");miEdit.addActionListener(e->editFullDrug());actions.add(miEdit);
        JMenuItem details=new JMenuItem("Thông tin phân loại / BHYT");details.addActionListener(e->editMetadata(false));actions.add(details);
        JMenuItem miCard=new JMenuItem("Xem thẻ kho (Lịch sử nhập/xuất)");miCard.addActionListener(e->showStockCard());actions.add(miCard);
        JMenuItem miLock=new JMenuItem("Khóa thuốc (Ngừng sử dụng)");miLock.addActionListener(e->toggleDrugActive(false));actions.add(miLock);
        JMenuItem miUnlock=new JMenuItem("Mở khóa thuốc");miUnlock.addActionListener(e->toggleDrugActive(true));actions.add(miUnlock);
        JMenuItem miExport=new JMenuItem("Xuất CSV danh sách đang lọc");miExport.addActionListener(e->exportCsv());actions.add(miExport);
        actions.addSeparator();
        JMenuItem close=new JMenuItem("Đóng");close.addActionListener(e->{Window window=SwingUtilities.getWindowAncestor(this);if(window!=null)window.dispose();});actions.add(close);
        menu.addActionListener(e->{if(menu.isShowing())actions.show(menu,0,menu.getHeight());});
        refresh.addActionListener(e->refreshData());add.addActionListener(e->createDrug());edit.addActionListener(e->editFullDrug());stockCard.addActionListener(e->showStockCard());exportBtn.addActionListener(e->exportCsv());mapModern.addActionListener(e->editMetadata(false));mapTraditional.addActionListener(e->editMetadata(true));
        summary.addActionListener(e->{applyFilter();});add(toolbar,BorderLayout.NORTH);
        sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));sidebar.setBackground(Color.WHITE);
        JPanel treeHolder=new JPanel(new BorderLayout());treeHolder.setBackground(Color.WHITE);treeHolder.add(sidebar,BorderLayout.NORTH);
        JScrollPane treeScroll=new JScrollPane(treeHolder);treeScroll.setBorder(new MatteBorder(0,0,0,1,GRID));treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel content=new JPanel(new BorderLayout());content.setBackground(Color.WHITE);
        JPanel searchStrip=new JPanel(new BorderLayout());searchStrip.setPreferredSize(new Dimension(100,28));searchStrip.setBackground(new Color(255,255,206));
        search.setFont(TEXT.deriveFont(15f));search.setOpaque(false);search.setBorder(new EmptyBorder(0,5,0,0));
        search.putClientProperty("JTextField.placeholderText","Tìm kiếm (Ctrl+F)");
        search.setUI(new javax.swing.plaf.basic.BasicTextFieldUI(){@Override protected void paintSafely(Graphics g){super.paintSafely(g);if(search.getText().isEmpty()&&!search.isFocusOwner()){g.setColor(new Color(65,65,40));g.setFont(TEXT.deriveFont(15f));g.drawString("Tìm kiếm (Ctrl+F)",5,20);}}});
        JButton find=new JButton(new Icon(){public int getIconWidth(){return 24;}public int getIconHeight(){return 24;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){var g=(Graphics2D)graphics.create();g.translate(x,y);g.setColor(new Color(0,95,220));g.setStroke(new BasicStroke(3));g.drawOval(8,2,12,12);g.drawLine(9,14,2,22);g.dispose();}});find.setMargin(new Insets(0,0,0,0));find.setBorder(new LineBorder(Color.GRAY));find.setForeground(new Color(0,90,220));find.setPreferredSize(new Dimension(30,26));find.addActionListener(e->applyFilter());
        warehouseFilter.setFont(TEXT.deriveFont(12f));warehouseFilter.setPreferredSize(new Dimension(170,26));warehouseFilter.addActionListener(e->applyFilter());
        expiryFilter.setFont(TEXT.deriveFont(12f));expiryFilter.setPreferredSize(new Dimension(175,26));expiryFilter.addActionListener(e->applyFilter());
        JPanel filtersRight=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,1));filtersRight.setOpaque(false);
        filtersRight.add(warehouseFilter);filtersRight.add(expiryFilter);filtersRight.add(find);
        searchStrip.add(search,BorderLayout.CENTER);searchStrip.add(filtersRight,BorderLayout.EAST);content.add(searchStrip,BorderLayout.NORTH);
        model=new DefaultTableModel(new String[]{"Mã Thuốc","Mã Tương Đương","Tên Thuốc","Hoạt Chất","Hàm Lượng","Nồng Độ","Thể tích","Đơn Vị Tính","Kho thuốc","Số lô","Hạn dùng","Tồn kho","Mã YHCT"},0){@Override public boolean isCellEditable(int row,int column){return false;}};
        table=new JTable(model){
            @Override public boolean getScrollableTracksViewportWidth(){return getParent() instanceof JViewport viewport && viewport.getWidth()>=MIN_TABLE_WIDTH;}
            @Override public void doLayout(){
                if(getWidth()!=fittedWidth && getColumnCount()==MIN_COLUMN_WIDTHS.length){
                    fittedWidth=getWidth();int available=Math.max(MIN_TABLE_WIDTH,getWidth());int extra=available-MIN_TABLE_WIDTH;int used=0;
                    for(int i=0;i<MIN_COLUMN_WIDTHS.length;i++){
                        int width=i==MIN_COLUMN_WIDTHS.length-1?available-used:MIN_COLUMN_WIDTHS[i]+extra*MIN_COLUMN_WIDTHS[i]/MIN_TABLE_WIDTH;
                        var column=getColumnModel().getColumn(i);column.setPreferredWidth(width);column.setWidth(width);used+=width;
                    }
                }
                super.doLayout();
            }
            @Override protected void paintComponent(Graphics graphics){
                super.paintComponent(graphics);int start=getRowCount()*getRowHeight();
                Graphics g=graphics.create();for(int y=start;y<getHeight();y+=getRowHeight()){
                    g.setColor(((y/getRowHeight())%2)==0?Color.WHITE:new Color(244,245,246));g.fillRect(0,y,getWidth(),getRowHeight());g.setColor(GRID);g.drawLine(0,y,getWidth(),y);
                }
                int x=0;g.setColor(GRID);for(int i=0;i<getColumnCount();i++){x+=getColumnModel().getColumn(i).getWidth();g.drawLine(x-1,start,x-1,getHeight());}g.dispose();
            }
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer,int row,int column){
            Component cell=super.prepareRenderer(renderer,row,column);
            if(cell instanceof JComponent component)component.setToolTipText(Objects.toString(getValueAt(row,column),""));
            if(!isRowSelected(row)){
                Thuoc d=row>=0&&row<displayed.size()?displayed.get(row):null;
                java.time.LocalDate today=java.time.LocalDate.now();
                if(d!=null&&d.expiry()!=null&&d.expiry().isBefore(today)){
                    cell.setBackground(new Color(255,228,228));cell.setForeground(new Color(165,0,0));
                }else if(d!=null&&d.expiry()!=null&&!d.expiry().isAfter(today.plusDays(90))){
                    cell.setBackground(new Color(255,245,214));cell.setForeground(new Color(130,70,0));
                }else{
                    cell.setBackground(row%2==0?Color.WHITE:new Color(244,245,246));cell.setForeground(Color.BLACK);
                }
            }return cell;
        }};
        table.setFont(TEXT);table.setRowHeight(22);table.setGridColor(GRID);table.setIntercellSpacing(new Dimension(1,1));table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(BLUE);table.setSelectionForeground(Color.WHITE);table.getTableHeader().setFont(TEXT);table.getTableHeader().setPreferredSize(new Dimension(100,32));table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer heading=new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable owner,Object value,boolean selected,boolean focus,int row,int column){
                super.getTableCellRendererComponent(owner,value,selected,focus,row,column);setFont(TEXT.deriveFont(12f));setBackground(Color.WHITE);setForeground(Color.BLACK);
                String text=Objects.toString(value,"");int width=owner.getColumnModel().getColumn(column).getWidth()-10;
                if(getFontMetrics(getFont()).stringWidth(text)>width)setText("<html>"+text.replaceFirst(" ","<br>")+"</html>");
                setToolTipText(text);return this;
            }
        };heading.setFont(TEXT.deriveFont(12f));heading.setBackground(Color.WHITE);heading.setForeground(Color.BLACK);heading.setHorizontalAlignment(SwingConstants.LEFT);heading.setBorder(new CompoundBorder(new MatteBorder(0,0,1,1,GRID),new EmptyBorder(0,4,0,0)));table.getTableHeader().setDefaultRenderer(heading);
        for(int i=0;i<MIN_COLUMN_WIDTHS.length;i++){
            table.getColumnModel().getColumn(i).setMinWidth(MIN_COLUMN_WIDTHS[i]);
            table.getColumnModel().getColumn(i).setPreferredWidth(MIN_COLUMN_WIDTHS[i]);
        }
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int column){super.getTableCellRendererComponent(table,value,selected,focus,row,column);setBorder(new EmptyBorder(0,15,0,2));return this;}
            @Override protected void paintComponent(Graphics g){super.paintComponent(g);g.setColor(new Color(100,119,218));g.drawLine(5,0,5,getHeight());g.drawLine(5,getHeight()/2,13,getHeight()/2);}
        });
        table.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)editFullDrug();}
            @Override public void mousePressed(MouseEvent e){showRowMenu(e);}
            @Override public void mouseReleased(MouseEvent e){showRowMenu(e);}
            private void showRowMenu(MouseEvent e){
                if(!e.isPopupTrigger())return;
                int r=table.rowAtPoint(e.getPoint());
                if(r>=0&&r<displayed.size())table.setRowSelectionInterval(r,r);
                JPopupMenu pop=new JPopupMenu();
                JMenuItem m1=new JMenuItem("Sửa thông tin thuốc đầy đủ");m1.addActionListener(ev->editFullDrug());pop.add(m1);
                JMenuItem m2=new JMenuItem("Ánh xạ danh mục / BHYT");m2.addActionListener(ev->editMetadata(false));pop.add(m2);
                JMenuItem m3=new JMenuItem("Xem Thẻ Kho (Lịch sử nhập/xuất)");m3.addActionListener(ev->showStockCard());pop.add(m3);
                pop.addSeparator();
                JMenuItem m4=new JMenuItem("Khóa thuốc (Ngừng sử dụng)");m4.addActionListener(ev->toggleDrugActive(false));pop.add(m4);
                JMenuItem m5=new JMenuItem("Mở khóa thuốc");m5.addActionListener(ev->toggleDrugActive(true));pop.add(m5);
                pop.show(table,e.getX(),e.getY());
            }
        });
        JScrollPane grid=new JScrollPane(table);grid.setColumnHeaderView(table.getTableHeader());grid.setBorder(null);grid.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);grid.getViewport().setBackground(Color.WHITE);content.add(grid,BorderLayout.CENTER);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,treeScroll,content){@Override public void doLayout(){setDividerLocation(Math.max(205,Math.min(245,(int)Math.round(getWidth()*.17))));super.doLayout();}};
        split.setDividerSize(2);split.setResizeWeight(.17);split.setBorder(null);add(split,BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){applyFilter();}public void removeUpdate(javax.swing.event.DocumentEvent e){applyFilter();}public void changedUpdate(javax.swing.event.DocumentEvent e){applyFilter();}});
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control F"),"find");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta F"),"find");
        getActionMap().put("find",new AbstractAction(){public void actionPerformed(ActionEvent e){search.requestFocusInWindow();search.selectAll();}});
        rebuildTree();refreshData();
    }
    private JButton tool(String text,String type,int width){
        Icon icon=new Icon(){public int getIconWidth(){return 26;}public int getIconHeight(){return 26;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(26d/36d,26d/36d);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(0,90,220));g.setStroke(new BasicStroke(3));
                switch(type){
                    case "menu"->{for(int yy:new int[]{8,17,26})g.fillRect(2,yy,32,4);}
                    case "add"->{g.fillRect(16,2,4,32);g.fillRect(2,16,32,4);}
                    case "edit"->{g.drawLine(7,29,27,9);g.drawLine(22,4,31,13);g.drawLine(4,32,12,29);}
                    case "card"->{g.drawRect(4,5,28,26);g.drawLine(4,13,32,13);g.drawLine(10,20,26,20);g.drawLine(10,26,22,26);}
                    case "export"->{g.drawRect(6,16,24,15);g.drawLine(18,4,18,22);g.drawPolyline(new int[]{11,18,25},new int[]{11,4,11},3);}
                    case "refresh"->{g.drawArc(4,4,27,27,35,285);g.fillPolygon(new int[]{25,33,33},new int[]{7,1,14},3);}
                    default->{g.setStroke(new BasicStroke(1.5f));g.drawRect(1,23,10,10);g.drawRect(24,2,10,10);g.setColor(Color.GRAY);g.drawPolyline(new int[]{7,7,28,28},new int[]{23,17,17,12},4);}
                }g.dispose();}
        };
        JButton button=new JButton(text,icon);button.setFont(TEXT);button.setPreferredSize(new Dimension(width,54));button.setBorder(new EmptyBorder(2,4,2,4));button.setContentAreaFilled(false);button.setFocusPainted(false);
        if(!"map".equals(type)){button.setHorizontalTextPosition(SwingConstants.CENTER);button.setVerticalTextPosition(SwingConstants.BOTTOM);}return button;
    }
    private static String normalize(String text){return Normalizer.normalize(Objects.toString(text,""),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT);}
    private String group(Thuoc drug){return drug.group().isBlank()?"Chưa phân loại":drug.group();}
    private List<Thuoc> unique(){Map<Integer,Thuoc> unique=new LinkedHashMap<>();for(var row:all)unique.putIfAbsent(row.id(),row);return List.copyOf(unique.values());}
    private void rebuildTree(){
        sidebar.removeAll();var unique=unique();
        sidebar.add(treeRow("Tất Cả Thuốc ("+unique.size()+")",0,true,selectedGroup==null&&insuranceFilter==null,()->{selectedGroup=null;insuranceFilter=null;rebuildTree();applyFilter();}));
        sidebar.add(treeRow((groupsExpanded?"⊟ ":"⊞ ")+"Loại Thuốc",0,true,false,()->{groupsExpanded=!groupsExpanded;rebuildTree();}));
        Set<String> names=new LinkedHashSet<>(GROUPS);for(var drug:unique)names.add(group(drug));
        if(groupsExpanded)for(var name:names){long count=unique.stream().filter(d->name.equals(group(d))).count();sidebar.add(treeRow(name+" ("+count+")",1,false,name.equals(selectedGroup),()->{selectedGroup=name;insuranceFilter=null;rebuildTree();applyFilter();}));}
        sidebar.add(treeRow((insuranceExpanded?"⊟ ":"⊞ ")+"Nhóm BHYT",0,true,false,()->{insuranceExpanded=!insuranceExpanded;rebuildTree();}));
        if(insuranceExpanded){
            insuredRow("Trong danh mục BHYT","YES",unique.stream().filter(d->Boolean.TRUE.equals(d.insured())).count());
            if(unique.stream().anyMatch(d->Boolean.FALSE.equals(d.insured())))insuredRow("Ngoài danh mục BHYT","NO",unique.stream().filter(d->Boolean.FALSE.equals(d.insured())).count());
            if(unique.stream().anyMatch(d->d.insured()==null))insuredRow("Chưa xác định BHYT","UNKNOWN",unique.stream().filter(d->d.insured()==null).count());
        }
        sidebar.revalidate();sidebar.repaint();
    }
    private void insuredRow(String text,String key,long count){sidebar.add(treeRow(text+" ("+count+")",1,false,key.equals(insuranceFilter),()->{insuranceFilter=key;selectedGroup=null;rebuildTree();applyFilter();}));}
    private JPanel treeRow(String title,int level,boolean bold,boolean selected,Runnable clicked){
        JPanel row=new JPanel(new BorderLayout()){@Override protected void paintComponent(Graphics g){super.paintComponent(g);g.setColor(new Color(110,128,209));g.drawLine(12,0,12,getHeight());if(level>0){g.drawLine(22,0,22,getHeight()/2);g.drawLine(22,getHeight()/2,30,getHeight()/2);}}};
        row.setBackground(selected?BLUE:Color.WHITE);row.setAlignmentX(LEFT_ALIGNMENT);row.setPreferredSize(new Dimension(220,23));row.setMinimumSize(new Dimension(0,23));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,23));
        JLabel label=new JLabel(title);label.setFont(bold?TEXT.deriveFont(Font.BOLD):TEXT.deriveFont(13f));label.setForeground(selected?Color.WHITE:Color.BLACK);label.setBorder(new EmptyBorder(0,level==0?20:32,0,0));row.add(label,BorderLayout.CENTER);row.setToolTipText(title);
        MouseAdapter listener=new MouseAdapter(){@Override public void mousePressed(MouseEvent e){clicked.run();}};row.addMouseListener(listener);label.addMouseListener(listener);return row;
    }
    private void applyFilter(){
        int selectedId=selectedDrug()==null?-1:selectedDrug().id();model.setRowCount(0);displayed.clear();String keyword=normalize(search.getText().trim());
        String selectedWh=Objects.toString(warehouseFilter.getSelectedItem(),"Tất cả kho");
        String expMode=Objects.toString(expiryFilter.getSelectedItem(),"Tất cả HSD");
        java.time.LocalDate today=java.time.LocalDate.now();
        Map<Integer,Thuoc> combined=new LinkedHashMap<>();
        for(var row:all){
            if(selectedGroup!=null&&!selectedGroup.equals(group(row)))continue;
            if(insuranceFilter!=null&&!switch(insuranceFilter){case "YES"->Boolean.TRUE.equals(row.insured());case "NO"->Boolean.FALSE.equals(row.insured());default->row.insured()==null;})continue;
            if(!"Tất cả kho".equals(selectedWh)&&!selectedWh.equals(row.warehouse()))continue;
            if("Đã hết hạn".equals(expMode)&&(row.expiry()==null||!row.expiry().isBefore(today)))continue;
            if("Sắp hết hạn (≤ 90 ngày)".equals(expMode)&&(row.expiry()==null||row.expiry().isBefore(today)||row.expiry().isAfter(today.plusDays(90))))continue;
            if("Hết tồn (= 0)".equals(expMode)&&(row.stock()!=null&&row.stock().signum()>0))continue;
            if(!keyword.isEmpty()&&!normalize(row.code()+" "+row.equivalentCode()+" "+row.name()+" "+row.ingredient()+" "+row.strength()+" "+row.batch()).contains(keyword))continue;
            if(summary.isSelected()){
                Thuoc previous=combined.get(row.id());
                if(previous==null)combined.put(row.id(),row);else combined.put(row.id(),new Thuoc(row.id(),row.version(),row.code(),row.equivalentCode(),row.name(),row.ingredient(),row.strength(),row.concentration(),row.volume(),row.unit(),row.group(),row.insured(),row.traditionalCode(),Objects.equals(previous.warehouse(),row.warehouse())?row.warehouse():"Nhiều kho","",null,previous.stock().add(row.stock())));
            }else displayed.add(row);
        }
        if(summary.isSelected())displayed.addAll(combined.values());
        int selectedRow=0;for(int i=0;i<displayed.size();i++){var row=displayed.get(i);model.addRow(new Object[]{row.code(),row.equivalentCode(),row.name(),row.ingredient(),row.strength(),row.concentration(),row.volume(),row.unit(),row.warehouse(),summary.isSelected()?"":row.batch(),summary.isSelected()?"":row.expiry(),row.stock(),row.traditionalCode()});if(row.id()==selectedId)selectedRow=i;}
        if(!displayed.isEmpty())table.setRowSelectionInterval(selectedRow,selectedRow);
    }
    public void refreshData(){
        if(loadWorker!=null)loadWorker.cancel(true);long request=++generation;
        var auth=KhamBenhPhienNguCanh.current();boolean canRead=auth!=null&&Set.of("ADMIN","PHARMACIST","WAREHOUSE_MANAGER").contains(auth.roleCode());
        boolean canWrite=canRead&&Set.of("ADMIN","PHARMACIST").contains(auth.roleCode());
        add.setEnabled(canWrite&&services!=null&&services.saveDrug()!=null);edit.setEnabled(canWrite&&services!=null&&services.saveDrug()!=null);mapModern.setEnabled(canWrite);mapTraditional.setEnabled(canWrite);
        if(!canRead||read==null){clear();notice.setText("Cần phiên đăng nhập có quyền nhà thuốc.");return;}
        notice.setText("Đang tải...");refresh.setEnabled(false);
        loadWorker=new SwingWorker<>(){
            protected List<Thuoc> doInBackground(){return read.getDrugDirectory(auth.facilityId(),warehouseId);}
            protected void done(){
                if(request!=generation||isCancelled())return;
                refresh.setEnabled(true);var current=KhamBenhPhienNguCanh.current();
                if(current==null||!current.sessionId().equals(auth.sessionId())){clear();notice.setText("Phiên làm việc đã thay đổi.");return;}
                try{all=List.copyOf(get());rebuildWarehouseFilter();rebuildTree();applyFilter();notice.setText(all.isEmpty()?"Không có thuốc.":"");}
                catch(InterruptedException ex){Thread.currentThread().interrupt();clear();notice.setText("Tải dữ liệu bị gián đoạn.");}
                catch(ExecutionException ex){clear();notice.setText("Không thể tải danh mục từ CSDL. Hãy Làm Mới.");}
            }
        };loadWorker.execute();
    }
    private void rebuildWarehouseFilter(){
        String prev=Objects.toString(warehouseFilter.getSelectedItem(),"Tất cả kho");
        Set<String> names=new LinkedHashSet<>();names.add("Tất cả kho");
        for(var d:all)if(d.warehouse()!=null&&!d.warehouse().isBlank())names.add(d.warehouse());
        warehouseFilter.setModel(new DefaultComboBoxModel<>(names.toArray(String[]::new)));
        if(names.contains(prev))warehouseFilter.setSelectedItem(prev);
    }
    private void clear(){all=List.of();displayed.clear();model.setRowCount(0);rebuildTree();refresh.setEnabled(true);}
    private Thuoc selectedDrug(){int row=table.getSelectedRow();return row>=0&&row<displayed.size()?displayed.get(row):null;}
    private void editMetadata(boolean traditional){
        var drug=selectedDrug();if(drug==null){message("Vui lòng chọn một thuốc.");return;}
        if(GraphicsEnvironment.isHeadless())return;
        JTextField equivalent=new JTextField(drug.equivalentCode()),herbal=new JTextField(drug.traditionalCode()),concentration=new JTextField(drug.concentration()),volume=new JTextField(drug.volume());
        JComboBox<String> group=new JComboBox<>(GROUPS.toArray(String[]::new));group.setEditable(true);group.setSelectedItem(drug.group());
        JComboBox<String> insured=new JComboBox<>(new String[]{"Chưa xác định","Trong danh mục BHYT","Ngoài danh mục BHYT"});insured.setSelectedIndex(drug.insured()==null?0:drug.insured()?1:2);
        JPanel form=new JPanel(new GridLayout(0,2,8,8));form.add(new JLabel("Mã tương đương (tân dược):"));form.add(equivalent);form.add(new JLabel("Mã danh mục YHCT:"));form.add(herbal);form.add(new JLabel("Loại thuốc:"));form.add(group);form.add(new JLabel("Nồng độ:"));form.add(concentration);form.add(new JLabel("Thể tích:"));form.add(volume);form.add(new JLabel("Nhóm BHYT:"));form.add(insured);
        JTextArea note=new JTextArea("Nhập mã ánh xạ đã được đơn vị xác nhận. Trạng thái danh mục BHYT không thay thế quy tắc tính quyền lợi thanh toán.");note.setLineWrap(true);note.setWrapStyleWord(true);note.setEditable(false);note.setOpaque(false);
        JPanel content=new JPanel(new BorderLayout(0,10));content.add(new JLabel(drug.code()+" - "+drug.name()),BorderLayout.NORTH);content.add(form,BorderLayout.CENTER);content.add(note,BorderLayout.SOUTH);content.setPreferredSize(new Dimension(640,290));
        if(JOptionPane.showConfirmDialog(this,content,traditional?"Ánh xạ danh mục học cổ truyền":"Thông tin danh mục / Ánh xạ tân dược",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        var updated=new Thuoc(drug.id(),drug.version(),drug.code(),equivalent.getText().trim(),drug.name(),drug.ingredient(),drug.strength(),concentration.getText().trim(),volume.getText().trim(),drug.unit(),Objects.toString(group.getSelectedItem(),"").trim(),insured.getSelectedIndex()==0?null:insured.getSelectedIndex()==1,herbal.getText().trim(),drug.warehouse(),drug.batch(),drug.expiry(),drug.stock());
        var auth=KhamBenhPhienNguCanh.current();mapModern.setEnabled(false);mapTraditional.setEnabled(false);
        new SwingWorker<vn.clinicmanagement.application.common.KetQua<Void>,Void>(){
            protected vn.clinicmanagement.application.common.KetQua<Void> doInBackground(){return new CapNhatDanhMucThuocDichVu(read).save(updated,warehouseId,auth);}
            protected void done(){try{var result=get();if(result.isFailure())message(result.getError().orElseThrow().message());}catch(Exception ex){message("Không thể lưu thông tin thuốc.");}finally{refreshData();}}
        }.execute();
    }
    private void createDrug(){
        if(GraphicsEnvironment.isHeadless()||services==null||services.saveDrug()==null)return;
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"EHC - Thêm Thuốc Mới",Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.setContentPane(new ThemThuocManHinh(services.saveDrug(),this::refreshData));dialog.pack();
        Rectangle bounds=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        dialog.setSize(Math.min(dialog.getWidth(),bounds.width),Math.min(dialog.getHeight(),bounds.height));dialog.setLocationRelativeTo(this);dialog.setVisible(true);
    }
    private void editFullDrug(){
        var drug=selectedDrug();if(drug==null){message("Vui lòng chọn một thuốc để sửa.");return;}
        if(GraphicsEnvironment.isHeadless()||read==null)return;
        var auth=KhamBenhPhienNguCanh.current();if(auth==null||!auth.isValid())return;
        boolean canWrite=Set.of("ADMIN","PHARMACIST").contains(auth.roleCode())&&services!=null&&services.saveDrug()!=null;
        var saveHandler=canWrite?services.saveDrug():null;
        String title=(canWrite?"EHC - Sửa Thuốc: ":"EHC - Thông Tin Thuốc: ")+drug.code();
        new SwingWorker<Map<String,String>,Void>(){
            protected Map<String,String> doInBackground(){return read.getDrugFullValues(drug.id(),auth.facilityId());}
            protected void done(){
                try{
                    Map<String,String> vals=get();
                    JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(DanhSachThuocTrongKhoManHinh.this),title,Dialog.ModalityType.MODELESS);
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dialog.setContentPane(new ThemThuocManHinh(saveHandler,drug.id(),vals,DanhSachThuocTrongKhoManHinh.this::refreshData));
                    dialog.pack();
                    Rectangle bounds=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    dialog.setSize(Math.min(dialog.getWidth(),bounds.width),Math.min(dialog.getHeight(),bounds.height));
                    dialog.setLocationRelativeTo(DanhSachThuocTrongKhoManHinh.this);dialog.setVisible(true);
                }catch(Exception ex){message("Không thể tải chi tiết thuốc.");}
            }
        }.execute();
    }
    private void toggleDrugActive(boolean active){
        var drug=selectedDrug();if(drug==null){message("Vui lòng chọn một thuốc.");return;}
        var auth=KhamBenhPhienNguCanh.current();
        if(read==null||auth==null||!Set.of("ADMIN","PHARMACIST").contains(auth.roleCode())){message("Không có quyền thay đổi trạng thái thuốc.");return;}
        if(!GraphicsEnvironment.isHeadless()){
            int ok=JOptionPane.showConfirmDialog(this,(active?"Mở khóa sử dụng thuốc ":"Khóa (ngừng sử dụng) thuốc ")+drug.code()+" - "+drug.name()+"?","Xác nhận",JOptionPane.YES_NO_OPTION);
            if(ok!=JOptionPane.YES_OPTION)return;
        }
        new SwingWorker<Void,Void>(){
            protected Void doInBackground(){read.setDrugActive(drug.id(),active,auth.facilityId(),auth.employeeId());return null;}
            protected void done(){
                try{get();message(active?"Đã mở khóa thuốc "+drug.code():"Đã khóa thuốc "+drug.code());refreshData();}
                catch(Exception ex){message("Không thể cập nhật trạng thái thuốc.");}
            }
        }.execute();
    }
    private void showStockCard(){
        var drug=selectedDrug();if(drug==null){message("Vui lòng chọn một thuốc để xem thẻ kho.");return;}
        var auth=KhamBenhPhienNguCanh.current();if(read==null||auth==null||!auth.isValid()||GraphicsEnvironment.isHeadless())return;
        new SwingWorker<List<NhaThuocDanhSachReadCongKetNoi.TheKhoDong>,Void>(){
            protected List<NhaThuocDanhSachReadCongKetNoi.TheKhoDong> doInBackground(){return read.getStockCard(drug.id(),auth.facilityId(),warehouseId);}
            protected void done(){
                try{
                    var rows=get();
                    DefaultTableModel dm=new DefaultTableModel(new String[]{"Thời gian","Kho","Số lô","Loại GD","Tồn trước","Thay đổi","Tồn sau","Chứng từ","Người thực hiện"},0){
                        @Override public boolean isCellEditable(int r,int c){return false;}
                    };
                    var fmt=java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                    for(var r:rows){
                        dm.addRow(new Object[]{r.time()!=null?r.time().format(fmt):"",r.warehouse(),r.batch(),r.type(),
                                r.before().stripTrailingZeros().toPlainString(),r.change().stripTrailingZeros().toPlainString(),
                                r.after().stripTrailingZeros().toPlainString(),r.docCode(),r.actor()});
                    }
                    JTable t=new JTable(dm);t.setRowHeight(24);
                    JPanel root=new JPanel(new BorderLayout(0,8));root.setBorder(new EmptyBorder(10,10,10,10));
                    root.add(new JLabel("<html><b>Thẻ Kho: "+drug.code()+" — "+drug.name()+"</b> ("+rows.size()+" giao dịch gần nhất)</html>"),BorderLayout.NORTH);
                    root.add(new JScrollPane(t),BorderLayout.CENTER);root.setPreferredSize(new Dimension(920,420));
                    JOptionPane.showMessageDialog(DanhSachThuocTrongKhoManHinh.this,root,"Thẻ Kho - "+drug.code(),JOptionPane.PLAIN_MESSAGE);
                }catch(Exception ex){message("Không thể tải thẻ kho: "+ex.getMessage());}
            }
        }.execute();
    }
    private void exportCsv(){
        if(GraphicsEnvironment.isHeadless())return;
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("danh-sach-thuoc-trong-kho.csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try(var writer=java.nio.file.Files.newBufferedWriter(chooser.getSelectedFile().toPath(),java.nio.charset.StandardCharsets.UTF_8)){
            writer.write('\ufeff');
            for(int row=-1;row<table.getRowCount();row++){
                for(int col=0;col<table.getColumnCount();col++){
                    if(col>0)writer.write(',');
                    String val=Objects.toString(row<0?table.getColumnModel().getColumn(col).getHeaderValue():table.getValueAt(row,col),"");
                    if(val.startsWith("=")||val.startsWith("+")||val.startsWith("-")||val.startsWith("@"))val="'"+val;
                    writer.write("\""+val.replace("\"","\"\"")+"\"");
                }
                writer.newLine();
            }
            message("Đã xuất "+table.getRowCount()+" dòng ra tệp CSV.");
        }catch(java.io.IOException ex){message("Không thể xuất CSV: "+ex.getMessage());}
    }
    private void message(String text){if(!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(this,text,"Danh sách thuốc",JOptionPane.INFORMATION_MESSAGE);}
    public JTable getTable(){return table;}
    public JTextField getSearchField(){return search;}
    public JCheckBox getSummaryCheckBox(){return summary;}
    public JPanel getSidebar(){return sidebar;}
    public boolean isLoading(){return loadWorker!=null&&!loadWorker.isDone();}
    @Override public void removeNotify(){if(loadWorker!=null)loadWorker.cancel(true);generation++;super.removeNotify();}
}
