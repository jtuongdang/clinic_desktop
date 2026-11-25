package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.application.catalog.*;
import vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Material consumption selector, reference 1586x959 (native title bar excluded). */
public final class VatTuHaoPhiManHinh extends JPanel {
    private static final Font MATERIAL_FONT=UngDungPhongChu.DICH_VU_CAU_HINH.deriveFont(java.awt.geom.AffineTransform.getScaleInstance(.90,1));
    private static final Icon STAR=new Icon(){public int getIconWidth(){return 22;}public int getIconHeight(){return 22;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Polygon shape=new Polygon();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;double r=i%2==0?10:4;shape.addPoint(x+11+(int)Math.round(Math.cos(a)*r),y+11+(int)Math.round(Math.sin(a)*r));}
            g.setColor(new Color(0,91,174));g.fill(shape);g.dispose();}};
    private record Warehouse(int id,String name){@Override public String toString(){return name;}}
    private record Group(String name,int count){@Override public String toString(){return name+" ("+count+")";}}
    private final DichVuDanhMucNguon source;
    private final Consumer<VatTuHaoPhiCauHinh> accepted;
    private final boolean legacyUnparsed;
    private final Map<String,Line> selection=new LinkedHashMap<>();
    private final Map<Stage,JRadioButton> stages=new EnumMap<>(Stage.class);
    final JComboBox<Warehouse> warehouses=new JComboBox<>();
    final JButton all=new JButton("− Tất Cả Vật Tư (0)");
    final JTree tree=new JTree(new DefaultMutableTreeNode("root"));
    final JTextField name=new JTextField(),packing=new JTextField(),unit=new JTextField(),quantity=new JTextField("1"),warehouseName=new JTextField();
    final JButton add=new JButton("✓ OK (Ctrl+A)"),save=new JButton("Lưu",DichVuCauHinhManHinh.saveIcon());
    final JCheckBox rounding=new JCheckBox("Làm tròn lên định mức gợi ý");
    final JLabel status=new JLabel("Đang tải danh mục vật tư…");
    final DefaultTableModel model=new DefaultTableModel(new String[]{"Mã Vật Tư","Tên Vật Tư","Đơn Vị","Quy Cách","Giá BHYT","Giá Viện Phí","Giá Dịch Vụ",""},0){public boolean isCellEditable(int r,int c){return false;}};
    final JTable table=new JTable(model){
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);g.setColor(new Color(199,207,214));int start=getRowCount()*getRowHeight();
            for(int y=start;y<getHeight();y+=getRowHeight())g.drawLine(0,y,getWidth(),y);int x=0;for(int i=0;i<getColumnCount();i++){x+=getColumnModel().getColumn(i).getWidth();g.drawLine(x-1,start,x-1,getHeight());}}
    };
    final DefaultListModel<Line> chosenModel=new DefaultListModel<>();
    final JList<Line> chosen=new JList<>(chosenModel);
    final JSplitPane columns;
    private final Editor editor=new Editor();
    private List<VatTuHaoPhi> catalog=List.of(),displayed=List.of();
    private String category="",baseline;
    private boolean changing,disposed,loading=true,readable=true;
    private VatTuHaoPhi selected;

    public VatTuHaoPhiManHinh(DichVuDanhMucNguon source,String definition,String legacy,Consumer<VatTuHaoPhiCauHinh> accepted){
        super(new BorderLayout());this.source=source;this.accepted=accepted;
        legacyUnparsed=(definition==null||definition.isBlank())&&legacy!=null&&!legacy.isBlank();setBackground(Color.WHITE);
        JLabel tab=new JLabel("Danh Sách Vật Tư"){
            @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();
                var shape=new Polygon(new int[]{0,28,getWidth()-1,getWidth()-1},new int[]{getHeight()-1,0,0,getHeight()-1},4);
                g.setPaint(new GradientPaint(0,0,Color.WHITE,0,getHeight(),new Color(219,235,247)));g.fill(shape);g.setColor(new Color(99,140,172));g.draw(shape);g.dispose();super.paintComponent(graphics);}
        };tab.setFont(UngDungPhongChu.DICH_VU_CAU_HINH_BOLD);tab.setBorder(new EmptyBorder(3,34,3,14));tab.setPreferredSize(new Dimension(203,38));
        JPanel tabBar=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));tabBar.setPreferredSize(new Dimension(100,40));tabBar.setBackground(new Color(249,249,249));tabBar.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(68,112,157)));tabBar.add(tab);add(tabBar,BorderLayout.NORTH);
        JPanel left=new JPanel(new BorderLayout());left.setMinimumSize(new Dimension(190,0));left.setBackground(Color.WHITE);
        warehouses.setFont(MATERIAL_FONT);warehouses.setForeground(new Color(145,18,0));warehouses.setPreferredSize(new Dimension(320,38));left.add(warehouses,BorderLayout.NORTH);
        JPanel categories=new JPanel(new BorderLayout());all.setFont(UngDungPhongChu.DICH_VU_CAU_HINH_BOLD);all.setBackground(new Color(0,145,255));all.setForeground(Color.WHITE);
        all.setHorizontalAlignment(SwingConstants.LEFT);all.setBorder(new EmptyBorder(1,10,1,0));all.setPreferredSize(new Dimension(320,30));all.addActionListener(e->{category="";tree.clearSelection();filter();});categories.add(all,BorderLayout.NORTH);
        tree.setRootVisible(false);tree.setShowsRootHandles(true);tree.setRowHeight(28);tree.setFont(MATERIAL_FONT);tree.setBackground(Color.WHITE);
        tree.setCellRenderer(new DefaultTreeCellRenderer(){public Component getTreeCellRendererComponent(JTree t,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean focus){
            super.getTreeCellRendererComponent(t,value,selected,expanded,leaf,row,focus);setLeafIcon(null);setOpenIcon(null);setClosedIcon(null);setFont(row==0?UngDungPhongChu.DICH_VU_CAU_HINH_BOLD:UngDungPhongChu.DICH_VU_CAU_HINH);return this;}});
        tree.addTreeSelectionListener(e->{if(changing)return;var node=(DefaultMutableTreeNode)tree.getLastSelectedPathComponent();category=node!=null&&node.getUserObject() instanceof Group group?group.name():"";filter();});
        JScrollPane treeScroll=new JScrollPane(tree);treeScroll.setBorder(null);categories.add(treeScroll);left.add(categories);
        buildEditor();JPanel right=new JPanel(new BorderLayout());right.add(editor,BorderLayout.NORTH);configureTable();
        JScrollPane tableScroll=new JScrollPane(table);tableScroll.setColumnHeaderView(table.getTableHeader());tableScroll.setBorder(null);
        chosen.setFont(MATERIAL_FONT);chosen.setCellRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(list,value,index,selected,focus);
                if(value instanceof Line l){setIcon(STAR);setText(l.name()+" ("+l.quantity().stripTrailingZeros().toPlainString()+")");setToolTipText(l.warehouseName());}return this;}});
        var selectedScroll=new JScrollPane(chosen);selectedScroll.setPreferredSize(new Dimension(100,130));selectedScroll.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.GRAY));
        JPanel grid=new JPanel(new BorderLayout());grid.add(tableScroll);grid.add(selectedScroll,BorderLayout.SOUTH);right.add(grid);
        columns=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);columns.setBorder(null);columns.setDividerSize(3);columns.setDividerLocation(320);columns.setResizeWeight(.202);add(columns);
        JPanel footer=new JPanel(new BorderLayout()){
            @Override public Dimension getPreferredSize(){return new Dimension(100,getWidth()>0&&getWidth()<1360?100:69);}
        };footer.setBackground(new Color(249,249,249));footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.GRAY));
        JPanel options=new JPanel(new FlowLayout(FlowLayout.LEFT,18,16));options.setOpaque(false);ButtonGroup timing=new ButtonGroup();
        for(Stage stage:List.of(Stage.RESULT)){var button=new JRadioButton(stage.title());button.setOpaque(false);button.setFont(UngDungPhongChu.DICH_VU_CAU_HINH.deriveFont(java.awt.geom.AffineTransform.getScaleInstance(.90,1)));timing.add(button);stages.put(stage,button);options.add(button);}
        rounding.setToolTipText("Làm tròn lên đơn vị nguyên cho lượng gợi ý; vẫn được sửa lượng thực dùng trước khi xác nhận.");rounding.setOpaque(false);rounding.setFont(MATERIAL_FONT);options.add(rounding);footer.add(options);
        save.setFont(MATERIAL_FONT);save.setContentAreaFilled(false);save.setBorder(new EmptyBorder(3,8,3,20));save.setPreferredSize(new Dimension(105,65));save.setEnabled(false);save.addActionListener(e->accept());footer.add(save,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);
        status.setFont(UngDungPhongChu.SMALL);status.setForeground(new Color(160,30,20));right.add(status,BorderLayout.SOUTH);
        status.addPropertyChangeListener("text",e->status.setVisible(status.getText()!=null&&!status.getText().isBlank()));
        warehouses.addActionListener(e->{if(!changing){category="";name.setText("");filter();}});
        name.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){search();}public void removeUpdate(javax.swing.event.DocumentEvent e){search();}public void changedUpdate(javax.swing.event.DocumentEvent e){search();}private void search(){if(!changing)filter();}});
        add.addActionListener(e->addSelected());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control A"),"addMaterial");getActionMap().put("addMaterial",new AbstractAction(){public void actionPerformed(ActionEvent e){addSelected();}});
        for(JTextField field:List.of(name,quantity)){field.getInputMap().put(KeyStroke.getKeyStroke("control A"),"addMaterial");field.getActionMap().put("addMaterial",getActionMap().get("addMaterial"));}
        chosen.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),"remove");chosen.getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"),"remove");
        chosen.getActionMap().put("remove",new AbstractAction(){public void actionPerformed(ActionEvent e){var line=chosen.getSelectedValue();if(line!=null){selection.remove(key(line.itemId(),line.warehouseId()));rebuildChosen();}}});
        JPopupMenu remove=new JPopupMenu();JMenuItem delete=new JMenuItem("Xóa vật tư khỏi danh sách");delete.addActionListener(e->chosen.getActionMap().get("remove").actionPerformed(e));remove.add(delete);chosen.setComponentPopupMenu(remove);
        try{var initial=VatTuHaoPhiCauHinh.decode(definition);initial.lines().forEach(l->selection.put(key(l.itemId(),l.warehouseId()),l));rounding.setSelected(initial.rounding());if(stages.containsKey(initial.stage()))stages.get(initial.stage()).setSelected(true);
            else if(!initial.lines().isEmpty())status.setText("Thời điểm cũ chưa được hỗ trợ. Chọn kê khi trả kết quả để lưu cấu hình mới.");
            rebuildChosen();baseline=current().encode();}
        catch(IllegalArgumentException ex){readable=false;status.setText(ex.getMessage());baseline="";}
        if((definition==null||definition.isBlank())&&legacy!=null&&!legacy.isBlank())status.setText("Cấu hình cũ được giữ nguyên ở dịch vụ. Chọn vật tư để lập lại danh sách hao phí.");
        load();
    }

    private int layoutWidth;
    @Override public void doLayout(){if(columns!=null&&layoutWidth!=getWidth()){layoutWidth=getWidth();columns.setDividerLocation(Math.max(200,(int)Math.round(getWidth()*.202)));}super.doLayout();}

    private void buildEditor(){
        editor.place(new JLabel("Tên Vật Tư:"),10,7,96,30);editor.place(name,109,4,384,31);JButton choose=new JButton("…");editor.place(choose,493,4,24,31);
        choose.addActionListener(e->{if(displayed.isEmpty())return;JComboBox<VatTuHaoPhi> options=new JComboBox<>(displayed.toArray(VatTuHaoPhi[]::new));
            if(JOptionPane.showConfirmDialog(this,options,"Chọn vật tư",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)select((VatTuHaoPhi)options.getSelectedItem());});
        editor.place(new JLabel("Đóng gói:"),529,7,91,30);editor.place(packing,624,4,167,31);editor.place(new JLabel("Đơn vị tính:"),803,7,95,30);editor.place(unit,902,4,148,31);
        editor.place(new JLabel("Số lượng:"),10,45,96,30);editor.place(quantity,109,41,150,31);quantity.setHorizontalAlignment(SwingConstants.CENTER);
        editor.place(new JLabel("Kho:"),272,45,90,30);editor.place(warehouseName,368,41,660,31);JButton chooseWarehouse=new JButton("…");editor.place(chooseWarehouse,1028,41,24,31);
        chooseWarehouse.addActionListener(e->{if(warehouses.getItemCount()==0)return;JComboBox<Warehouse> options=new JComboBox<>();for(int i=0;i<warehouses.getItemCount();i++)options.addItem(warehouses.getItemAt(i));options.setSelectedItem(warehouses.getSelectedItem());
            if(JOptionPane.showConfirmDialog(this,options,"Chọn kho",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)warehouses.setSelectedItem(options.getSelectedItem());});
        editor.place(add,1064,37,163,39);add.setBackground(new Color(255,221,105));add.setForeground(new Color(0,70,35));add.setFocusPainted(false);
        for(var field:List.of(packing,unit,warehouseName)){field.setEditable(false);field.setBackground(Color.WHITE);}
    }
    private void configureTable(){
        table.setRowHeight(28);table.setFont(MATERIAL_FONT);table.setFillsViewportHeight(true);table.setBackground(Color.WHITE);table.setGridColor(new Color(199,207,214));
        table.setCellSelectionEnabled(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setFont(MATERIAL_FONT);table.getTableHeader().setPreferredSize(new Dimension(100,36));
        int[] widths={110,390,83,166,120,120,126,148};for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
            super.getTableCellRendererComponent(t,value,selected,focus,row,column);setBorder(new EmptyBorder(1,6,1,6));setBackground(selected?new Color(215,239,251):row%2==0?new Color(247,247,247):Color.WHITE);setForeground(Color.BLACK);setHorizontalAlignment(column>=4&&column<=6?RIGHT:column==2?CENTER:LEFT);return this;}});
        table.getSelectionModel().addListSelectionListener(e->{int row=table.getSelectedRow();if(!e.getValueIsAdjusting()&&!changing&&row>=0&&row<displayed.size())select(displayed.get(row));});
        table.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e))addSelected();}});
    }
    private void load(){
        add.setEnabled(false);new SwingWorker<List<VatTuHaoPhi>,Void>(){
            protected List<VatTuHaoPhi> doInBackground(){return source.loadMaterials();}
            protected void done(){if(disposed)return;try{catalog=List.copyOf(get());changing=true;
                Map<Integer,Warehouse> choices=new LinkedHashMap<>();catalog.forEach(m->choices.putIfAbsent(m.warehouseId(),new Warehouse(m.warehouseId(),m.warehouseName())));choices.values().forEach(warehouses::addItem);
                if(!selection.isEmpty()){int first=selection.values().iterator().next().warehouseId();choices.values().stream().filter(w->w.id()==first).findFirst().ifPresent(warehouses::setSelectedItem);}
                changing=false;loading=false;filter();
                if(!selection.isEmpty()){
                    var first=selection.values().iterator().next();
                    displayed.stream().filter(m->m.id()==first.itemId()&&m.warehouseId()==first.warehouseId()).findFirst().ifPresent(m->{
                        changing=true;table.changeSelection(displayed.indexOf(m),6,false,false);changing=false;selected=m;
                    });
                }
                if(selected!=null)select(selected);save.setEnabled(readable);if(catalog.isEmpty())status.setText("Cơ sở hiện tại chưa có danh mục vật tư trong kho.");else if(status.getText().startsWith("Đang tải"))status.setText(" ");
            }catch(Exception ex){loading=false;status.setText("Không tải được danh mục vật tư. Đóng và mở lại để thử lại.");}}
        }.execute();
    }
    private void filter(){
        var warehouse=(Warehouse)warehouses.getSelectedItem();String query=normalize(name.getText());
        var available=catalog.stream().filter(m->warehouse!=null&&m.warehouseId()==warehouse.id()).toList();
        displayed=available.stream().filter(m->category.isBlank()||category.equals(m.group())).filter(m->normalize(m.code()+" "+m.name()).contains(query)).toList();
        changing=true;model.setRowCount(0);for(var m:displayed)model.addRow(new Object[]{m.code(),m.name(),m.unit(),m.packing(),price(m.insurancePrice()),price(m.hospitalPrice()),price(m.servicePrice()),""});
        var root=new DefaultMutableTreeNode("root");var group=new DefaultMutableTreeNode("Vật tư ("+available.size()+")");root.add(group);
        Map<String,Integer> groups=new LinkedHashMap<>();available.forEach(m->groups.merge(m.group(),1,Integer::sum));groups.forEach((g,n)->group.add(new DefaultMutableTreeNode(new Group(g,n))));
        tree.setModel(new DefaultTreeModel(root));tree.expandRow(0);all.setText("− Tất Cả Vật Tư ("+available.size()+")");warehouseName.setText(warehouse==null?"":warehouse.name());
        selected=displayed.isEmpty()?null:displayed.get(0);add.setEnabled(!loading&&readable&&selected!=null);if(!displayed.isEmpty())table.changeSelection(0,6,false,false);
        packing.setText(selected==null?"":selected.packing());unit.setText(selected==null?"":selected.unit());changing=false;
    }
    private void select(VatTuHaoPhi material){if(material==null)return;selected=material;changing=true;var existing=selection.get(key(material.id(),material.warehouseId()));quantity.setText(existing==null?"1":existing.quantity().stripTrailingZeros().toPlainString());name.setText(material.name());packing.setText(material.packing());unit.setText(material.unit());changing=false;add.setEnabled(readable&&!loading);}
    void addSelected(){
        if(loading||!readable||selected==null)return;
        try{var n=DichVuCauHinh.number(quantity.getText());if(n==null||n.signum()<=0)throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
            var material=selected;Line line=new Line(material.id(),material.warehouseId(),material.code(),material.name(),material.unit(),material.packing(),material.warehouseName(),n);
            if(selection.size()>=100&&!selection.containsKey(key(line.itemId(),line.warehouseId())))throw new IllegalArgumentException("Tối đa 100 vật tư.");
            selection.put(key(line.itemId(),line.warehouseId()),line);rebuildChosen();status.setText("Đã chọn "+selection.size()+" vật tư. Chọn thời điểm kê và nhấn Lưu.");
        }catch(IllegalArgumentException ex){status.setText(ex.getMessage());}
    }
    private void rebuildChosen(){chosenModel.clear();selection.values().forEach(chosenModel::addElement);}
    VatTuHaoPhiCauHinh current(){return new VatTuHaoPhiCauHinh(stages.entrySet().stream().filter(e->e.getValue().isSelected()).map(Map.Entry::getKey).findFirst().orElse(Stage.NONE),rounding.isSelected(),List.copyOf(selection.values()));}
    void accept(){if(loading||!readable)return;try{var value=current();
        if(legacyUnparsed&&value.lines().isEmpty()){status.setText("Cấu hình cũ được giữ nguyên. Chọn vật tư và thời điểm kê để thay đổi.");return;}
        value.validate();baseline=value.encode();accepted.accept(value);}catch(IllegalArgumentException ex){status.setText(ex.getMessage());}}
    public boolean canClose(){return !readable||baseline==null||baseline.equals(current().encode())||(!GraphicsEnvironment.isHeadless()&&JOptionPane.showConfirmDialog(this,"Đóng và bỏ lựa chọn vật tư chưa lưu?","Vật Tư Hao Phí",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);}
    public void dispose(){disposed=true;}
    private static String key(int item,int warehouse){return item+":"+warehouse;}
    private static String normalize(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).trim();}
    private static String price(BigDecimal value){return value==null?"":java.text.NumberFormat.getNumberInstance(Locale.US).format(value);}
    private static final class Editor extends JPanel {
        final Map<JComponent,Rectangle> positions=new LinkedHashMap<>();Editor(){super(null);setBackground(new Color(249,249,249));setPreferredSize(new Dimension(100,83));}
        void place(JComponent c,int x,int y,int w,int h){positions.put(c,new Rectangle(x,y,w,h));c.setFont(MATERIAL_FONT);add(c);}
        @Override public void doLayout(){double scale=Math.max(.65,getWidth()/1263.0);positions.forEach((c,r)->{c.setBounds((int)(r.x*scale),r.y,(int)(r.width*scale),r.height);c.setFont(MATERIAL_FONT.deriveFont((float)Math.max(12,17*Math.min(1,scale))));});}
    }
}
