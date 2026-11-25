package vn.clinicmanagement.desktop.modules.inventory;


import vn.clinicmanagement.desktop.modules.print.inventory.TuTrucPhieuNhapBuCoSoHopThoai;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Bang;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.infrastructure.persistence.inventory.TuTrucRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.*;

/** Danh sách phiếu tủ trực theo ảnh mẫu; các chi tiết mở trong chính tab hiện tại. */
public final class TuTrucVatTuManHinh extends CoSoManHinhManHinh {
    static final Color GRID = new Color(207,210,215), SELECTED = new Color(0,145,255);
    static final String[] COLUMNS={"", "Trạng Thái Phiếu", "Ngày Tạo", "Ngày Y Lệnh", "Mã Phiếu", "Mã Phiếu TH",
            "Loại Phiếu", "Đối Tượng BN", "Khoa Lập Phiếu", "Nơi Lập Phiếu"};
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy"), TIME=DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final TuTrucCongKetNoi data;
    final TuTrucThanhCongCu toolbar=new TuTrucThanhCongCu();
    final JComboBox<Kho> cabinets=new JComboBox<>();
    final JButton all=new JButton("− Tất Cả Phiếu (0)");
    final JTree categories=new JTree(new DefaultMutableTreeNode("root"));
    final JTextField search=new JTextField(){
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(getText().isEmpty()){
            g.setFont(getFont());g.setColor(new Color(102,102,81));
            g.drawString("Tìm kiếm (Ctrl+F)",6,(getHeight()-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent());
        }}
    };
    final DefaultTableModel model=readonly(COLUMNS);
    final JTable table=grid(model);
    final JSplitPane columns;
    final JSplitPane rows;
    final JPanel lower=new JPanel(new BorderLayout());
    private final JLabel feedback=new JLabel();
    private List<Phieu> tickets=List.of(), displayed=List.of();
    private String selectedType="";
    private int cabinetGeneration, ticketGeneration, detailGeneration;
    private boolean changingCabinets, changingTree;
    private TuTrucNhapBuManHinh draft;

