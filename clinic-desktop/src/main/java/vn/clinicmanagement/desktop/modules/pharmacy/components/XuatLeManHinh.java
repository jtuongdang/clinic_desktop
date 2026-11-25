package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.retail.XuatLe;
import vn.clinicmanagement.application.inventory.retail.XuatLe.*;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static vn.clinicmanagement.desktop.modules.pharmacy.components.NhapNhaCungCapManHinh.MauPanel;
import static vn.clinicmanagement.desktop.modules.pharmacy.components.NhapNhaCungCapManHinh.button;

public final class XuatLeManHinh extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Color WINE = new Color(145,20,20), GRID = new Color(218,222,227);
    private static final Font FONT = new Font("Arial",Font.PLAIN,13);
    private final XuatLe service;
    private final PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
    private final Runnable refreshParent;
    private final String headingTitle;
    private final UUID key = UUID.randomUUID();
    private final Map<String,JTextField> fields = new LinkedHashMap<>();
    private final JComboBox<Warehouse> warehouse = new JComboBox<>();
    private final JComboBox<String> gender = new JComboBox<>(new String[]{"","Nam","Nữ","Khác"});
    private final JButton select = new JButton("…"), update = new JButton("Cập Nhật (Ctrl+A)");
    private final JButton save = button("Lưu (F8)","save"), print = button("Lưu + In (F9)","save");
    private final JLabel total = new JLabel("0đ"), status = new JLabel(" ");
    private final JPanel header = new JPanel();
    private final List<Line> lines = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"STT","Mã Thuốc","Tên Thuốc","Đơn Vị Tính",
            "Số Lượng","Giá bán","VAT(%)","Thành Tiền","Miễn Giảm(%)","SĐK","Số Lô","Hạn Sử Dụng"},0) {
        @Override public boolean isCellEditable(int row,int col) { return false; }
    };
    private final JTable table = new JTable(model) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isPaintingForPrint()) return;
            int top = getRowCount()*getRowHeight();
            g.setColor(GRID);
            for(int y=top+getRowHeight()-1;y<getHeight();y+=getRowHeight()) g.drawLine(0,y,getWidth(),y);
            int x=0;
            for(var col : Collections.list(getColumnModel().getColumns())) {
                x+=col.getWidth(); g.drawLine(x-1,top,x-1,getHeight());
            }
        }
    };
    private List<Lot> lots = List.of();
    private Lot selected;
    private BigDecimal discount = BigDecimal.ZERO;
    private boolean saving, saved;
    private int generation;

    public XuatLeManHinh(XuatLe service,Integer initialWarehouse,Runnable refreshParent) {
        this(service, initialWarehouse, refreshParent, "XUẤT THUỐC CHO KHÁCH MUA LẺ");
    }

    public XuatLeManHinh(XuatLe service,Integer initialWarehouse,Runnable refreshParent,String headingTitle) {
        super(new BorderLayout());
        this.service=service;
        this.refreshParent=refreshParent;
        this.headingTitle=headingTitle!=null&&!headingTitle.isBlank()?headingTitle.toUpperCase(Locale.ROOT):"XUẤT THUỐC CHO KHÁCH MUA LẺ";
        setPreferredSize(new Dimension(1280,680));
        header.setLayout(new BoxLayout(header,BoxLayout.Y_AXIS));
        header.add(heading());
        header.add(customerFields());
        header.add(drugFields());
        if (!"XUẤT THUỐC CHO KHÁCH MUA LẺ".equals(this.headingTitle) && fields.get("Ghi chú") != null) {
            fields.get("Ghi chú").setText(headingTitle);
        }
        add(header,BorderLayout.NORTH);
        add(grid(),BorderLayout.CENTER);
        add(footer(),BorderLayout.SOUTH);
        bind("F8",()->save(false)); bind("F9",()->save(true)); bind("ctrl A",this::applyLine);
        for(String k:List.of("Số lượng","%VAT","HDSD","Chẩn đoán")){
            var input=fields.get(k);
            input.getInputMap().put(KeyStroke.getKeyStroke("ctrl A"),"addRetailLine");
            input.getActionMap().put("addRetailLine",getActionMap().get("ctrl A"));
        }
        select.addActionListener(e->pick(text("Tên thuốc")));
        fields.get("Tên thuốc").addActionListener(e->pick(text("Tên thuốc")));
        fields.get("Mã vạch").addActionListener(e->pick(text("Mã vạch")));
        update.addActionListener(e->applyLine());
        save.addActionListener(e->save(false)); print.addActionListener(e->save(true));
        watch(fields.get("Số lượng"),this::displayAmount);
        watch(fields.get("%VAT"),this::displayAmount);
        warehouse.addActionListener(e->loadLots());
        controls();
        if(service==null || !XuatLe.permitted(auth)) {
            status.setText("Cần đăng nhập bằng tài khoản có quyền xuất thuốc.");
        } else {
            status.setText("Đang tải kho thuốc…");
            async(()->service.warehouses(auth), values->{
                warehouse.setModel(new DefaultComboBoxModel<>(values.toArray(Warehouse[]::new)));
                values.stream().filter(w->Objects.equals(initialWarehouse,w.id())).findFirst().ifPresent(warehouse::setSelectedItem);
                loadLots();
            });
        }
    }

    private JPanel heading() {
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE); panel.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,GRID),new EmptyBorder(4,12,4,14)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,46)); panel.setPreferredSize(new Dimension(1280,46));
        JButton menu=button("","menu");menu.setToolTipText("Thao tác phiếu");
        var popup=new JPopupMenu();
        item(popup,"Chọn thuốc trong kho",()->pick(""));
        item(popup,"Tải lại lô thuốc",this::loadLots);
        item(popup,"Sửa dòng đã chọn",this::editLine);
        item(popup,"Xóa dòng đã chọn",this::removeLine);
        item(popup,"Bỏ dòng đang nhập",this::clearLine);
        menu.addActionListener(e->popup.show(menu,0,menu.getHeight()));panel.add(menu,BorderLayout.WEST);
        JLabel title=new JLabel(headingTitle);
        title.setForeground(WINE);title.setFont(FONT.deriveFont(Font.BOLD,20f));panel.add(title,BorderLayout.EAST);
        return panel;
    }

    private MauPanel customerFields() {
        MauPanel p=new MauPanel(96);
        control(p,"Kho thuốc",warehouse,9,5,99,334);
        field(p,"Ngày lập",345,5,437,554,true).setText(DATE.format(LocalDate.now()));
        JButton datePicker=new JButton("▾");datePicker.setToolTipText("Chọn ngày lập");
        datePicker.setMargin(new Insets(0,0,0,0));p.place(datePicker,554,5,24,23);
        datePicker.addActionListener(e->chooseDate());
        field(p,"Người lập",587,5,674,928,false).setText(NguoiDungPhien.getInstance().getFullName());
        field(p,"Mã BN",9,34,99,237,true); field(p,"Họ và tên",246,34,333,578,true);
        field(p,"Tuổi",587,34,674,751,true); control(p,"Giới tính",gender,761,34,848,927);
        field(p,"Địa chỉ",936,34,1026,1329,true); field(p,"Điện thoại",1338,34,1433,1572,true);
        field(p,"Email",9,63,99,237,true); field(p,"Ghi chú",246,63,333,691,true);
        expandedField(p,"Bác sĩ",700,63,793,1105);
        p.setBorder(new MatteBorder(0,0,1,0,Color.GRAY)); return p;
    }

    private void chooseDate() {
        LocalDate day;
        try { day=LocalDate.parse(text("Ngày lập"),DATE); }
        catch(DateTimeException ex){day=LocalDate.now();}
        var picker=new JSpinner(new SpinnerDateModel(java.sql.Date.valueOf(day),null,null,Calendar.DAY_OF_MONTH));
        picker.setEditor(new JSpinner.DateEditor(picker,"dd/MM/yyyy"));
        if(JOptionPane.showConfirmDialog(this,picker,"Ngày lập phiếu",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            try {
                picker.commitEdit();
                Calendar cal=Calendar.getInstance();cal.setTime((java.util.Date)picker.getValue());
                put("Ngày lập",DATE.format(LocalDate.of(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DAY_OF_MONTH))));
            }catch(java.text.ParseException ex){status.setText("Ngày lập phải có dạng dd/MM/yyyy.");}
        }
    }

    private MauPanel drugFields() {
        MauPanel p=new MauPanel(120);
        field(p,"Tên thuốc",9,7,99,672,true).setBackground(new Color(255,253,233));
        select.setMargin(new Insets(0,0,0,0));select.setToolTipText("Chọn thuốc và lô còn tồn");p.place(select,672,7,24,23);
        field(p,"Mã thuốc",704,7,792,927,false);
        var stock=field(p,"Tồn kho",936,7,1025,1165,false);
        stock.setBackground(new Color(255,208,213)); stock.setForeground(Color.BLUE); stock.setHorizontalAlignment(JTextField.CENTER);
        field(p,"Mã vạch",1173,7,1258,1401,true).setBackground(new Color(255,249,175));
        field(p,"Hàm lượng",9,35,99,237,false); field(p,"Giá nhập",246,35,333,465,false);
        field(p,"Đơn vị",474,35,558,696,false); field(p,"Quy cách",704,35,792,927,false);
        field(p,"Nước SX",936,35,1025,1165,false); field(p,"Hãng SX",1173,35,1258,1572,false);
        field(p,"Số lượng",9,63,99,237,true); field(p,"Giá bán",246,63,333,465,false);
        field(p,"%VAT",474,63,558,696,true); field(p,"Thành tiền",704,63,792,927,false);
        expandedField(p,"HDSD",936,63,1025,1572);
        field(p,"Số ĐK",9,91,99,237,false); field(p,"Số lô",246,91,333,465,false);
        field(p,"Hạn SD",474,91,558,696,false); p.place(update,704,91,223,23);
        expandedField(p,"Chẩn đoán",936,91,1025,1572);
        for(String k:List.of("Số lượng","Giá nhập","Giá bán","%VAT","Thành tiền","Tồn kho")){
            fields.get(k).setHorizontalAlignment(JTextField.CENTER); fields.get(k).setText("0");
        }
        return p;
    }

    private void expandedField(MauPanel p,String key,int x,int y,int start,int end) {
        var input=field(p,key,x,y,start,end-24,true);
        JButton expand=new JButton("…");
        expand.setToolTipText(key);expand.setMargin(new Insets(0,0,0,0));
        expand.addActionListener(e->{
            var editor=new JTextArea(input.getText(),4,40);
            editor.setLineWrap(true);editor.setWrapStyleWord(true);
            if(JOptionPane.showConfirmDialog(this,new JScrollPane(editor),key,JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)input.setText(editor.getText().trim().replace('\n',' '));
        });
        p.place(expand,end-24,y,24,23);
    }
    private static void control(MauPanel p,String key,JComponent input,int x,int y,int start,int end) {
        JLabel label=new JLabel(key+":");label.setFont(FONT);label.setLabelFor(input);
        p.place(label,x,y,start-x-2,23);input.setFont(FONT);input.setName(key);p.place(input,start,y,end-start,23);
    }
    private JTextField field(MauPanel p,String key,int x,int y,int start,int end,boolean editable) {
        var f=new JTextField();
        f.setBorder(new CompoundBorder(new LineBorder(new Color(155,155,155)),new EmptyBorder(0,4,0,3)));
        f.setEditable(editable); f.setBackground(editable?Color.WHITE:new Color(247,247,247));
        fields.put(key,f);control(p,key,f,x,y,start,end);return f;
    }

    private JScrollPane grid(){
        table.setFont(FONT);table.setRowHeight(22);table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);table.setShowGrid(true);table.setGridColor(GRID);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(FONT.deriveFont(12f));table.getTableHeader().setReorderingAllowed(false);
        int[] widths={45,95,330,93,88,87,60,90,97,86,88,97};
        for(int i=0;i<widths.length;i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)editLine();}
        });
        var popup=new JPopupMenu(); item(popup,"Sửa dòng",this::editLine);
        item(popup,"Miễn giảm (%)",this::changeDiscount);item(popup,"Xóa dòng",this::removeLine);
        table.setComponentPopupMenu(popup);
        table.addMouseListener(new MouseAdapter(){
            private void selectRow(MouseEvent e){if(e.isPopupTrigger()){int row=table.rowAtPoint(e.getPoint());if(row>=0)table.setRowSelectionInterval(row,row);else table.clearSelection();}}
            @Override public void mousePressed(MouseEvent e){selectRow(e);}
            @Override public void mouseReleased(MouseEvent e){selectRow(e);}
        });
        var scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());scroll.setBorder(new LineBorder(GRID));return scroll;
    }

    private JPanel footer() {
        JPanel p=new JPanel(new BorderLayout(12,0));p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,Color.GRAY),new EmptyBorder(4,8,4,10)));
        p.setPreferredSize(new Dimension(1280,42));
        var sums=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));sums.setOpaque(false);
        sums.add(new JLabel("Tiền hóa đơn:"));total.setForeground(WINE);total.setFont(FONT.deriveFont(Font.BOLD,18f));total.setMinimumSize(new Dimension(115,23));total.setPreferredSize(new Dimension(170,23));sums.add(total);
        p.add(sums,BorderLayout.WEST); status.setFont(FONT);p.add(status,BorderLayout.CENTER);
        var actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0));actions.setOpaque(false);
        styleActionButton(save, new Color(16, 85, 165), new Color(235, 244, 255), new Color(140, 180, 225));
        styleActionButton(print, new Color(18, 120, 58), new Color(232, 247, 238), new Color(140, 205, 165));
        actions.add(save);actions.add(print);
        p.add(actions,BorderLayout.EAST);return p;
    }

    private static void styleActionButton(JButton btn, Color fg, Color bg, Color border) {
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFont(FONT.deriveFont(Font.BOLD, 12f));
        btn.setBorder(new CompoundBorder(new LineBorder(border, 1), new EmptyBorder(5, 12, 5, 12)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void loadLots() {
        if(saving || saved || service==null || !sameSession())return;
        var w=(Warehouse)warehouse.getSelectedItem(); int request=++generation;
        lots=List.of();clearLine();
        if(w==null){status.setText("Cơ sở chưa có kho đang hoạt động.");return;}
        status.setText("Đang tải lô thuốc…");
        async(()->service.lots(w.id(),auth),data->{
            if(request!=generation)return;
            lots=data;status.setText(data.isEmpty()?"Kho chưa có lô thuốc còn tồn và còn hạn.":" ");controls();
        });
    }

    private void pick(String search) {
        if(saved || saving || lots.isEmpty() || !sameSession())return;
        var options=lots;
        var exact=options.stream().filter(l->!search.isBlank() && (search.equalsIgnoreCase(l.barcode()) || search.equalsIgnoreCase(l.code()))).toList();
        if(exact.size()==1){choose(exact.get(0));return;}
        var list=new DefaultTableModel(new String[]{"Mã thuốc","Tên thuốc","Đơn vị","Số lô","Hạn dùng","Tồn","Giá bán","Mã vạch"},0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };
        for(var l:options)list.addRow(new Object[]{l.code(),l.name(),l.unit(),l.batch(),DATE.format(l.expiry()),number(l.stock()),number(l.price()),l.barcode()});
        var choices=new JTable(list);var sorter=new TableRowSorter<>(list);choices.setRowSorter(sorter);
        choices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);choices.getColumnModel().getColumn(1).setPreferredWidth(225);
        var query=new JTextField(search);Runnable filter=()->{
            sorter.setRowFilter(RowFilter.regexFilter("(?iu)"+java.util.regex.Pattern.quote(query.getText().trim())));
            if(choices.getRowCount()>0)choices.setRowSelectionInterval(0,0);
        };
        watch(query,filter);filter.run();
        var p=new JPanel(new BorderLayout(0,5));p.add(query,BorderLayout.NORTH);p.add(new JScrollPane(choices));
        p.setPreferredSize(new Dimension(Math.min(840,Math.max(520,getWidth()-80)),280));
        if(JOptionPane.showConfirmDialog(this,p,"Chọn thuốc và lô xuất",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION && choices.getSelectedRow()>=0)
            choose(options.get(choices.convertRowIndexToModel(choices.getSelectedRow())));
    }

    private void choose(Lot lot) {
        var existing=lines.stream().filter(l->l.lot().id()==lot.id()).findFirst();
        selected=lot;discount=existing.map(Line::discount).orElse(BigDecimal.ZERO);
        put("Tên thuốc",lot.name());put("Mã thuốc",lot.code());put("Mã vạch",lot.barcode());
        put("Đơn vị",lot.unit());put("Số lô",lot.batch());put("Hạn SD",DATE.format(lot.expiry()));
        put("Hàm lượng",lot.strength());put("Quy cách",lot.packing());put("Nước SX",lot.country());put("Hãng SX",lot.maker());
        put("Số ĐK",lot.registration());put("Tồn kho",number(lot.stock()));put("Giá nhập",number(lot.cost()));put("Giá bán",number(lot.price()));
        put("Số lượng",number(existing.map(Line::quantity).orElse(BigDecimal.ONE)));put("%VAT",number(existing.map(Line::vat).orElse(lot.vat())));
        put("HDSD",existing.map(Line::directions).orElse(lot.directions()));put("Chẩn đoán",existing.map(Line::diagnosis).orElse(""));
        fields.get("Tên thuốc").setEditable(false);fields.get("Mã vạch").setEditable(false);
        fields.get("Số lượng").requestFocusInWindow();fields.get("Số lượng").selectAll();controls();displayAmount();
    }
    private void clearLine() {
        if(saving || saved)return;
        selected=null;discount=BigDecimal.ZERO;
        for(String k:List.of("Tên thuốc","Mã thuốc","Mã vạch","Đơn vị","Số lô","Hạn SD","Hàm lượng","Quy cách","Nước SX","Hãng SX","Số ĐK","HDSD","Chẩn đoán"))put(k,"");
        for(String k:List.of("Tồn kho","Giá nhập","Giá bán","Số lượng","%VAT","Thành tiền"))put(k,"0");
        fields.get("Tên thuốc").setEditable(true);fields.get("Mã vạch").setEditable(true);controls();
    }
    private Line currentLine(){
        return new Line(selected,decimal(text("Số lượng")),decimal(text("%VAT")),discount,text("HDSD"),text("Chẩn đoán"));
    }
    private void displayAmount(){
        if(selected==null)return;
        try {put("Thành tiền",number(currentLine().amount()));}catch(NumberFormatException ex){put("Thành tiền","");}
    }
    private void applyLine(){
        if(selected==null || saving || saved || !sameSession())return;
        try{
            var line=currentLine();ticket(List.of(line)).validate();
            if(line.quantity().compareTo(selected.stock())>0)throw new IllegalArgumentException("Số lượng xuất vượt tồn của lô.");
            int i=0;while(i<lines.size() && lines.get(i).lot().id()!=selected.id())i++;
            if(i==lines.size())lines.add(line);else lines.set(i,line);
            clearLine();redraw();status.setText(" ");fields.get("Tên thuốc").requestFocusInWindow();
        }catch(RuntimeException ex){error(ex);}
    }
    private void editLine(){
        int row=table.getSelectedRow();
        if(row<0 || saving || saved)return;
        int id=lines.get(row).lot().id();
        lots.stream().filter(l->l.id()==id).findFirst().ifPresentOrElse(this::choose,()->status.setText("Lô đã hết tồn hoặc hết hạn. Hãy xóa dòng và chọn lô khác."));
    }
    private void changeDiscount(){
        int row=table.getSelectedRow();if(row<0 || saving || saved)return;
        Line l=lines.get(row);String value=JOptionPane.showInputDialog(this,"Miễn giảm (%)",number(l.discount()));
        if(value==null)return;
        try{var edited=new Line(l.lot(),l.quantity(),l.vat(),decimal(value),l.directions(),l.diagnosis());
            ticket(List.of(edited)).validate();lines.set(row,edited);clearLine();redraw();
        }catch(RuntimeException ex){error(ex);}
    }
    private void removeLine(){
        int row=table.getSelectedRow();if(row<0 || saving || saved)return;
        lines.remove(row);clearLine();redraw();
    }
    private void redraw(){
        model.setRowCount(0);BigDecimal sum=BigDecimal.ZERO;
        for(var l:lines){var b=l.lot();model.addRow(new Object[]{model.getRowCount()+1,b.code(),b.name(),b.unit(),number(l.quantity()),
                number(b.price()),number(l.vat()),number(l.amount()),number(l.discount()),b.registration(),b.batch(),DATE.format(b.expiry())});
            sum=sum.add(l.amount());}
        total.setText(java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(sum)+"đ");controls();
    }
    private Ticket ticket(List<Line> rows){
        var w=(Warehouse)warehouse.getSelectedItem();
        var buyer=new Customer(text("Mã BN"),text("Họ và tên"),text("Tuổi"),Objects.toString(gender.getSelectedItem(),""),
                text("Địa chỉ"),text("Điện thoại"),text("Email"),text("Ghi chú"),text("Bác sĩ"));
        return new Ticket(key,w==null?0:w.id(),LocalDate.parse(text("Ngày lập"),DATE),buyer,rows);
    }
    private void save(boolean withPrint){
        if(saving || !sameSession())return;
        if(saved){if(withPrint)printReceipt();return;}
        if(selected!=null || !text("Tên thuốc").isEmpty() || !text("Mã vạch").isEmpty()){
            String msg = "Hãy bấm Cập Nhật (Ctrl+A) để thêm dòng thuốc đang nhập vào phiếu trước khi lưu.";
            status.setText(msg);
            if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(this, msg, "Xuất lẻ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(service==null)return;
        if(lines.isEmpty()){
            String msg = "Phiếu chưa có thuốc nào. Vui lòng chọn thuốc, nhập số lượng và bấm Cập Nhật (Ctrl+A).";
            status.setText(msg);
            if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(this, msg, "Xuất lẻ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Ticket t;
        try{t=ticket(lines);t.validate();}catch(RuntimeException ex){error(ex);return;}
        saving=true;enableTree(header,false);controls();status.setText("Đang lưu phiếu…");
        new SwingWorker<KetQua<String>,Void>(){
            @Override protected KetQua<String> doInBackground(){return service.save(t,auth);}
            @Override protected void done(){
                saving=false;
                if(!sameSession()){controls();return;}
                try{var result=get();
                    if(result.isFailure())status.setText(result.getError().orElseThrow().message());
                    else{saved=true;status.setText("Đã lưu "+t.number());print.setText("In lại (F9)");refreshParent.run();}
                }catch(Exception ex){status.setText("Không xác nhận được kết quả lưu. Kiểm tra danh sách phiếu xuất lẻ.");}
                enableTree(header,!saved);controls();
                if(saved && withPrint)printReceipt();
            }
        }.execute();
    }
    private void printReceipt(){
        if(!saved || !sameSession())return;
        try{
            String heading="Phiếu xuất lẻ "+ticket(lines).number();
            // Escape free-form text before passing it to MessageFormat.
            String footer=text("Ngày lập")+"   "+text("Họ và tên")+"   Tổng tiền "+total.getText();
            boolean printed=table.print(JTable.PrintMode.FIT_WIDTH,new MessageFormat(heading),
                    new MessageFormat("'"+footer.replace("'","''")+"'"),true,null,true);
            status.setText(printed?"Đã gửi phiếu đến máy in.":"Đã lưu phiếu. Có thể in lại bằng F9.");
        }catch(PrinterException | RuntimeException ex){status.setText("Đã lưu phiếu. Kiểm tra máy in và nhấn F9 để in lại.");}
    }
    public boolean canClose(){
        boolean empty=lines.isEmpty() && selected==null && gender.getSelectedIndex()<=0 &&
                List.of("Mã BN","Họ và tên","Điện thoại","Email","Địa chỉ","Ghi chú","Bác sĩ","Tuổi","Tên thuốc","Mã vạch")
                        .stream().allMatch(k->text(k).isEmpty());
        return !saving && (saved || empty ||
                JOptionPane.showConfirmDialog(this,"Đóng phiếu đang nhập?","Xuất lẻ",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);
    }
    private void controls(){
        boolean editing=!saving && !saved;
        warehouse.setEnabled(editing && lines.isEmpty());select.setEnabled(editing && !lots.isEmpty());
        update.setEnabled(editing && selected!=null); save.setEnabled(editing);
        print.setEnabled(!saving);
    }
    private boolean sameSession(){
        if(auth!=null && auth.equals(KhamBenhPhienNguCanh.current()))return true;
        status.setText("Phiên làm việc đã thay đổi. Vui lòng mở lại phiếu.");return false;
    }
    private <T> void async(Callable<T> operation,Consumer<T> apply){
        new SwingWorker<T,Void>(){
            @Override protected T doInBackground() throws Exception{return operation.call();}
            @Override protected void done(){if(!sameSession())return;
                try{apply.accept(get());}catch(Exception ex){status.setText("Không tải được dữ liệu kho. Vui lòng mở lại phiếu.");}}
        }.execute();
    }
    private void error(RuntimeException ex){
        status.setText(ex instanceof DateTimeException?"Ngày lập phải có dạng dd/MM/yyyy.":
                ex instanceof NumberFormatException?"Vui lòng nhập số hợp lệ.":ex.getMessage());
    }
    private String text(String key){return fields.get(key).getText().trim();}
    private void put(String key,String value){fields.get(key).setText(Objects.toString(value,""));}
    private static String number(BigDecimal n){return n.stripTrailingZeros().toPlainString();}
    private static BigDecimal decimal(String s){
        if(!s.trim().matches("[0-9]{1,16}([.,][0-9]{1,4})?"))throw new NumberFormatException();
        return new BigDecimal(s.trim().replace(',','.'));
    }
    private static void enableTree(Component c,boolean enabled){
        c.setEnabled(enabled);
        if(c instanceof Container p)for(var child:p.getComponents())enableTree(child,enabled);
    }
    private void bind(String key,Runnable action){
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),key);
        getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){action.run();}});
    }
    private static void item(JPopupMenu p,String name,Runnable r){p.add(name).addActionListener(e->r.run());}
    private static void watch(JTextField f,Runnable r){
        f.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){r.run();}
            public void removeUpdate(DocumentEvent e){r.run();}
            public void changedUpdate(DocumentEvent e){r.run();}
        });
    }
}
