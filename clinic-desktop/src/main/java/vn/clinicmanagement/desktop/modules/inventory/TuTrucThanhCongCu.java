package vn.clinicmanagement.desktop.modules.inventory;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.*;
import java.time.format.*;
import java.util.function.Consumer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** Thanh công cụ chuẩn 62px đồng bộ với các phân hệ chính. */
public final class TuTrucThanhCongCu extends JPanel {
    final JButton menu = tool("Menu", NhaThuocBieuTuong.createMenuIcon(), 58);
    final JButton report = tool("Báo Cáo", printer(), 72);
    final JButton list = tool("Danh Sách", NhaThuocBieuTuong.createListIcon(), 82);
    final JButton create = tool("Tạo Phiếu", NhaThuocBieuTuong.createPlusIcon(), 86);
    final JTextField code = new JTextField();
    final JSpinner from = date(false), until = date(true);
    final JButton search = new JButton("Tìm Kiếm", scaled(NhaThuocBieuTuong.createSearchIcon(), 24));
    private Consumer<String> createTicketListener;
    private final TuTrucTaoPhieuMenu createTicketMenu = new TuTrucTaoPhieuMenu(type -> {
        if (createTicketListener != null) createTicketListener.accept(type);
    });

    TuTrucThanhCongCu() {
        super(new BorderLayout());
        setName("tuTrucToolbar");
        setPreferredSize(new Dimension(1000, 62));
        setBackground(new Color(247,247,249));
        setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(150,150,150)));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); left.setOpaque(false);
        left.add(menu); left.add(report); left.add(separator()); left.add(list); left.add(separator()); left.add(create);
        JLabel label = new JLabel("Mã phiếu:"); label.setFont(UngDungPhongChu.TU_TRUC);
        label.setBorder(new EmptyBorder(0,8,0,6)); left.add(label);
        code.setName("tuTrucMaPhieu"); code.setFont(UngDungPhongChu.TU_TRUC);
        code.setPreferredSize(new Dimension(150,26)); left.add(code);
        add(left,BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout(10,0)); right.setOpaque(false);
        right.setBorder(new EmptyBorder(4,6,4,10));
        JPanel dates = new JPanel(new BorderLayout(8,0)); dates.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(2,1,0,4)); labels.setOpaque(false);
        JPanel values = new JPanel(new GridLayout(2,1,0,4)); values.setOpaque(false);
        for (String title:new String[]{"Từ ngày", "Đến ngày"}) {
            JLabel dateLabel=new JLabel(title); dateLabel.setFont(UngDungPhongChu.TU_TRUC); labels.add(dateLabel);
            values.add(title.equals("Từ ngày")?from:until);
        }
        dates.add(labels,BorderLayout.WEST);dates.add(values,BorderLayout.CENTER);
        right.add(dates,BorderLayout.CENTER);
        search.setName("tuTrucTimKiem"); search.setFont(UngDungPhongChu.TU_TRUC_BOLD);
        search.setContentAreaFilled(false); search.setBorder(new EmptyBorder(0,4,0,4));
        search.setFocusPainted(false);search.setPreferredSize(new Dimension(112,52)); right.add(search,BorderLayout.EAST); add(right,BorderLayout.EAST);

        create.setName("tuTrucTaoPhieu");
        create.getAccessibleContext().setAccessibleName("Tạo Phiếu");
        createTicketMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) { highlightCreate(true); }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { highlightCreate(false); }
            public void popupMenuCanceled(PopupMenuEvent e) { highlightCreate(false); }
        });
        create.addActionListener(e -> {
            if (createTicketMenu.isVisible()) createTicketMenu.setVisible(false);
            else if (create.isShowing()) createTicketMenu.show(create, -12, create.getHeight());
        });
        create.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), "openCreateMenu");
        create.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("F4"), "openCreateMenu");
        create.getActionMap().put("openCreateMenu", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { create.doClick(); }
        });
    }

    public TuTrucTaoPhieuMenu getCreateTicketMenu() { return createTicketMenu; }
    public void setOnCreateTicketListener(Consumer<String> listener) { createTicketListener = listener; }
    private void highlightCreate(boolean open) {
        create.putClientProperty("tuTrucMenuMo", open);
        create.getAccessibleContext().setAccessibleDescription(open ? "Menu tạo phiếu đang mở" : "Mở menu tạo phiếu");
        create.repaint();
    }

    LocalDateTime[] range() {
        try {
            from.commitEdit(); until.commitEdit();
            LocalDateTime f=LocalDateTime.ofInstant(((java.util.Date)from.getValue()).toInstant(),ZoneId.systemDefault());
            LocalDateTime t=LocalDateTime.ofInstant(((java.util.Date)until.getValue()).toInstant(),ZoneId.systemDefault());
            if (f.isAfter(t)) throw new IllegalArgumentException();
            return new LocalDateTime[]{f,t.plusMinutes(1)};
        } catch (java.text.ParseException|IllegalArgumentException ex) {
            throw new IllegalArgumentException("Kiểm tra từ ngày và đến ngày (HH:mm dd/MM/yyyy).");
        }
    }

    private static JSpinner date(boolean end) {
        var value=LocalDate.now().atTime(end?LocalTime.of(23,59):LocalTime.MIDNIGHT);
        JSpinner spinner=new JSpinner(new SpinnerDateModel(java.sql.Timestamp.valueOf(value),null,null,java.util.Calendar.DAY_OF_MONTH));
        var editor=new JSpinner.DateEditor(spinner,"HH:mm dd/MM/yyyy");
        editor.getFormat().setLenient(false); spinner.setEditor(editor);
        editor.getTextField().setFont(UngDungPhongChu.TU_TRUC); spinner.setPreferredSize(new Dimension(148,24));
        return spinner;
    }
    private static JComponent separator() {
        JPanel p=new JPanel(); p.setOpaque(false); p.setPreferredSize(new Dimension(1,50));
        p.setBorder(BorderFactory.createMatteBorder(0,0,0,1,new Color(165,165,165))); return p;
    }
    private static JButton tool(String title,Icon icon,int width) {
        JButton button=new JButton(title,scaled(icon,24)) {
            @Override protected void paintComponent(Graphics g){
                if(Boolean.TRUE.equals(getClientProperty("tuTrucMenuMo"))){
                    Graphics2D copy=(Graphics2D)g.create();
                    copy.setPaint(new GradientPaint(0,0,new Color(225,241,255),0,getHeight(),new Color(170,208,240)));
                    copy.fillRect(1,1,getWidth()-2,getHeight()-2);copy.setColor(new Color(83,137,182));
                    copy.drawRect(1,1,getWidth()-3,getHeight()-3);copy.setColor(new Color(65,90,110));
                    copy.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{1,2},0));
                    copy.drawRect(3,3,getWidth()-7,getHeight()-7);copy.dispose();
                }
                super.paintComponent(g);if(!title.equals("Menu")){
                g.setColor(new Color(40,40,40));int x=getWidth()-10,y=25;
                g.fillPolygon(new int[]{x,x+6,x+3},new int[]{y,y,y+3},3);
            }}
        }; button.setFont(UngDungPhongChu.TU_TRUC);
        button.setHorizontalTextPosition(SwingConstants.CENTER); button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setPreferredSize(new Dimension(width,60)); button.setFocusPainted(false);
        button.setContentAreaFilled(false); button.setBorder(new EmptyBorder(3,2,3,2));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button;
    }
    static Icon scaled(Icon icon,int size) {
        return new Icon() {
            public int getIconWidth(){return size;} public int getIconHeight(){return size;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D copy=(Graphics2D)g.create(); copy.translate(x,y);
                copy.scale((double)size/icon.getIconWidth(),(double)size/icon.getIconHeight());
                icon.paintIcon(c,copy,0,0); copy.dispose();
            }
        };
    }
    private static Icon printer() {
        return new Icon() {
            public int getIconWidth(){return 32;} public int getIconHeight(){return 32;}
            public void paintIcon(Component c,Graphics original,int x,int y){
            Graphics2D g=(Graphics2D)original.create();g.translate(x,y);
            g.setColor(new Color(135,145,150));g.fillRoundRect(2,11,28,15,3,3);
            g.setColor(Color.WHITE);g.fillRect(9,1,14,13);g.fillRect(9,20,14,11);
            g.setColor(new Color(75,105,125));g.drawRect(9,1,14,13);g.drawRect(9,20,14,11);
            g.drawLine(12,24,20,24);g.drawLine(12,27,20,27);g.setColor(new Color(30,135,210));g.fillRect(24,15,3,2);
            g.dispose();
            }
        };
    }
}
