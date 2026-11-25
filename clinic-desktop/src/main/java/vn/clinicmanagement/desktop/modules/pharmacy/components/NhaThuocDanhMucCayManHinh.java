package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Cột phân loại phiếu kho: ba nhóm độc lập theo ảnh tham chiếu. */
public class NhaThuocDanhMucCayManHinh extends JPanel {
    public static final String DEFAULT_WAREHOUSE_TITLE = "Kho Nhà Thuốc";
    private static final Color SELECTED = new Color(0, 142, 255);
    private static final Color LINE = new Color(119, 133, 187);
    private static final Font TEXT = UngDungPhongChu.BODY;
    public record CategoryFilter(Integer warehouseId, String ticketType) {
        public static final String ALL="ALL", NHAP_KHO="NHAP_KHO", XUAT_KHO="XUAT_KHO", DON_THUOC="DON_THUOC";
        public static final String XUAT_LE="XUAT_LE", XUAT_BENH_NHAN="XUAT_BENH_NHAN";
    }
    private Integer warehouseId;
    private int selectedIndex;
    private String selectedKey="ALL";
    private int allCount, importCount, exportCount, prescriptionCount;
    private Consumer<CategoryFilter> onCategorySelected;
    private Consumer<String> statusListener;
    private final JLabel lblTitle = new JLabel(DEFAULT_WAREHOUSE_TITLE);
    private final JLabel lblAll = new JLabel(), lblImport = new JLabel(), lblExport = new JLabel(), lblPrescription = new JLabel();
    private final List<BranchRow> rows = new ArrayList<>();
    private final Map<String, TreeGroup> groups = new LinkedHashMap<>();
    private final Map<String, JLabel> statusLabels = new LinkedHashMap<>();
    private final JLabel[] totals = {new JLabel("0đ"),new JLabel("0đ"),new JLabel("0đ"),new JLabel("—")};
    private final BranchRow rowAllTickets, rowImport, rowExport, rowPrescription;

