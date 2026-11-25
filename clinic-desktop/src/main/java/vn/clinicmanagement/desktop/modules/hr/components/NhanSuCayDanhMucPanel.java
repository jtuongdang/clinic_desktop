package vn.clinicmanagement.desktop.modules.hr.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.ViewSection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Cây điều hướng bên trái và bảng thống kê nhanh cho phân hệ Quản Lý Nhân Sự.
 */
public final class NhanSuCayDanhMucPanel extends JPanel {
    private final JTree navTree;
    private final JLabel lblTotalEmp = new JLabel("0");
    private final JLabel lblActiveEmp = new JLabel("0");
    private final JLabel lblAttendanceCount = new JLabel("0");
    private final JLabel lblTotalPayroll = new JLabel("0đ");

    public NhanSuCayDanhMucPanel(Consumer<ViewSection> onSectionSelected) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(245, 650));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, new Color(192, 192, 192)));

        JLabel lblHeader = new JLabel("Quản Lý Nhân Sự");
        lblHeader.setFont(UngDungPhongChu.SECTION);
        lblHeader.setForeground(Color.RED);
        lblHeader.setBorder(new EmptyBorder(8, 10, 6, 6));
        add(lblHeader, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tất Cả Nhân Sự");

        DefaultMutableTreeNode empGroup = new DefaultMutableTreeNode("Hồ Sơ & Phân Quyền");
        DefaultMutableTreeNode allNode = new DefaultMutableTreeNode(ViewSection.ALL_EMPLOYEES);
        empGroup.add(allNode);
        empGroup.add(new DefaultMutableTreeNode(ViewSection.ACTIVE_EMPLOYEES));
        empGroup.add(new DefaultMutableTreeNode(ViewSection.LOCKED_EMPLOYEES));

        DefaultMutableTreeNode attGroup = new DefaultMutableTreeNode("Chấm Công & Lịch Trực");
        attGroup.add(new DefaultMutableTreeNode(ViewSection.SCHEDULES));
        attGroup.add(new DefaultMutableTreeNode(ViewSection.ATTENDANCE));

        DefaultMutableTreeNode payGroup = new DefaultMutableTreeNode("Tiền Lương & Hoa Hồng");
        payGroup.add(new DefaultMutableTreeNode(ViewSection.PAYROLL));

        root.add(empGroup);
        root.add(attGroup);
        root.add(payGroup);

        navTree = new JTree(new DefaultTreeModel(root));
        navTree.setFont(UngDungPhongChu.BODY);
        navTree.setRowHeight(24);
        navTree.setBorder(new EmptyBorder(4, 6, 4, 6));
        for (int i = 0; i < navTree.getRowCount(); i++) navTree.expandRow(i);
        navTree.setSelectionPath(new TreePath(allNode.getPath()));

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        navTree.setCellRenderer(renderer);

        navTree.addTreeSelectionListener(e -> {
            if (navTree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof ViewSection section
                    && onSectionSelected != null) {
                onSectionSelected.accept(section);
            }
        });
        add(new JScrollPane(navTree), BorderLayout.CENTER);

        JPanel summary = new JPanel(new GridLayout(4, 2, 6, 5));
        summary.setBackground(new Color(248, 250, 252));
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 215, 220)),
                new EmptyBorder(8, 10, 8, 10)));
        summary.add(new JLabel("Tổng nhân sự :"));
        lblTotalEmp.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotalEmp.setFont(UngDungPhongChu.BODY_BOLD);
        lblTotalEmp.setForeground(new Color(10, 86, 179));
        summary.add(lblTotalEmp);

        summary.add(new JLabel("Đang hoạt động :"));
        lblActiveEmp.setHorizontalAlignment(SwingConstants.RIGHT);
        lblActiveEmp.setFont(UngDungPhongChu.BODY_BOLD);
        lblActiveEmp.setForeground(new Color(15, 118, 56));
        summary.add(lblActiveEmp);

        summary.add(new JLabel("Chấm công tháng :"));
        lblAttendanceCount.setHorizontalAlignment(SwingConstants.RIGHT);
        lblAttendanceCount.setFont(UngDungPhongChu.BODY_BOLD);
        summary.add(lblAttendanceCount);

        summary.add(new JLabel("Tổng quỹ lương :"));
        lblTotalPayroll.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotalPayroll.setFont(UngDungPhongChu.BODY_BOLD);
        lblTotalPayroll.setForeground(new Color(180, 30, 30));
        summary.add(lblTotalPayroll);
        add(summary, BorderLayout.SOUTH);
    }

    public void setTotalEmployees(int count) {
        lblTotalEmp.setText(String.valueOf(count));
    }

    public void setActiveEmployees(long count) {
        lblActiveEmp.setText(String.valueOf(count));
    }

    public void setAttendanceCount(int count) {
        lblAttendanceCount.setText(String.valueOf(count));
    }

    public void setTotalPayrollText(String text) {
        lblTotalPayroll.setText(text);
    }
}
