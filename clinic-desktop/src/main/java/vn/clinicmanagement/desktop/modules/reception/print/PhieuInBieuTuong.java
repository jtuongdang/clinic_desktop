package vn.clinicmanagement.desktop.modules.reception.print;

import javax.swing.*;
import java.awt.*;
/** Biểu tượng vector cố định để hình nút không phụ thuộc emoji của hệ điều hành. */
public final class PhieuInBieuTuong implements Icon {
    private final boolean pdf;
    public PhieuInBieuTuong(boolean pdf) { this.pdf = pdf; }
    @Override public int getIconWidth() { return 34; }
    @Override public int getIconHeight() { return 34; }
    @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        if (pdf) {
            g.setColor(Color.WHITE);g.fillPolygon(new int[]{7,23,29,29,7},new int[]{1,1,7,32,32},5);
            g.setColor(new Color(140,155,150));g.drawPolygon(new int[]{7,23,29,29,7},new int[]{1,1,7,32,32},5);
            g.setColor(new Color(95,160,135));g.fillRect(1,10,24,12);
            g.setColor(Color.WHITE);g.setFont(vn.clinicmanagement.desktop.core.config.UngDungPhongChu.TINY);g.drawString("PDF",2,20);
        } else {
            g.setColor(new Color(160,165,165));g.fillRoundRect(2,12,30,16,3,3);
            g.setColor(new Color(190,210,220));g.fillRect(8,2,18,12);
            g.setColor(new Color(100,140,165));g.drawRect(8,2,18,12);
            g.setColor(Color.WHITE);g.fillRect(9,21,16,11);
            g.setColor(new Color(125,155,175));g.drawRect(9,21,16,11);g.drawLine(12,25,22,25);g.drawLine(12,28,22,28);
        }
        g.dispose();
    }
}