    public NhaThuocDanhMucCayManHinh() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(318, 693));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1,1,0,1,new Color(192,192,192)));
        lblTitle.setFont(UngDungPhongChu.SECTION);lblTitle.setForeground(Color.RED);
        lblTitle.setBorder(new EmptyBorder(0,7,0,2));lblTitle.setPreferredSize(new Dimension(318,43));
        add(lblTitle,BorderLayout.NORTH);
        JPanel tree = new JPanel();tree.setLayout(new BoxLayout(tree,BoxLayout.Y_AXIS));tree.setBackground(Color.WHITE);
        rowAllTickets = new BranchRow("ALL",lblAll,false,true,()->selectRow(0));
        tree.add(rowAllTickets);
        TreeGroup types = group(tree,"Loại Phiếu");
        rowImport = new BranchRow("NHAP_KHO",lblImport,false,false,()->selectRow(1));
        rowExport = new BranchRow("XUAT_LE",lblExport,false,false,()->selectRow(2));
        rowPrescription = new BranchRow("XUAT_BENH_NHAN",lblPrescription,true,false,()->selectRow(3));
        types.children.add(rowImport);types.children.add(rowExport);types.children.add(rowPrescription);
        TreeGroup status = group(tree,"Trạng thái");
        addStatus(status,"ACTIVE","Đang sử dụng",false);
        addStatus(status,"CANCELLED","Đã hủy",false);
        addStatus(status,"UNSENT","Chưa gửi liên thông",true);
        TreeGroup state = group(tree,"Trạng thái phiếu");
        addStatus(state,"EXPORTED","Đã xuất",false);addStatus(state,"PENDING","Chờ duyệt",true);
        JPanel top = new JPanel(new BorderLayout());top.setBackground(Color.WHITE);top.add(tree,BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top);scroll.setBorder(null);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(32);add(scroll,BorderLayout.CENTER);
        JPanel summary = new JPanel(new GridLayout(4,1));summary.setBackground(Color.WHITE);summary.setPreferredSize(new Dimension(318,134));
        String[] names={"Tổng tiền nhập NCC:","Tổng tiền trả NCC:","Tổng tiền xuất lẻ:","Tổng tiền nhập lẻ:"};
        for(int i=0;i<names.length;i++){
            JPanel line = new JPanel(new BorderLayout());line.setBackground(Color.WHITE);line.setBorder(new EmptyBorder(3,8,3,8));
            line.add(new JLabel(names[i]),BorderLayout.WEST);totals[i].setForeground(i==0||i==3?Color.RED:Color.BLUE);line.add(totals[i],BorderLayout.EAST);summary.add(line);
        }
        add(summary,BorderLayout.SOUTH);
        updateTicketCounts(0,0,0,0);updateStatusCounts(0,0,0,0);selectAppearance("ALL");
    }
    private TreeGroup group(JPanel tree,String name){TreeGroup group=new TreeGroup(name);groups.put(name,group);tree.add(group);return group;}
    private void addStatus(TreeGroup group,String key,String title,boolean last){
        JLabel label=new JLabel(title+" (0)");statusLabels.put(key,label);
        BranchRow row=new BranchRow(key,label,last,false,()->{
            selectedIndex=-1;selectAppearance(key);
            if(onCategorySelected!=null)onCategorySelected.accept(new CategoryFilter(warehouseId,CategoryFilter.ALL));
            if(statusListener!=null)statusListener.accept(key);
        });
        if("UNSENT".equals(key)){
            row.setEnabled(false);row.setToolTipText("Chưa có dữ liệu trạng thái liên thông trong hệ thống.");
            label.setToolTipText(row.getToolTipText());
        }
        group.children.add(row);
    }
    private void selectAppearance(String key){
        selectedKey=key;
        for(BranchRow row:rows){row.label.setForeground(row.key.equals(key)?Color.WHITE:Color.BLACK);row.repaint();}
    }
    private final class BranchRow extends JPanel {
        final String key;final JLabel label;final boolean last,root;
        BranchRow(String key,JLabel label,boolean last,boolean root,Runnable clicked){
            super(new BorderLayout());this.key=key;this.label=label;this.last=last;this.root=root;rows.add(this);
            setOpaque(false);setAlignmentX(LEFT_ALIGNMENT);
            int height=root?34:32;setPreferredSize(new Dimension(318,height));setMinimumSize(new Dimension(0,height));setMaximumSize(new Dimension(Integer.MAX_VALUE,height));
            label.setFont(root?TEXT.deriveFont(Font.BOLD):TEXT);label.setBorder(new EmptyBorder(0,root?32:64,0,2));add(label,BorderLayout.CENTER);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter mouse=new MouseAdapter(){@Override public void mousePressed(MouseEvent e){if(isEnabled())clicked.run();}};
            addMouseListener(mouse);label.addMouseListener(mouse);
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"),"select");
            getActionMap().put("select",new AbstractAction(){public void actionPerformed(ActionEvent e){if(isEnabled())clicked.run();}});
        }
        @Override protected void paintComponent(Graphics g){
            g.setColor(key.equals(selectedKey)?SELECTED:Color.WHITE);g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(LINE);
            if(root){g.drawLine(18,getHeight()/2,28,getHeight()/2);g.drawLine(18,getHeight()/2,18,getHeight());}
            else{g.drawLine(18,0,18,getHeight());g.drawLine(44,0,44,last?getHeight()/2:getHeight());g.drawLine(44,getHeight()/2,58,getHeight()/2);}
        }
    }
    private final class TreeGroup extends JPanel {
        final JPanel children=new JPanel();final JButton heading;
        boolean expanded=true;
        TreeGroup(String name){
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setOpaque(false);setAlignmentX(LEFT_ALIGNMENT);
            children.setLayout(new BoxLayout(children,BoxLayout.Y_AXIS));children.setOpaque(false);children.setAlignmentX(LEFT_ALIGNMENT);
            heading=new JButton(name){
                @Override protected void paintComponent(Graphics g){
                    g.setColor(Color.WHITE);g.fillRect(0,0,getWidth(),getHeight());g.setColor(LINE);g.drawLine(18,0,18,getHeight());
                    super.paintComponent(g);
                    g.setColor(new Color(145,145,145));g.setColor(Color.WHITE);g.fillRect(11,9,14,14);g.setColor(Color.GRAY);g.drawRect(11,9,13,13);
                    g.setColor(new Color(70,70,70));g.drawLine(14,15,21,15);if(!expanded)g.drawLine(18,12,18,19);
                }
            };
            heading.setFont(TEXT.deriveFont(Font.BOLD));heading.setForeground(Color.BLACK);heading.setHorizontalAlignment(SwingConstants.LEFT);
            heading.setBorder(new EmptyBorder(0,36,0,0));heading.setFocusPainted(false);heading.setContentAreaFilled(false);
            heading.setAlignmentX(LEFT_ALIGNMENT);heading.setPreferredSize(new Dimension(318,32));heading.setMinimumSize(new Dimension(0,32));heading.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));
            heading.addActionListener(e->{expanded=!expanded;children.setVisible(expanded);heading.repaint();revalidate();repaint();});
            add(heading);add(children);
        }
        @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
    }
    public JButton getGroupToggle(String name){return groups.containsKey(name)?groups.get(name).heading:null;}
    public boolean isGroupExpanded(String name){return groups.containsKey(name)&&groups.get(name).expanded;}
    public void setOnStatusSelected(Consumer<String> listener){statusListener=listener;}
    public void updateStatusCounts(int active,int cancelled,int exported,int pending){updateStatusCounts(active,cancelled,null,exported,pending);}
    public void updateStatusCounts(int active,int cancelled,Integer unsent,int exported,int pending){
        String[] keys={"ACTIVE","CANCELLED","UNSENT","EXPORTED","PENDING"};
        Integer[] counts={active,cancelled,unsent,exported,pending};String[] names={"Đang sử dụng","Đã hủy","Chưa gửi liên thông","Đã xuất","Chờ duyệt"};
        for(int i=0;i<keys.length;i++)statusLabels.get(keys[i]).setText(names[i]+" ("+(counts[i]==null?"—":counts[i])+")");
        for(BranchRow row:rows)if("UNSENT".equals(row.key))row.setEnabled(unsent!=null);
    }
    public void updateTicketCounts(int all,int imports,int retail,int patient){
        allCount=all;importCount=imports;exportCount=retail;prescriptionCount=patient;
        lblAll.setText("Tất Cả Phiếu ("+all+")");lblImport.setText("Nhập nhà cung cấp ("+imports+")");
        lblExport.setText("Xuất lẻ ("+retail+")");lblPrescription.setText("Xuất bệnh nhân ("+patient+")");
    }
    public void updateMoney(BigDecimal imports,BigDecimal returns,BigDecimal retail,BigDecimal retailReturns){
        BigDecimal[] values={imports,returns,retail,retailReturns};
        for(int i=0;i<4;i++)totals[i].setText(i==3?"—":new java.text.DecimalFormat("#,##0.##'đ'").format(values[i]));
    }
    public void selectRow(int index){
        if(index<0||index>3)return;selectedIndex=index;
        selectAppearance(getTicketTypeByIndex(index));
        if(onCategorySelected!=null)onCategorySelected.accept(getSelectedFilter());
    }
    public void selectCategory(String type){
        if(type==null)return;
        switch(type.toUpperCase(Locale.ROOT)){
            case "ALL","TAT_CA"->selectRow(0);case "NHAP_KHO"->selectRow(1);
            case "XUAT_KHO","XUAT_LE"->selectRow(2);case "DON_THUOC","XUAT_BENH_NHAN"->selectRow(3);default->{}
        }
    }
    public String getTicketTypeByIndex(int index){return switch(index){case 1->CategoryFilter.NHAP_KHO;case 2->CategoryFilter.XUAT_LE;case 3->CategoryFilter.XUAT_BENH_NHAN;default->CategoryFilter.ALL;};}
    public CategoryFilter getSelectedFilter(){return new CategoryFilter(warehouseId,getTicketTypeByIndex(selectedIndex));}
    public int getSelectedIndex(){return selectedIndex;}
    public void setWarehouseTitle(String title){lblTitle.setText(title==null||title.isBlank()?DEFAULT_WAREHOUSE_TITLE:title);lblTitle.setToolTipText(lblTitle.getText());}
    public String getWarehouseTitle(){return lblTitle.getText();}
    public void setWarehouseId(Integer id){warehouseId=id;}
    public Integer getWarehouseId(){return warehouseId;}
    public void setWarehouse(Integer id,String title){setWarehouseId(id);setWarehouseTitle(title);}
    public void setOnCategorySelected(Consumer<CategoryFilter> listener){onCategorySelected=listener;}
    public Consumer<CategoryFilter> getOnCategorySelected(){return onCategorySelected;}
    public JLabel getLblTitle(){return lblTitle;}public JLabel getLblAll(){return lblAll;}public JLabel getLblImport(){return lblImport;}
    public JLabel getLblExport(){return lblExport;}public JLabel getLblPrescription(){return lblPrescription;}
    public JPanel getRowAllTickets(){return rowAllTickets;}public JPanel getRowImport(){return rowImport;}public JPanel getRowImportReplenish(){return rowImport;}
    public JPanel getRowExport(){return rowExport;}public JPanel getRowPrescription(){return rowPrescription;}
    public int getAllCount(){return allCount;}public int getImportCount(){return importCount;}public int getExportCount(){return exportCount;}public int getPrescriptionCount(){return prescriptionCount;}
}
