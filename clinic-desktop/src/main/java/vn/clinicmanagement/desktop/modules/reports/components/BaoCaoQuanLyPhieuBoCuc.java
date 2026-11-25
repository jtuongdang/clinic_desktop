package vn.clinicmanagement.desktop.modules.reports.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import static vn.clinicmanagement.desktop.modules.reports.components.PhieuBaoCaoMoHinh.*;

/** Same density, fonts, colors and 58px toolbar as the pharmacy register. */
public final class BaoCaoQuanLyPhieuBoCuc {
    public record Filter(String key,java.util.function.Predicate<Phieu> predicate) { }
    public final JButton menu=tool("Menu",NhaThuocBieuTuong.createMenuIcon(),54),report=tool("Báo Cáo",NhaThuocBieuTuong.createReportIcon(),70),
            export=tool("Xuất Dữ Liệu",NhaThuocBieuTuong.createReportIcon(),92),list=tool("Danh Sách",NhaThuocBieuTuong.createListIcon(),78),refresh=tool("Làm Mới",new BaoCaoBieuTuong(BaoCaoBieuTuong.Kind.REFRESH,24),76),search=new JButton("Tìm Kiếm",NhaThuocBieuTuong.createSearchIcon());
    public final JTextField code=new JTextField(),from=new JTextField(),to=new JTextField(),keyword=new JTextField();
    public final JComboBox<String> mode=new JComboBox<>(new String[]{"Hiển thị tổng hợp","Đang xử lý","Đã hoàn thành","Đã hủy"});
    public final JTable table;public final JSplitPane split;public final JPanel toolbar,footer=new JPanel(new BorderLayout());
    public final JLabel status=new JLabel("Chọn khoảng ngày và bấm Tìm Kiếm để xem phiếu."),count=new JLabel("0 phiếu");
    private final JPanel sidebar=new JPanel(new BorderLayout()),branches=new JPanel(),summary=new JPanel(new GridLayout(4,1));
    private final JLabel[] totals={new JLabel(),new JLabel(),new JLabel(),new JLabel()};
    private final Map<String,JButton> branchButtons=new LinkedHashMap<>();
    private Consumer<Filter> listener=filter->{};private String selectedKey="ALL";
    private static final Color LINE=new Color(119,133,187),BLUE=new Color(0,142,255);
    public BaoCaoQuanLyPhieuBoCuc(JPanel host,PhieuBaoCaoMoHinh model){
        host.setLayout(new BorderLayout());host.setBackground(Color.WHITE);host.setPreferredSize(new Dimension(1440,760));
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));tools.setOpaque(false);for(JButton button:List.of(menu,report,export,list,refresh))tools.add(button);
        toolbar=new JPanel(new BorderLayout());toolbar.setName("reports.toolbar");toolbar.setBackground(new Color(235,241,247));toolbar.setBorder(new MatteBorder(0,0,1,0,new Color(189,202,215)));toolbar.add(tools,BorderLayout.WEST);
        JPanel filters=new JPanel(new GridBagLayout());filters.setOpaque(false);GridBagConstraints g=new GridBagConstraints();g.fill=GridBagConstraints.HORIZONTAL;g.anchor=GridBagConstraints.WEST;g.insets=new Insets(1,4,1,4);
        LocalDate today=LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));DateTimeFormatter date=DateTimeFormatter.ofPattern("dd/MM/yyyy");from.setText("00:00 "+today.minusDays(30).format(date));to.setText("23:59 "+today.format(date));
        mode.setFont(UngDungPhongChu.BODY);mode.setPreferredSize(new Dimension(140,22));
        for(JTextField field:List.of(code,from,to)){field.setFont(UngDungPhongChu.BODY);field.setPreferredSize(new Dimension(field==code?140:132,22));field.setBorder(new CompoundBorder(new LineBorder(new Color(165,180,196)),new EmptyBorder(1,4,1,4)));}
        Object[][] fields={{"Tìm theo:",mode,"Từ ngày:",from},{"Mã phiếu:",code,"Đến ngày:",to}};
        for(int r=0;r<2;r++)for(int c=0;c<4;c++){g.gridy=r;g.gridx=c;Object value=fields[r][c];if(value instanceof String text){JLabel label=new JLabel(text);label.setFont(c==0&&r==1?UngDungPhongChu.BODY_BOLD:UngDungPhongChu.BODY);filters.add(label,g);}else filters.add((Component)value,g);}
        search.setFont(UngDungPhongChu.BODY_BOLD);search.setForeground(new Color(30,55,82));search.setPreferredSize(new Dimension(113,46));search.setBackground(new Color(231,239,249));
        JPanel right=new JPanel(new BorderLayout(8,0));right.setOpaque(false);right.setBorder(new EmptyBorder(4,4,4,6));right.add(filters);right.add(search,BorderLayout.EAST);toolbar.add(right,BorderLayout.EAST);host.add(toolbar,BorderLayout.NORTH);
        sidebar.setName("reports.sidebar");sidebar.setBackground(Color.WHITE);sidebar.setBorder(new MatteBorder(1,1,0,1,new Color(192,192,192)));
        JLabel title=new JLabel("Báo Cáo - Quản Lý Phiếu");title.setFont(UngDungPhongChu.SECTION);title.setForeground(Color.RED);title.setBorder(new EmptyBorder(0,7,0,2));title.setPreferredSize(new Dimension(270,43));sidebar.add(title,BorderLayout.NORTH);
        branches.setLayout(new BoxLayout(branches,BoxLayout.Y_AXIS));branches.setBackground(Color.WHITE);JPanel treeHolder=new JPanel(new BorderLayout());treeHolder.setBackground(Color.WHITE);treeHolder.add(branches,BorderLayout.NORTH);
        JScrollPane treeScroll=new JScrollPane(treeHolder);treeScroll.setBorder(null);treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);treeScroll.getVerticalScrollBar().setUnitIncrement(32);sidebar.add(treeScroll);
        summary.setBackground(Color.WHITE);summary.setPreferredSize(new Dimension(270,134));String[] titles={"Số phiếu hiển thị:","Phiếu đang xử lý:","Tổng tiền hóa đơn:","Đã thu hóa đơn:"};
        for(int i=0;i<4;i++){JPanel line=new JPanel(new BorderLayout());line.setOpaque(false);line.setBorder(new EmptyBorder(3,8,3,8));JLabel label=new JLabel(titles[i]);label.setFont(UngDungPhongChu.BODY);line.add(label);totals[i].setFont(UngDungPhongChu.BODY);totals[i].setForeground(i<2?Color.RED:Color.BLUE);line.add(totals[i],BorderLayout.EAST);summary.add(line);}sidebar.add(summary,BorderLayout.SOUTH);
        table=new JTable(model){@Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D canvas=(Graphics2D)graphics.create();canvas.setColor(new Color(221,227,234));int start=getRowCount()*getRowHeight();for(int y=start;y<getHeight();y+=getRowHeight())canvas.drawLine(0,y,getWidth(),y);int x=0;for(int c=0;c<getColumnCount();c++){x+=getColumnModel().getColumn(c).getWidth();canvas.drawLine(x,start,x,getHeight());}canvas.dispose();}};
        table.setName("reports.tickets");table.setFont(UngDungPhongChu.BODY);table.setRowHeight(24);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setSelectionBackground(BLUE);table.setSelectionForeground(Color.WHITE);table.setShowGrid(true);table.setGridColor(new Color(221,227,234));table.setFillsViewportHeight(true);table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setPreferredSize(new Dimension(0,26));
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){super.getTableCellRendererComponent(t,value,selected,false,row,col);putClientProperty("html.disable",true);setFont(UngDungPhongChu.BODY);setBorder(new EmptyBorder(0,5,0,5));setHorizontalAlignment(col==8?SwingConstants.RIGHT:SwingConstants.LEFT);setBackground(selected?BLUE:Color.WHITE);setForeground(selected?Color.WHITE:col==0&&state(model.row(row).status())==NhomTrangThai.CANCELLED?Color.RED:Color.BLACK);setToolTipText(Objects.toString(value,""));return this;}});
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object v,boolean s,boolean f,int r,int c){super.getTableCellRendererComponent(t,v,false,false,r,c);setFont(UngDungPhongChu.BODY_BOLD);setForeground(new Color(35,43,53));setBackground(new Color(234,239,245));setHorizontalAlignment(SwingConstants.CENTER);setBorder(new MatteBorder(0,0,1,1,new Color(221,227,234)));return this;}});
        int[] widths={120,135,100,145,150,190,155,140,110},minimum={95,125,75,110,135,150,100,110,85};for(int i=0;i<widths.length;i++){table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);table.getColumnModel().getColumn(i).setMinWidth(minimum[i]);}
        JScrollPane grid=new JScrollPane(table);grid.setBorder(null);grid.setColumnHeaderView(table.getTableHeader());
        JPanel searchStrip=new JPanel(new BorderLayout());searchStrip.setPreferredSize(new Dimension(0,26));keyword.setFont(UngDungPhongChu.BODY);keyword.setBackground(new Color(255,255,204));keyword.setBorder(new EmptyBorder(0,10,0,5));keyword.putClientProperty("JTextField.placeholderText","Tìm kiếm (Ctrl+F)");searchStrip.add(keyword);JButton find=new JButton(NhaThuocBieuTuong.createSearchIcon());find.setPreferredSize(new Dimension(28,26));find.setBorder(new MatteBorder(0,1,0,0,new Color(222,220,170)));find.setContentAreaFilled(false);find.addActionListener(e->keyword.requestFocusInWindow());searchStrip.add(find,BorderLayout.EAST);searchStrip.setBackground(new Color(255,255,204));
        JPanel tableArea=new JPanel(new BorderLayout());tableArea.setMinimumSize(new Dimension());tableArea.add(searchStrip,BorderLayout.NORTH);tableArea.add(grid);
        split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,sidebar,tableArea);split.setBorder(null);split.setDividerSize(2);split.setContinuousLayout(true);split.setResizeWeight(.1875);sidebar.setMinimumSize(new Dimension(220,0));host.add(split);
        footer.setBackground(new Color(246,248,251));footer.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,new Color(211,219,228)),new EmptyBorder(0,8,0,8)));footer.setPreferredSize(new Dimension(0,24));status.setFont(UngDungPhongChu.SMALL);count.setFont(UngDungPhongChu.SMALL);footer.add(status);footer.add(count,BorderLayout.EAST);host.add(footer,BorderLayout.SOUTH);
        rebuild(List.of());updateSummary(List.of());
    }
    public void resize(JPanel host){toolbar.setPreferredSize(new Dimension(0,58));split.setDividerLocation(Math.max(220,(int)Math.round(host.getWidth()*.1875)));}
    public void setFilterListener(Consumer<Filter> listener){this.listener=listener;}
    public void select(String key){selectedKey=key;branchButtons.forEach((k,b)->{b.setForeground(k.equals(selectedKey)?Color.WHITE:Color.BLACK);b.repaint();});}
    public void rebuild(List<Phieu> rows){
        branches.removeAll();branchButtons.clear();addBranch(branches,"ALL","Tất Cả Phiếu ("+rows.size()+")",true,false,row->true);
        JPanel types=section("Loại Phiếu");for(Loai type:Loai.values())addBranch(types,"TYPE_"+type.name(),type.title()+" ("+rows.stream().filter(r->r.type()==type).count()+")",false,type==Loai.CHI_DINH,r->r.type()==type);
        JPanel states=section("Trạng thái");for(NhomTrangThai group:NhomTrangThai.values())addBranch(states,"STATE_"+group.name(),group.title()+" ("+rows.stream().filter(r->state(r.status())==group).count()+")",false,group==NhomTrangThai.CANCELLED,r->state(r.status())==group);
        var departments=rows.stream().map(Phieu::department).filter(s->!safe(s).isBlank()).distinct().sorted().toList();if(!departments.isEmpty()){JPanel groups=section("Khoa / Phòng");for(int i=0;i<departments.size();i++){String dept=departments.get(i);addBranch(groups,"DEPT_"+dept,dept+" ("+rows.stream().filter(r->dept.equals(r.department())).count()+")",false,i==departments.size()-1,r->dept.equals(r.department()));}}
        select(selectedKey);branches.revalidate();branches.repaint();
    }
    private JPanel section(String name){
        JPanel group=new JPanel();group.setLayout(new BoxLayout(group,BoxLayout.Y_AXIS));group.setOpaque(false);group.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel children=new JPanel();children.setLayout(new BoxLayout(children,BoxLayout.Y_AXIS));children.setOpaque(false);children.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton heading=new JButton(name){@Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);graphics.setColor(Color.GRAY);graphics.drawRect(12,10,12,12);graphics.drawLine(14,16,22,16);if(!children.isVisible())graphics.drawLine(18,12,18,20);graphics.setColor(LINE);graphics.drawLine(18,23,18,getHeight());}};
        heading.setFont(UngDungPhongChu.BODY_BOLD);heading.setHorizontalAlignment(SwingConstants.LEFT);heading.setBorder(new EmptyBorder(0,36,0,0));heading.setBackground(Color.WHITE);heading.setFocusPainted(false);heading.addActionListener(e->{children.setVisible(!children.isVisible());heading.repaint();});sizedRow(heading,32);group.add(heading);group.add(children);branches.add(group);return children;
    }
    private void addBranch(JPanel parent,String key,String text,boolean root,boolean last,java.util.function.Predicate<Phieu> predicate){
        JButton button=new JButton(text){@Override protected void paintComponent(Graphics graphics){graphics.setColor(key.equals(selectedKey)?BLUE:Color.WHITE);graphics.fillRect(0,0,getWidth(),getHeight());super.paintComponent(graphics);graphics.setColor(LINE);if(root){graphics.drawLine(18,getHeight()/2,28,getHeight()/2);graphics.drawLine(18,getHeight()/2,18,getHeight());}else{graphics.drawLine(18,0,18,getHeight());graphics.drawLine(44,0,44,last?getHeight()/2:getHeight());graphics.drawLine(44,getHeight()/2,58,getHeight()/2);}}};
        button.setFont(root?UngDungPhongChu.BODY_BOLD:UngDungPhongChu.BODY);button.setHorizontalAlignment(SwingConstants.LEFT);button.setContentAreaFilled(false);button.setFocusPainted(false);button.setBorder(new EmptyBorder(0,root?32:64,0,3));button.setToolTipText(text);sizedRow(button,root?34:32);button.addActionListener(e->{select(key);listener.accept(new Filter(key,predicate));});branchButtons.put(key,button);parent.add(button);
    }
    private static void sizedRow(JComponent c,int height){c.setAlignmentX(Component.LEFT_ALIGNMENT);c.setPreferredSize(new Dimension(270,height));c.setMinimumSize(new Dimension(0,height));c.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));}
    public void updateSummary(List<Phieu> rows){totals[0].setText(String.valueOf(rows.size()));totals[1].setText(String.valueOf(rows.stream().filter(r->state(r.status())==NhomTrangThai.PENDING).count()));List<Phieu> invoices=rows.stream().filter(r->r.type()==Loai.HOA_DON).toList();totals[2].setText(invoices.isEmpty()?"—":money(invoices.stream().map(Phieu::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add)));totals[3].setText(invoices.isEmpty()?"—":money(invoices.stream().map(Phieu::paid).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add)));count.setText(rows.size()+" phiếu");}
    public JButton filterButton(String key){return branchButtons.get(key);}
    public static JButton tool(String text,Icon icon,int width){JButton button=new JButton(text,new Icon(){public int getIconWidth(){return 24;}public int getIconHeight(){return 24;}public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D canvas=(Graphics2D)g.create();canvas.translate(x,y);canvas.scale(24d/icon.getIconWidth(),24d/icon.getIconHeight());icon.paintIcon(c,canvas,0,0);canvas.dispose();}});button.setFont(UngDungPhongChu.BODY);button.setHorizontalTextPosition(SwingConstants.CENTER);button.setVerticalTextPosition(SwingConstants.BOTTOM);button.setIconTextGap(3);button.setFocusPainted(false);button.setContentAreaFilled(false);button.setBorder(new EmptyBorder(3,2,3,2));button.setPreferredSize(new Dimension(width,54));return button;}
}