    public TuTrucVatTuManHinh() { this(production(null)); }
    public TuTrucVatTuManHinh(PhienVongDoiDichVu lifecycle) { this(production(lifecycle)); }
    public TuTrucVatTuManHinh(TuTrucCongKetNoi data) {
        super("cabinet","Tủ Trực Vật Tư","🗄");
        this.data=Objects.requireNonNull(data);
        setBackground(Color.WHITE); add(toolbar,BorderLayout.NORTH);
        JPanel left=new JPanel(new BorderLayout()); left.setBackground(Color.WHITE);
        left.setMinimumSize(new Dimension(190,0));
        cabinets.setName("tuTrucChonTu"); cabinets.setFont(UngDungPhongChu.TU_TRUC_TEN_TU);
        cabinets.setForeground(Color.RED); cabinets.setBackground(Color.WHITE);
        cabinets.setPreferredSize(new Dimension(250,28));
        cabinets.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                super.getListCellRendererComponent(list,value,index,selected,focus);
                if(value==null)setText("(Chọn tủ trực)");setForeground(selected?Color.WHITE:Color.RED);return this;
            }
        });
        JPanel treeArea=new JPanel(new BorderLayout());treeArea.setBackground(Color.WHITE);
        all.setFont(UngDungPhongChu.TU_TRUC_BOLD);all.setHorizontalAlignment(SwingConstants.LEFT);
        all.setBorder(new EmptyBorder(2,10,2,0));all.setPreferredSize(new Dimension(250,26));
        all.setFocusPainted(false);all.addActionListener(e->{selectedType="";categories.clearSelection();applyFilter();});
        treeArea.add(all,BorderLayout.NORTH);
        categories.setRootVisible(false);categories.setShowsRootHandles(true);categories.setRowHeight(24);
        categories.setFont(UngDungPhongChu.TU_TRUC);categories.setBackground(Color.WHITE);
        categories.putClientProperty("JTree.lineStyle","Angled");
        if(categories.getUI() instanceof javax.swing.plaf.basic.BasicTreeUI ui){
            ui.setExpandedIcon(vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong.createMinusBoxIcon());
            ui.setLeftChildIndent(10);ui.setRightChildIndent(19);
        }
        categories.setCellRenderer(new DefaultTreeCellRenderer(){
            @Override public Component getTreeCellRendererComponent(JTree t,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean focus){
                super.getTreeCellRendererComponent(t,value,selected,expanded,leaf,row,focus);
                setLeafIcon(null);setClosedIcon(null);setOpenIcon(null);
                setFont(row==0?UngDungPhongChu.TU_TRUC_BOLD:UngDungPhongChu.TU_TRUC);
                setTextSelectionColor(Color.WHITE);setBackgroundSelectionColor(SELECTED);return this;
            }
        });
        categories.addTreeSelectionListener(e->{if(changingTree)return;
            var node=(DefaultMutableTreeNode)categories.getLastSelectedPathComponent();
            selectedType=node!=null && node.getUserObject() instanceof Category c?c.type():"";applyFilter();});
        JScrollPane treeScroll=new JScrollPane(categories);treeScroll.setBorder(null);treeArea.add(treeScroll);
        left.add(cabinets,BorderLayout.NORTH);left.add(treeArea,BorderLayout.CENTER);
        JPanel right=new JPanel(new BorderLayout());right.setBackground(Color.WHITE);
        JPanel searchBar=new JPanel(new BorderLayout());searchBar.setPreferredSize(new Dimension(100,28));
        search.setName("tuTrucTimNhanh");search.setFont(UngDungPhongChu.TU_TRUC);
        search.setBackground(new Color(255,255,193));
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(172,172,151)),new EmptyBorder(0,6,0,4)));
        JButton find=new JButton(TuTrucThanhCongCu.scaled(vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong.createSearchIcon(),18));
        find.setPreferredSize(new Dimension(28,26));find.addActionListener(e->applyFilter());searchBar.add(search);searchBar.add(find,BorderLayout.EAST);
        right.add(searchBar,BorderLayout.NORTH);
        configureTable();
        JScrollPane scroll=new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(GRID));scroll.getViewport().setBackground(Color.WHITE);
        lower.setMinimumSize(new Dimension(0,160));lower.setVisible(false);
        rows=new JSplitPane(JSplitPane.VERTICAL_SPLIT,scroll,lower);rows.setBorder(null);rows.setDividerSize(0);rows.setResizeWeight(.56);
        right.add(rows,BorderLayout.CENTER);
        feedback.setFont(UngDungPhongChu.SMALL);feedback.setForeground(new Color(176,44,20));feedback.setBorder(new EmptyBorder(2,6,2,6));feedback.setVisible(false);
        right.add(feedback,BorderLayout.SOUTH);
        columns=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);columns.setBorder(null);
        columns.setDividerSize(3);columns.setContinuousLayout(true);columns.setResizeWeight(.18);columns.setDividerLocation(250);
        add(columns,BorderLayout.CENTER);
        cabinets.addActionListener(e->{if(!changingCabinets){selectedType="";clearDetails();loadTickets();}});
        toolbar.search.addActionListener(e->loadTickets());toolbar.code.addActionListener(e->loadTickets());
        toolbar.code.getDocument().addDocumentListener(listener(this::applyFilter));
        search.getDocument().addDocumentListener(listener(this::applyFilter));
        wireMenus();updateTree();
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control F"),"find");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta F"),"find");
        getActionMap().put("find",new AbstractAction(){public void actionPerformed(ActionEvent e){search.requestFocusInWindow();search.selectAll();}});
    }

    private static TuTrucCongKetNoi production(PhienVongDoiDichVu lifecycle){
        var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow);var provider=new JdbcKetNoiNhaCungCap(tx);
        var employees=new vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository(provider,uow);
        var system=new vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository(provider,uow);
        var active=lifecycle!=null?lifecycle:(vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi)(id->java.util.Optional.empty());
        var authorization=new vn.clinicmanagement.application.authorization.PhanQuyenDichVu(active,employees,system);
        return new TuTrucRepository(provider,tx,new JdbcKiemToanRepository(provider,uow,CoSoDuLieuKetNoi::getConnection),authorization);
    }
    @Override public void refreshData(){
        var context=KhamBenhPhienNguCanh.current();
        if(context==null){clearData();message("Vui lòng đăng nhập để xem tủ trực.");return;}
        int generation=++cabinetGeneration; int old=selectedCabinetId();
        clearData();
        String session=context.sessionId().toString();
        async(()->data.getCabinets(context),items->{
            if(generation!=cabinetGeneration||!current(session))return;
            changingCabinets=true; cabinets.removeAllItems();items.forEach(cabinets::addItem);
            items.stream().filter(k->k.id()==old).findFirst().ifPresent(cabinets::setSelectedItem);changingCabinets=false;
            if(items.isEmpty()){clearData();message("Cơ sở hiện tại chưa có tủ trực đang hoạt động.");}
            else loadTickets();
        },error->{if(generation==cabinetGeneration&&current(session)){changingCabinets=true;cabinets.removeAllItems();changingCabinets=false;clearData();message("Không tải được danh sách tủ trực. Chọn Menu → Làm mới để thử lại.");}});
    }
    private void loadTickets(){
        int generation=++ticketGeneration;var context=KhamBenhPhienNguCanh.current();int cabinet=selectedCabinetId();
        if(context==null||cabinet<=0){tickets=List.of();applyFilter();return;}
        java.time.LocalDateTime[] range;
        try{range=toolbar.range();}catch(IllegalArgumentException ex){message(ex.getMessage());return;}
        tickets=List.of();applyFilter();message("Đang tải phiếu…");String session=context.sessionId().toString();
        async(()->data.getTickets(context,cabinet,range[0],range[1]),items->{
            if(generation!=ticketGeneration||cabinet!=selectedCabinetId()||!current(session))return;
            tickets=List.copyOf(items);message("");applyFilter();
        },error->{if(generation==ticketGeneration&&current(session))message("Không tải được phiếu tủ trực. Vui lòng tìm kiếm lại.");});
    }
    private void clearData(){++ticketGeneration;clearDetails();tickets=List.of();applyFilter();}
    private void applyFilter(){
        String code=normalize(toolbar.code.getText()),query=normalize(search.getText());
        displayed=tickets.stream().filter(p->selectedType.isEmpty()||selectedType.equals(p.type()))
                .filter(p->normalize(p.code()).contains(code))
                .filter(p->normalize(p.code()+" "+status(p)+" "+p.type()+" "+p.department()+" "+p.location()+" "+p.executionCode()+" "+patient(p.patientType())).contains(query)).toList();
        model.setRowCount(0);
        for(var p:displayed)model.addRow(new Object[]{p,status(p),p.createdAt()==null?"":TIME.format(p.createdAt()),
                p.orderDate()==null?"":DATE.format(p.orderDate()),p.code(),p.executionCode(),p.type(),patient(p.patientType()),p.department(),p.location()});
        updateTree();if(!displayed.isEmpty())table.setRowSelectionInterval(0,0);
    }
    private record Category(String type,int count){@Override public String toString(){return type+" ("+count+")";}}
    private void updateTree(){
        all.setText("− Tất Cả Phiếu ("+tickets.size()+")");all.setBackground(selectedType.isEmpty()?SELECTED:Color.WHITE);
        all.setForeground(selectedType.isEmpty()?Color.WHITE:Color.BLACK);
        changingTree=true;
        var root=new DefaultMutableTreeNode("root");var group=new DefaultMutableTreeNode("Loại Phiếu");root.add(group);
        Set<String> types=new LinkedHashSet<>();types.add("Nhập bù");tickets.forEach(p->types.add(p.type()));TreePath selected=null;
        for(String type:types){var node=new DefaultMutableTreeNode(new Category(type,(int)tickets.stream().filter(p->type.equals(p.type())).count()));group.add(node);
            if(type.equals(selectedType))selected=new TreePath(node.getPath());}
        categories.setModel(new DefaultTreeModel(root));categories.expandRow(0);if(selected!=null)categories.setSelectionPath(selected);changingTree=false;
    }
    private void configureTable(){
        table.setName("tuTrucPhieu");table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths={28,150,150,115,100,115,150,135,148,210};
        for(int i=0;i<widths.length;i++){var c=table.getColumnModel().getColumn(i);c.setPreferredWidth(widths[i]);c.setWidth(widths[i]);}
        table.getTableHeader().setFont(UngDungPhongChu.TU_TRUC);table.getTableHeader().setPreferredSize(new Dimension(100,34));
        table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setBackground(new Color(250,250,250));
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
                super.getTableCellRendererComponent(t,value,false,false,row,column);setFont(UngDungPhongChu.TU_TRUC_DU_LIEU);
                setBorder(new EmptyBorder(0,4,0,3));setHorizontalAlignment(column>=2&&column<=5?CENTER:LEFT);setIcon(null);
                var p=displayed.get(t.convertRowIndexToModel(row));setForeground("DA_HUY".equals(p.status())?Color.RED:Color.BLACK);
                setBackground(selected&&column==1?new Color(182,250,252):Color.WHITE);
                if(column==0){setText("");setHorizontalAlignment(CENTER);setIcon(dot(p.status()));}
                return this;
            }
        });
        table.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e))openDetails();}});
        table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"details");
        table.getActionMap().put("details",new AbstractAction(){public void actionPerformed(ActionEvent e){openDetails();}});
    }
    private void wireMenus(){
        JPopupMenu menu=new JPopupMenu();item(menu,"Làm mới",this::refreshData);item(menu,"Phân khoa/phòng quản lý tủ",this::manageCabinetDepartments);item(menu,"Xử lý tiêu hao đang chờ",this::processPendingConsumption);item(menu,"Xuất danh sách CSV",this::export);item(menu,"Đóng chi tiết",this::clearDetails);popup(toolbar.menu,menu);
        JPopupMenu report=new TuTrucBaoCaoMenu(title->{
            if("Báo Cáo Xuất Nhập Tồn".equals(title)){
                new TuTrucBaoCaoSoTonHopThoai(SwingUtilities.getWindowAncestor(this),data).setVisible(true);
            }else if("Báo Cáo Xuất Hao Phí Khoa Phòng".equals(title)){
                new TuTrucBaoCaoSoTonHopThoai(SwingUtilities.getWindowAncestor(this),data).setVisible(true);
            }else{
                message("Đã chọn: "+title);
            }
        });
        popup(toolbar.report,report);
        JPopupMenu list=new JPopupMenu();
        item(list,"Danh sách vật tư trong kho",this::openWarehouseInventoryDialog);
        popup(toolbar.list,list);
        toolbar.setOnCreateTicketListener(this::createTicketSelected);
        toolbar.getCreateTicketMenu().addPopupMenuListener(new PopupMenuListener(){
            public void popupMenuWillBecomeVisible(PopupMenuEvent e){
                var c=KhamBenhPhienNguCanh.current();
                toolbar.getCreateTicketMenu().setActionsEnabled(selectedCabinetId()>0&&c!=null);
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}
            public void popupMenuCanceled(PopupMenuEvent e){}
        });
    }
    private void manageCabinetDepartments(){
        var context=KhamBenhPhienNguCanh.current();int cabinet=selectedCabinetId();
        if(context==null||cabinet<=0){message("Chọn tủ trực trước khi cấu hình phạm vi khoa/phòng.");return;}
        String session=context.sessionId().toString();message("Đang tải phạm vi khoa/phòng…");
        async(()->data.getCabinetDepartments(context,cabinet),departments->{
            if(cabinet!=selectedCabinetId()||!current(session))return;
            if(departments.isEmpty()){message("Cơ sở chưa có khoa/phòng hoạt động.");return;}
            JList<KhoaTuTruc> list=new JList<>(departments.toArray(KhoaTuTruc[]::new));list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setCellRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean selected,boolean focus){
                super.getListCellRendererComponent(l,v,i,selected,focus);if(v instanceof KhoaTuTruc d)setText((d.assigned()?"✓  ":"   ")+d.name());return this;}});
            int[] selected=java.util.stream.IntStream.range(0,departments.size()).filter(i->departments.get(i).assigned()).toArray();list.setSelectedIndices(selected);
            int answer=JOptionPane.showConfirmDialog(this,new JScrollPane(list),"Phân khoa/phòng quản lý tủ — "+cabinets.getSelectedItem(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(answer!=JOptionPane.OK_OPTION||cabinet!=selectedCabinetId()||!current(session))return;
            List<Integer> ids=list.getSelectedValuesList().stream().map(KhoaTuTruc::id).toList();message("Đang lưu phạm vi tủ…");
            async(()->data.saveCabinetDepartments(context,cabinet,ids),result->{
                if(cabinet!=selectedCabinetId()||!current(session))return;
                if(result!=null&&result.isSuccess()){message("Đã cập nhật phạm vi khoa/phòng. Đang tải lại danh sách tủ…");refreshData();}
                else message(result==null?"Không lưu được phạm vi tủ.":result.getError().map(e->e.message()).orElse("Không lưu được phạm vi tủ."));
            },error->{if(cabinet==selectedCabinetId()&&current(session))message("Không xác nhận được phạm vi đã lưu. Tải lại trước khi thử lại.");});
        },error->{if(cabinet==selectedCabinetId()&&current(session))message("Không tải được phạm vi khoa/phòng của tủ.");});
    }
    private void processPendingConsumption(){
        var context=KhamBenhPhienNguCanh.current();if(context==null){message("Phiên làm việc không hợp lệ.");return;}String session=context.sessionId().toString();message("Đang tải tiêu hao đang chờ…");
        async(()->data.getPendingConsumption(context),pending->{if(!current(session))return;
            if(pending.isEmpty()){message("Không có tiêu hao đang chờ trong khoa/phòng hiện tại.");return;}
            Object selected=JOptionPane.showInputDialog(this,"Chọn tiêu hao để thử ghi sổ lại sau khi đã bổ sung tồn:","Tiêu hao đang chờ",JOptionPane.QUESTION_MESSAGE,null,pending.toArray(),pending.get(0));
            if(!(selected instanceof vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.ChoXuLy event))return;
            async(()->data.retryPendingConsumption(context,event.id()),result->{if(!current(session))return;
                if(result!=null&&result.isSuccess()&&"POSTED".equals(result.getValue().map(v->v.status()).orElse("")))message("Đã ghi sổ tiêu hao. Tồn và ledger đã cập nhật.");
                else if(result!=null&&result.isSuccess())message("Vẫn thiếu tồn khả dụng; tiêu hao tiếp tục ở trạng thái PENDING.");
                else message(result==null?"Không xử lý được tiêu hao.":result.getError().map(e->e.message()).orElse("Không xử lý được tiêu hao."));
            },error->{if(current(session))message("Không xác nhận được kết quả xử lý tiêu hao đang chờ.");});
        },error->{if(current(session))message("Không tải được tiêu hao đang chờ.");});
    }
    private void createTicketSelected(String type){
        var context=KhamBenhPhienNguCanh.current();
        if(context==null){
            message("Phiên hiện tại không có quyền lập phiếu tủ trực.");return;
        }
        if(selectedCabinetId()<=0){message("Vui lòng chọn tủ trực trước khi tạo phiếu.");return;}
        if("Nhập Bù Cơ Số Tủ Trực".equals(type)){
            message("");
            openReplenishment((Kho)cabinets.getSelectedItem(),null);
        }
        else message(type+": hiện chưa hỗ trợ lập và lưu loại phiếu này.");
    }
    private void openWarehouseInventoryDialog(){
        Window owner=SwingUtilities.getWindowAncestor(this);
        var dialog=new TuTrucVatTuTrongKhoHopThoai(owner,data,selectedCabinetId());
        dialog.setVisible(true);
    }
    private void openDetails(){
        if(draft!=null){draft.dispose();draft=null;}
        int row=table.getSelectedRow();var context=KhamBenhPhienNguCanh.current();int cabinet=selectedCabinetId();
        if(row<0||context==null||cabinet<=0)return;Phieu ticket=displayed.get(table.convertRowIndexToModel(row));
        int generation=++detailGeneration;String session=context.sessionId().toString();
        async(()->data.getDetails(context,cabinet,ticket),detail->{if(generation==detailGeneration&&cabinet==selectedCabinetId()&&current(session)){
            if(GraphicsEnvironment.isHeadless())showDetails("Chi tiết phiếu "+ticket.code(),scrollFor(grid(bang(detail))));
            else new TuTrucPhieuNhapBuCoSoHopThoai(SwingUtilities.getWindowAncestor(this),data,(Kho)cabinets.getSelectedItem(),ticket,detail,this::loadTickets).setVisible(true);
        }},error->{if(generation==detailGeneration&&current(session))message("Không tải được chi tiết phiếu.");});
    }
    private void openReplenishment(Kho targetCabinet,Integer sourceWarehouseId){
        var context=KhamBenhPhienNguCanh.current();var cabinet=targetCabinet!=null?targetCabinet:(Kho)cabinets.getSelectedItem();if(context==null||cabinet==null)return;
        clearDetails();
        draft=new TuTrucNhapBuManHinh(data,cabinet,sourceWarehouseId,()->{draft=null;clearDetails();loadTickets();});
        ++detailGeneration;showDetails("Nhập bù — "+cabinet.name(),draft);
    }
    private void showDetails(String title,JComponent content){
        if(draft!=null&&content!=draft){draft.dispose();draft=null;}
        lower.removeAll();JPanel heading=new JPanel(new BorderLayout());heading.setBackground(new Color(235,241,247));
        JLabel text=new JLabel(title);text.setFont(UngDungPhongChu.BODY_BOLD);text.setBorder(new EmptyBorder(5,8,5,8));heading.add(text);
        JButton close=new JButton("Đóng ×");close.setFont(UngDungPhongChu.BODY);close.setPreferredSize(new Dimension(80,24));close.addActionListener(e->clearDetails());heading.add(close,BorderLayout.EAST);
        lower.add(heading,BorderLayout.NORTH);lower.add(content);lower.setVisible(true);rows.setDividerSize(4);rows.setDividerLocation(.55);lower.revalidate();lower.repaint();
    }
    private void clearDetails(){++detailGeneration;if(draft!=null)draft.dispose();draft=null;lower.removeAll();lower.setVisible(false);rows.setDividerSize(0);rows.setDividerLocation(1.0);}
    @Override public void onHide(){++cabinetGeneration;++ticketGeneration;clearDetails();}
    int selectedCabinetId(){var k=(Kho)cabinets.getSelectedItem();return k==null?0:k.id();}
    void message(String text){feedback.setText(text);feedback.setVisible(text!=null&&!text.isBlank());}
    private void export(){
        JFileChooser picker=new JFileChooser();picker.setSelectedFile(new File("phieu-tu-truc.csv"));
        if(picker.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        File file=picker.getSelectedFile();if(file.exists()&&JOptionPane.showConfirmDialog(this,"Ghi đè tệp đã có?","Xuất CSV",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(var writer=new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8)){
            writer.write('\ufeff');for(int r=-1;r<model.getRowCount();r++){
                for(int c=1;c<model.getColumnCount();c++){if(c>1)writer.write(',');String cell=String.valueOf(r<0?model.getColumnName(c):model.getValueAt(r,c));
                    if(cell.matches("^[=+@\\-].*"))cell="'"+cell;writer.write('"'+cell.replace("\"","\"\"")+'"');}
                writer.write("\r\n");}
            message("Đã xuất "+model.getRowCount()+" phiếu.");
        }catch(IOException ex){message("Không ghi được tệp CSV.");}
    }
    public static <T> void async(Supplier<T> work,Consumer<T> success,Consumer<Exception> failure){new SwingWorker<T,Void>(){
        protected T doInBackground(){return work.get();}protected void done(){try{success.accept(get());}catch(InterruptedException ex){Thread.currentThread().interrupt();failure.accept(ex);}catch(Exception ex){failure.accept(ex);}}
    }.execute();}
    public static boolean current(String session){var c=KhamBenhPhienNguCanh.current();return c!=null&&c.sessionId().toString().equals(session);}
    public static String normalize(String s){return java.text.Normalizer.normalize(s==null?"":s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).trim();}
    static DocumentListener listener(Runnable action){return new DocumentListener(){public void insertUpdate(DocumentEvent e){action.run();}public void removeUpdate(DocumentEvent e){action.run();}public void changedUpdate(DocumentEvent e){action.run();}};}
    static DefaultTableModel readonly(String[] columns){return new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int row,int column){return false;}};}
    static DefaultTableModel bang(Bang b){var m=readonly(b.columns().toArray(String[]::new));b.rows().forEach(r->m.addRow(r.toArray()));return m;}
    static JScrollPane scrollFor(JTable table){var scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());return scroll;}
    static JTable grid(DefaultTableModel model){JTable t=new JTable(model){
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);int y=getRowCount()*getRowHeight();g.setColor(GRID);
            for(int line=y;line<getHeight();line+=getRowHeight())g.drawLine(0,line,getWidth(),line);
            int x=0;for(int c=0;c<getColumnCount();c++){x+=getColumnModel().getColumn(c).getWidth();g.drawLine(x-1,y,x-1,getHeight());}}
    };t.setRowHeight(24);t.setFont(UngDungPhongChu.TU_TRUC_DU_LIEU);t.setGridColor(GRID);t.setShowGrid(true);t.setIntercellSpacing(new Dimension(1,1));
        t.setFillsViewportHeight(true);t.setBackground(Color.WHITE);t.setSelectionBackground(SELECTED);t.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);return t;}
    public static String status(Phieu p){return switch(p.status()==null?"":p.status()){
        case "CHO_DUYET","TAM","REQUESTED"->"Chờ duyệt";case "PARTIALLY_APPROVED"->"Duyệt một phần";case "DA_DUYET","APPROVED"->"Đã duyệt";
        case "PARTIALLY_ISSUED"->"Xuất một phần";case "ISSUED","DA_XUAT"->"Nhập bù".equals(p.type())?"Đang chuyển":"Đã xuất kho";
        case "PARTIALLY_RECEIVED"->"Nhận một phần";case "RECEIVED","DA_NHAN_HANG","HOAN_THANH"->"Đã nhập tủ";
        case "DA_HUY","CANCELLED"->"Đã hủy";case "REVERSED"->"Đã đảo";case "TU_CHOI"->"Từ chối";default->p.status();};}
    private static String patient(String s){return switch(s==null?"":s){case "BHYT"->"BHYT";case "THU_PHI","VIEN_PHI"->"Viện phí";default->s;};}
    private static Icon dot(String status){return new Icon(){public int getIconWidth(){return 14;}public int getIconHeight(){return 14;}
        public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D copy=(Graphics2D)g.create();copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(switch(status==null?"":status){case "CHO_DUYET","TAM"->new Color(244,170,20);case "DA_HUY","TU_CHOI"->Color.GRAY;default->Color.RED;});copy.fillOval(x,y,14,14);copy.dispose();}};}
    private static JMenuItem item(JPopupMenu menu,String text,Runnable action){var item=new JMenuItem(text);item.setFont(UngDungPhongChu.BODY);item.addActionListener(e->action.run());menu.add(item);return item;}
    private static void popup(JButton button,JPopupMenu menu){button.addActionListener(e->{if(button.isShowing())menu.show(button,0,button.getHeight());});}
}
