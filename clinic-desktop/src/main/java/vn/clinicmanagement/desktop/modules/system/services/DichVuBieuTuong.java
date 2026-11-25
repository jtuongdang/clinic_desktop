package vn.clinicmanagement.desktop.modules.system.services;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

public record DichVuBieuTuong(Kind kind,int size)implements Icon {
    public enum Kind {MENU,PRICE,REFRESH,ADD,MAP,UP,DOWN,SAVE,SEARCH}
    @Override public int getIconWidth(){return size;}
    @Override public int getIconHeight(){return size;}
    @Override public void paintIcon(Component component,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(size/40.0,size/40.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setStroke(new BasicStroke(2.5f));g.setColor(new Color(0,99,188));
        switch(kind){
            case MENU->{for(int i=7;i<35;i+=11){g.setColor(new Color(0,48,113));g.fillRoundRect(2,i,36,5,1,1);g.setColor(new Color(20,110,210));g.fillRect(3,i+1,34,2);}}
            case REFRESH->{g.setColor(new Color(0,156,202));g.draw(new Arc2D.Double(6,5,28,29,35,300,Arc2D.OPEN));g.fillPolygon(new int[]{27,37,37},new int[]{12,12,2},3);}
            case ADD->{g.setColor(new Color(0,37,92));g.fillRect(17,3,6,34);g.fillRect(3,17,34,6);g.setColor(new Color(16,90,182));g.fillRect(19,4,2,32);}
            case MAP->{g.setColor(new Color(25,25,25));g.setStroke(new BasicStroke(1.5f));g.drawRect(25,1,12,12);g.drawRect(1,27,12,12);g.drawRect(25,27,12,12);g.drawLine(31,13,31,20);g.drawLine(7,20,31,20);g.drawLine(7,20,7,27);g.drawLine(31,20,31,27);}
            case UP,DOWN->{if(kind==Kind.DOWN){g.translate(40,40);g.rotate(Math.PI);}Polygon p=new Polygon(new int[]{2,20,38,26,26,14,14},new int[]{24,5,24,24,36,36,24},7);g.setPaint(new GradientPaint(0,0,new Color(43,156,241),0,38,new Color(0,70,166)));g.fill(p);g.setColor(new Color(0,39,99));g.draw(p);}
            case SAVE->{g.setPaint(new GradientPaint(0,0,new Color(0,114,98),40,40,new Color(93,187,153)));g.fillRect(0,0,40,40);g.setColor(new Color(0,90,76));g.drawRect(0,0,39,39);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(5));g.drawLine(7,20,17,30);g.drawLine(17,30,34,9);}
            case SEARCH->{g.setColor(new Color(0,60,112));g.drawOval(14,3,21,21);g.setStroke(new BasicStroke(4));g.drawLine(15,23,3,36);}
            case PRICE->{g.setColor(Color.WHITE);g.fillRoundRect(8,4,25,34,3,3);g.setColor(new Color(0,35,36));g.drawRoundRect(8,4,25,34,3,3);g.fillRect(15,1,11,7);g.setColor(new Color(171,0,0));g.fillOval(14,14,7,7);g.setColor(Color.GRAY);g.drawLine(24,15,29,15);g.drawLine(24,21,29,21);g.drawLine(14,27,29,27);}
        }g.dispose();
    }
}
