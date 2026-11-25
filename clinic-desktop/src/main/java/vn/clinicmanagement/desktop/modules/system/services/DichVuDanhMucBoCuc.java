package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import static vn.clinicmanagement.desktop.modules.system.services.DichVuBieuTuong.Kind.*;

/** Service-list reference content: 1586 x 960, toolbar 80, tree 312, separator 3. */
public final class DichVuDanhMucBoCuc {
    public final JButton menu=tool("",MENU),price=tool("Cập nhật Giá",PRICE),refresh=tool("Làm Mới",REFRESH),add=tool("Thêm",ADD),
            mapService=tool("Ánh Xạ DVKT",MAP),mapExam=tool("Ánh Xạ Công Khám",MAP),mapBed=tool("Ánh Xạ Ngày Giường",MAP),
            up=tool("Lên",UP),down=tool("Xuống",DOWN),save=tool("Lưu Thứ Tự",SAVE),search=inline("",SEARCH);
    public final JTextField keyword=new JTextField();
    public final JTree tree=new JTree(new DefaultMutableTreeNode("Tất Cả Dịch Vụ (0)")) {
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
            g.setColor(new Color(135,135,135));g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{1,2},0));
            Rectangle visible=getVisibleRect();
            for(int row=1;row<getRowCount();row++){
                Rectangle r=getRowBounds(row);if(!r.intersects(visible))continue;
                TreePath path=getPathForRow(row);
                if(path.getPathCount()==3){
                    int x=r.x-px(12),center=r.y+r.height/2;
                    var node=(DefaultMutableTreeNode)path.getLastPathComponent();
                    g.drawLine(x,r.y,x,node.getNextSibling()==null?center:r.y+r.height);g.drawLine(x,center,r.x-px(3),center);
                }
            }
            if(isRowSelected(0)&&getRowCount()>0){
                Rectangle r=getRowBounds(0);g.setColor(new Color(0,143,250));g.fillRect(0,r.y,getWidth(),r.height);
                g.setFont(UngDungPhongChu.DICH_VU_DANH_MUC_BOLD.deriveFont((float)(13*scale)));g.setColor(Color.WHITE);
                g.drawString(getModel().getRoot().toString(),px(26),r.y+(r.height-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent());
                g.setStroke(new BasicStroke(1));g.drawLine(px(10),r.y+r.height/2,px(20),r.y+r.height/2);
            }g.dispose();
        }
    };
    public final JTable table;
    public final JSplitPane split;
    public final JLabel status=new JLabel(" ");
    public final JPanel footer=new JPanel(new BorderLayout());
    public final JPanel toolbar;
    public final JPanel searchBar=new JPanel(new BorderLayout());
    private final JScrollPane treeScroll,tableScroll;
    private final DichVuDanhMucMoHinh model;
    private double scale=1;
    public DichVuDanhMucBoCuc(JPanel host,DichVuDanhMucMoHinh model){
        this.model=model;host.setLayout(new BorderLayout());host.setBackground(Color.WHITE);host.setPreferredSize(new Dimension(1586,960));
        toolbar=new JPanel(null){
            @Override public void doLayout(){
                place(menu,3,6,65,68);place(price,75,4,105,72);place(refresh,194,4,82,72);place(add,292,4,70,72);
                place(mapService,383,4,112,72);place(mapExam,502,4,153,72);place(mapBed,662,4,170,72);
                place(up,1251,4,90,72);place(down,1347,4,90,72);place(save,1452,4,129,72);
            }
            private void place(Component c,int x,int y,int w,int h){c.setBounds(px(x),px(y),px(w),px(h));}
        };
        toolbar.setBackground(new Color(250,250,250));toolbar.setBorder(new MatteBorder(0,0,1,0,new Color(170,170,170)));
        for(JButton button:List.of(menu,price,refresh,add,mapService,mapExam,mapBed,up,down,save))toolbar.add(button);
        host.add(toolbar,BorderLayout.NORTH);
        for(JButton button:List.of(price,add,mapService,mapExam,mapBed)) {
            button.setEnabled(false);button.setToolTipText("Chưa kết nối luồng cập nhật danh mục và ánh xạ trên màn hình này.");
        }
        tree.setRootVisible(true);tree.setShowsRootHandles(true);tree.setBorder(new EmptyBorder(0,1,0,0));
        tree.putClientProperty("JTree.lineStyle","Angled");
        tree.setUI(new BasicTreeUI(){
            @Override protected int getRowX(int row,int depth){return px(depth*24);}
            @Override protected void paintExpandControl(Graphics g,Rectangle clip,Insets insets,Rectangle bounds,TreePath path,int row,boolean expanded,boolean visited,boolean leaf){
                if(leaf)return;
                int size=Math.max(8,px(10)),x=bounds.x-px(15),y=bounds.y+(bounds.height-size)/2;
                g.setColor(Color.WHITE);g.fillRect(x,y,size,size);g.setColor(new Color(100,100,100));g.drawRect(x,y,size,size);
                g.drawLine(x+2,y+size/2,x+size-2,y+size/2);if(!expanded)g.drawLine(x+size/2,y+2,x+size/2,y+size-2);
            }
        });
        tree.setCellRenderer(new DefaultTreeCellRenderer(){
            @Override public Component getTreeCellRendererComponent(JTree t,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean focus){
                super.getTreeCellRendererComponent(t,value,selected,expanded,leaf,row,false);
                setLeafIcon(null);setOpenIcon(null);setClosedIcon(null);setIcon(null);
                setFont(UngDungPhongChu.DICH_VU_DANH_MUC_BOLD.deriveFont((float)(13*scale)));
                setBackgroundSelectionColor(new Color(0,143,250));setTextSelectionColor(Color.WHITE);setBorderSelectionColor(new Color(0,143,250));return this;
            }
        });
        treeScroll=scroll(tree);treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        table=new JTable(model){
            @Override protected void paintComponent(Graphics graphics){
                super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
                Rectangle clip=g.getClipBounds();
                for(int row=0;row<model.getRowCount();row++){
                    var item=model.row(row);if(item.kind()==DichVuDanhMucMoHinh.Kind.ITEM)continue;
                    Rectangle r=getCellRect(row,0,true);if(clip!=null&&!r.intersects(clip))continue;
                    g.setColor(isRowSelected(row)?getSelectionBackground():row%2==0?Color.WHITE:new Color(246,246,247));g.fillRect(0,r.y,getWidth(),r.height);
                    g.setColor(isRowSelected(row)?Color.WHITE:Color.BLACK);g.setFont(UngDungPhongChu.DICH_VU_DANH_MUC_BOLD.deriveFont((float)(13*scale)));
                    g.drawString(item.title(),px(28+item.depth()*24),r.y+(r.height-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent());
                    int x=px(9+item.depth()*24),center=r.y+r.height/2,size=px(10);
                    g.setStroke(new BasicStroke(1));g.setColor(Color.WHITE);g.fillRect(x,center-size/2,size,size);
                    g.setColor(new Color(100,110,125));g.drawRect(x,center-size/2,size,size);g.drawLine(x+2,center,x+size-2,center);
                    if(!model.expanded(item))g.drawLine(x+size/2,center-size/2+2,x+size/2,center+size/2-2);
                    g.setColor(new Color(185,187,193));g.drawLine(0,r.y+r.height-1,getWidth(),r.y+r.height-1);
                }g.dispose();
            }
        };table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);table.setGridColor(new Color(185,187,193));table.setIntercellSpacing(new Dimension(1,1));
        table.setSelectionBackground(new Color(0,143,250));table.setSelectionForeground(Color.WHITE);
        table.setDefaultRenderer(Object.class,new RowRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){
                super.getTableCellRendererComponent(t,v,false,false,r,c);setFont(UngDungPhongChu.DICH_VU_TIEU_DE.deriveFont((float)(13*scale)));setBackground(Color.WHITE);setForeground(Color.BLACK);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0,0,1,1,new Color(218,218,218)),new EmptyBorder(0,px(8),0,0)));return this;
            }
        });
        tableScroll=scroll(table);tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        keyword.setBackground(new Color(255,255,204));keyword.setBorder(BorderFactory.createLineBorder(new Color(150,150,135)));
        keyword.setMargin(new Insets(0,7,0,7));
        keyword.setUI(new BasicTextFieldUI(){
            @Override protected void paintSafely(Graphics g){super.paintSafely(g);
                if(keyword.getText().isEmpty()&&!keyword.isFocusOwner()){g.setColor(Color.BLACK);g.setFont(keyword.getFont());g.drawString("Tìm kiếm (Ctrl+F)",7,(keyword.getHeight()-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent());}
            }
        });search.setBorder(new LineBorder(new Color(170,170,170)));
        searchBar.setBackground(Color.WHITE);searchBar.setBorder(new EmptyBorder(1,0,2,0));searchBar.add(keyword);searchBar.add(search,BorderLayout.EAST);
        JPanel right=new JPanel(new BorderLayout());right.setMinimumSize(new Dimension());right.add(searchBar,BorderLayout.NORTH);right.add(tableScroll);
        treeScroll.setMinimumSize(new Dimension());split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,treeScroll,right);
        split.setDividerSize(3);split.setBorder(new MatteBorder(0,1,1,1,new Color(120,160,165)));split.setResizeWeight(312.0/1586);host.add(split);
        footer.setBackground(Color.WHITE);footer.setBorder(new EmptyBorder(3,9,2,9));status.setVerticalAlignment(SwingConstants.CENTER);footer.add(status);host.add(footer,BorderLayout.SOUTH);
        save.setToolTipText("Lưu thứ tự hiển thị trên máy này");
    }
    public double scale(){return scale;}
    public void resize(JPanel host){
        scale=Math.max(.5,host.getWidth()/1586.0);
        Font plain=UngDungPhongChu.DICH_VU_DANH_MUC.deriveFont((float)(13*scale));
        toolbar.setPreferredSize(new Dimension(0,px(80)));footer.setPreferredSize(new Dimension(0,status.getText().isBlank()?0:px(26)));
        searchBar.setPreferredSize(new Dimension(0,px(30)));search.setPreferredSize(new Dimension(px(26),0));
        keyword.setFont(plain);status.setFont(plain);tree.setFont(plain);tree.setRowHeight(px(23));table.setFont(plain);table.setRowHeight(px(23));
        table.getTableHeader().setPreferredSize(new Dimension(0,px(28)));
        for(JButton b:List.of(menu,price,refresh,add,mapService,mapExam,mapBed,up,down,save,search)){
            b.setFont(UngDungPhongChu.DICH_VU_CONG_CU.deriveFont((float)(12*scale)));var icon=(DichVuBieuTuong)b.getIcon();b.setIcon(new DichVuBieuTuong(icon.kind(),px(b==search?19:b==save?30:b==up||b==down?28:28)));b.setDisabledIcon(b.getIcon());
        }
        int[] widths={213,195,396,76,118,116,114};for(int i=0;i<widths.length;i++){var c=table.getColumnModel().getColumn(i);c.setMinWidth(0);c.setPreferredWidth(px(widths[i]));c.setWidth(px(widths[i]));}
        for(JScrollPane scroll:List.of(treeScroll,tableScroll))scroll.getVerticalScrollBar().setPreferredSize(new Dimension(px(18),0));
        split.setDividerLocation(px(312));
    }
    private int px(int n){return Math.max(1,(int)Math.round(n*scale));}
    private final class RowRenderer extends DefaultTableCellRenderer {
        private DichVuDanhMucMoHinh.Row row;
        private int column, rowIndex;
        @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int r,int c){
            super.getTableCellRendererComponent(t,value,selected,false,r,c);row=model.row(r);column=c;rowIndex=r;
            setFont(row.kind()==DichVuDanhMucMoHinh.Kind.ITEM?t.getFont():UngDungPhongChu.DICH_VU_DANH_MUC_BOLD.deriveFont((float)(13*scale)));
            setBackground(selected?t.getSelectionBackground():r%2==0?Color.WHITE:new Color(246,246,247));setForeground(selected?Color.WHITE:Color.BLACK);
            setHorizontalAlignment(SwingConstants.LEFT);
            setHorizontalAlignment(c>=4?SwingConstants.RIGHT:c==3?SwingConstants.CENTER:SwingConstants.LEFT);
            if(value instanceof java.math.BigDecimal number){var format=java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));format.setMaximumFractionDigits(2);setText(format.format(number));}
            setBorder(new EmptyBorder(0,c==0?px(28+row.depth()*24):px(8),0,px(6)));setToolTipText(value==null?null:value.toString());return this;
        }
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);if(column!=0||row==null)return;
            Graphics2D g=(Graphics2D)graphics.create();int center=getHeight()/2,x=px(9+row.depth()*24);
            g.setColor(new Color(130,145,173));g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{1,2},0));
            if(row.depth()>0){
                boolean leaf=row.kind()==DichVuDanhMucMoHinh.Kind.ITEM;
                int parent=leaf?x+px(1):x-px(16);
                boolean nextSibling=leaf&&rowIndex+1<model.getRowCount()&&model.row(rowIndex+1).kind()==DichVuDanhMucMoHinh.Kind.ITEM
                        &&model.row(rowIndex+1).group().equals(row.group())&&model.row(rowIndex+1).category().equals(row.category());
                g.drawLine(parent,0,parent,nextSibling?getHeight():center);g.drawLine(parent,center,x+px(leaf?14:5),center);
            }
            if(row.kind()!=DichVuDanhMucMoHinh.Kind.ITEM){
                int size=px(10);g.setStroke(new BasicStroke(1));g.setColor(Color.WHITE);g.fillRect(x,center-size/2,size,size);
                g.setColor(row.kind()==DichVuDanhMucMoHinh.Kind.CATEGORY?new Color(50,90,235):new Color(90,90,90));g.drawRect(x,center-size/2,size,size);
                g.drawLine(x+2,center,x+size-2,center);if(!model.expanded(row))g.drawLine(x+size/2,center-size/2+2,x+size/2,center+size/2-2);
            }g.dispose();
        }
    }
    private static JButton tool(String text,DichVuBieuTuong.Kind kind){
        JButton b=inline(text,kind);b.setHorizontalTextPosition(SwingConstants.CENTER);b.setVerticalTextPosition(SwingConstants.BOTTOM);b.setIconTextGap(4);return b;
    }
    private static JButton inline(String text,DichVuBieuTuong.Kind kind){
        JButton b=new JButton(text,new DichVuBieuTuong(kind,34));b.setFont(UngDungPhongChu.DICH_VU_DANH_MUC);
        b.setContentAreaFilled(false);b.setFocusPainted(false);b.setBorder(new EmptyBorder(0,0,0,0));return b;
    }
    private static JScrollPane scroll(Component view){
        JScrollPane pane=new JScrollPane(view);pane.setBorder(null);pane.getViewport().setBackground(Color.WHITE);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        pane.getVerticalScrollBar().setUI(new BasicScrollBarUI(){
            @Override protected void configureScrollBarColors(){trackColor=new Color(244,244,244);thumbColor=new Color(194,194,194);}
            @Override protected JButton createDecreaseButton(int direction){return new BasicArrowButton(direction);}
            @Override protected JButton createIncreaseButton(int direction){return new BasicArrowButton(direction);}
            @Override protected void paintThumb(Graphics g,JComponent c,Rectangle r){g.setColor(thumbColor);g.fillRect(r.x,r.y,r.width,r.height);}
        });return pane;
    }
}
