package vn.clinicmanagement.desktop.modules.reports.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

public record BaoCaoBieuTuong(Kind kind,int size) implements Icon {
    public enum Kind {MENU,REFRESH,ADD,FETCH,LIST,UP,DOWN,SAVE,SEARCH}
    @Override public int getIconWidth(){return size;}
    @Override public int getIconHeight(){return size;}
    @Override public void paintIcon(Component c,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(size/40.0,size/40.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.5f));g.setColor(new Color(0,116,206));
        switch(kind){
            case MENU -> {for(int i=8;i<35;i+=10)g.fillRect(3,i,34,4);}
            case ADD -> {g.fillRect(17,3,6,34);g.fillRect(3,17,34,6);}
            case REFRESH -> {g.draw(new Arc2D.Double(6,5,28,29,35,300,Arc2D.OPEN));g.fillPolygon(new int[]{27,37,37},new int[]{12,12,2},3);}
            case LIST -> {for(int i=7;i<36;i+=11){g.fillRect(3,i,3,3);g.fillRect(10,i,27,3);}}
            case UP,DOWN -> {
                if(kind==Kind.DOWN){g.translate(40,40);g.rotate(Math.PI);}
                g.fillPolygon(new int[]{2,20,38,26,26,14,14},new int[]{24,5,24,24,36,36,24},7);
            }
            case SAVE -> {g.setColor(new Color(96,158,141));g.fillRect(0,0,40,40);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(5));g.drawLine(8,21,17,30);g.drawLine(17,30,33,10);}
            case SEARCH -> {g.setColor(new Color(0,103,204));g.drawOval(14,3,21,21);g.setStroke(new BasicStroke(4));g.drawLine(15,23,3,36);}
            case FETCH -> {
                g.setColor(new Color(55,55,55));g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1,2,16,23,3,3);g.drawRoundRect(23,2,16,23,3,3);g.drawRoundRect(12,17,16,22,3,3);
                g.drawLine(17,13,23,13);g.drawLine(20,13,20,17);
            }
        }g.dispose();
    }
}
