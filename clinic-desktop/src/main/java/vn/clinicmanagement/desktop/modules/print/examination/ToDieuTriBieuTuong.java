package vn.clinicmanagement.desktop.modules.print.examination;


import vn.clinicmanagement.desktop.modules.print.billing.*;
import javax.swing.*;
import java.awt.*;

/** Vector artwork for the treatment document viewer, independent of OS emoji fonts. */
final class ToDieuTriBieuTuong implements Icon {
    enum Kind { SIGN, APPROVE, SAVE, CANCEL, PDF, MENU, PRINT, CHECK }
    private final Kind kind;
    private final int size;
    ToDieuTriBieuTuong(Kind kind, int size) { this.kind = kind; this.size = size; }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D)graphics.create(); g.translate(x,y); g.scale(size/40.0,size/40.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(new Color(140,140,140));
        switch(kind) {
            case CANCEL -> { g.drawLine(7,7,33,33); g.drawLine(7,33,33,7); }
            case CHECK, APPROVE -> {
                if(kind == Kind.APPROVE) { g.drawRect(4,9,27,26); g.setColor(new Color(145,145,145)); }
                else g.setColor(new Color(28,126,52));
                g.setStroke(new BasicStroke(5)); g.drawLine(8,20,17,29); g.drawLine(17,29,35,7);
            }
            case SAVE -> {
                g.setColor(new Color(119,43,145));g.fillRoundRect(3,2,34,36,3,3);
                g.setColor(Color.WHITE);g.fillRect(10,3,18,12);g.fillRect(10,23,20,12);
                g.setColor(new Color(119,43,145));g.fillRect(22,4,4,9);
            }
            case SIGN -> {
                g.setColor(new Color(218,160,32));g.fillPolygon(new int[]{3,20,24,7},new int[]{29,12,17,34},4);
                g.setColor(new Color(40,102,47));g.fillPolygon(new int[]{19,31,36,24},new int[]{12,1,6,18},4);
                g.setColor(new Color(45,45,45));g.fillPolygon(new int[]{2,3,8},new int[]{37,28,34},3);
                g.setColor(new Color(233,180,38));g.fillRect(12,29,25,4);g.fillRect(12,35,25,3);
            }
            case MENU -> {g.setColor(new Color(0,63,169));for(int i=7;i<36;i+=11)g.fillRoundRect(3,i,34,5,3,3);}
            case PRINT -> {
                g.setColor(new Color(130,139,145));g.fillRoundRect(1,14,38,18,3,3);
                g.setColor(Color.WHITE);g.fillRect(10,1,21,17);g.fillRect(10,26,21,13);
                g.setColor(new Color(105,125,137));g.drawRect(10,1,21,17);g.drawRect(10,26,21,13);
                g.setColor(new Color(25,140,192));g.fillRect(13,29,15,2);g.fillRect(13,34,12,2);
            }
            case PDF -> {
                g.setColor(Color.WHITE);g.fillPolygon(new int[]{10,27,35,35,10},new int[]{1,1,9,37,37},5);
                g.setColor(new Color(110,110,110));g.drawPolygon(new int[]{10,27,35,35,10},new int[]{1,1,9,37,37},5);
                g.setColor(new Color(42,125,72));g.drawLine(1,13,24,13);g.drawLine(18,7,24,13);g.drawLine(18,19,24,13);
                g.setColor(new Color(120,48,144));g.fillRect(24,23,14,14);g.setColor(Color.WHITE);g.drawRect(28,26,6,7);
            }
        } g.dispose();
    }
}
