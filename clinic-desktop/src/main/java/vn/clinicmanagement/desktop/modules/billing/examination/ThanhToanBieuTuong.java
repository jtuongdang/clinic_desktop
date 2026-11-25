package vn.clinicmanagement.desktop.modules.billing.examination;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

record ThanhToanBieuTuong(Kind kind,int size) implements Icon {
    enum Kind {MENU,PRINT,REFRESH,DOCUMENT,XML,INSURANCE,SEARCH,IMAGE,SHIELD,COINS,CART,GIFT,CANCEL,COPY,SWAP,SPLIT,MERGE,PERCENT,TAG,CALCULATOR,EDIT,HISTORY,INFO,BARCODE,PREVIOUS,NEXT,CHECK}
    public int getIconWidth(){return size;}public int getIconHeight(){return size;}
    public void paintIcon(Component c,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.scale(size/32.0,size/32.0);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(0,112,214));g.setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(kind){
            case MENU->{for(int k=6;k<=26;k+=10)g.fillRect(1,k,29,3);}
            case PRINT->{g.setColor(new Color(80,90,100));g.fillRoundRect(1,12,30,15,3,3);g.setColor(Color.WHITE);g.fillRect(7,2,18,13);g.setColor(Color.GRAY);g.drawRect(7,2,18,13);g.setColor(Color.WHITE);g.fillRect(7,22,18,9);g.setColor(Color.GRAY);g.drawRect(7,22,18,9);g.setColor(new Color(0,145,220));g.fillOval(25,16,3,3);}
            case REFRESH->{g.draw(new Arc2D.Double(4,3,24,25,30,295,Arc2D.OPEN));g.fillPolygon(new int[]{22,31,29},new int[]{8,8,0},3);}
            case SEARCH->{g.drawOval(12,2,17,17);g.setStroke(new BasicStroke(4));g.drawLine(12,19,2,29);}
            case DOCUMENT->{g.setColor(Color.DARK_GRAY);g.drawRoundRect(2,3,10,20,2,2);g.drawRoundRect(20,3,10,20,2,2);g.drawLine(16,6,16,30);g.drawLine(6,27,26,27);for(int k=7;k<20;k+=5){g.drawLine(5,k,9,k);g.drawLine(23,k,27,k);}}
            case XML->{g.setColor(new Color(127,215,255));g.fillRect(6,1,20,29);g.setColor(new Color(0,110,230));g.drawRect(6,1,20,29);g.setFont(vn.clinicmanagement.desktop.core.config.UngDungPhongChu.TINY.deriveFont(8f));g.drawString("XML",7,19);}
            case INSURANCE->{g.setColor(new Color(130,216,247));g.fillRoundRect(1,6,29,21,3,3);g.setColor(new Color(0,90,170));g.drawRoundRect(1,6,29,21,3,3);g.fillRect(4,10,8,7);g.drawLine(16,11,27,11);g.drawLine(16,16,27,16);g.setColor(new Color(0,130,85));g.fillOval(18,18,14,14);g.setColor(Color.WHITE);g.drawLine(21,25,24,28);g.drawLine(24,28,29,22);}
            case IMAGE->{g.drawRect(2,4,28,24);g.fillPolygon(new int[]{4,13,19,26,29,4},new int[]{25,13,19,10,25,25},6);g.fillOval(6,7,5,5);}
            case SHIELD->{g.fillPolygon(new int[]{16,29,27,21,16,11,5,3},new int[]{1,6,18,27,31,27,18,6},8);g.setColor(Color.WHITE);g.drawLine(16,4,16,26);g.drawLine(6,11,26,11);}
            case COINS->{g.setColor(new Color(235,157,0));for(int k=25;k>4;k-=7){g.fillOval(3,k-4,26,8);g.drawLine(3,k-3,3,k+1);g.drawLine(29,k-3,29,k+1);}g.setColor(new Color(255,223,60));g.drawOval(3,1,26,8);}
            case CART->{g.drawLine(1,3,6,3);g.drawLine(6,3,10,23);g.drawLine(10,23,28,23);g.fillPolygon(new int[]{7,31,27,10},new int[]{7,7,19,19},4);g.fillOval(11,27,4,4);g.fillOval(25,27,4,4);}
            case GIFT->{g.fillRect(3,13,26,18);g.fillRect(1,9,30,6);g.drawOval(5,1,10,8);g.drawOval(16,1,10,8);g.setColor(Color.WHITE);g.drawLine(16,10,16,31);g.drawLine(3,16,29,16);}
            case CANCEL->{g.setColor(new Color(217,0,20));g.fillOval(1,1,30,30);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(4));g.drawLine(9,9,23,23);g.drawLine(23,9,9,23);}
            case COPY->{g.drawRect(3,9,19,21);g.drawRect(11,2,18,21);}
            case SWAP->{g.drawLine(2,10,29,10);g.drawLine(3,22,30,22);g.drawLine(23,4,29,10);g.drawLine(23,16,29,10);g.drawLine(9,16,3,22);g.drawLine(9,28,3,22);}
            case SPLIT->{g.drawRect(6,2,6,28);g.drawRect(21,2,6,28);}
            case MERGE->{g.drawRoundRect(2,10,17,12,5,5);g.drawRoundRect(14,4,16,12,5,5);g.drawLine(11,20,22,10);}
            case PERCENT->{g.setColor(Color.DARK_GRAY);g.drawOval(3,2,8,9);g.drawOval(21,21,8,9);g.drawLine(4,29,28,2);}
            case TAG->{g.setColor(new Color(239,160,0));g.fillPolygon(new int[]{2,15,29,18,10},new int[]{16,3,13,29,29},5);g.setColor(Color.WHITE);g.fillOval(14,6,3,3);}
            case CALCULATOR->{g.drawRect(5,1,22,30);g.drawRect(9,5,14,5);for(int a=9;a<=23;a+=7)for(int b=15;b<=27;b+=6)g.fillRect(a,b,3,3);}
            case EDIT->{g.setStroke(new BasicStroke(6));g.drawLine(7,25,25,6);g.setColor(Color.DARK_GRAY);g.fillPolygon(new int[]{3,5,10},new int[]{30,22,28},3);}
            case HISTORY->{g.drawOval(6,3,24,25);g.drawLine(18,7,18,17);g.drawLine(18,17,25,19);g.drawLine(1,15,9,15);g.drawLine(1,15,4,8);}
            case INFO->{g.fillOval(1,1,30,30);g.setColor(Color.WHITE);g.fillOval(14,6,4,4);g.fillRect(14,13,4,12);}
            case BARCODE->{g.setColor(Color.DARK_GRAY);for(int k=2;k<31;k+=3)g.fillRect(k,2,k%2==0?2:1,28);}
            case PREVIOUS,NEXT->{g.setColor(new Color(173,173,173));if(kind==Kind.PREVIOUS)g.fillPolygon(new int[]{23,7,23},new int[]{2,16,30},3);else g.fillPolygon(new int[]{8,24,8},new int[]{2,16,30},3);}
            case CHECK->{g.setColor(new Color(0,140,153));g.fillOval(1,1,30,30);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(4));g.drawLine(7,15,13,22);g.drawLine(13,22,25,9);}
        }g.dispose();
    }
}
