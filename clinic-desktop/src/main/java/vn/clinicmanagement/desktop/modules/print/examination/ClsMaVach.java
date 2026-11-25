package vn.clinicmanagement.desktop.modules.print.examination;


import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** Code 39 with start/stop symbols and 10-module quiet zones. Unsupported PIDs are left as text only. */
public final class ClsMaVach {
    /* Character patterns from ZXing Code39Reader, Copyright 2008 ZXing authors.
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
     * Unless required by applicable law or agreed to in writing, software distributed
     * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
     * CONDITIONS OF ANY KIND, either express or implied. See the License for the
     * specific language governing permissions and limitations under the License.
     * https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/oned/Code39Reader.java
     */
    private static final String ALPHABET="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%";
    private static final int[] PATTERNS={0x034,0x121,0x061,0x160,0x031,0x130,0x070,0x025,0x124,0x064,
            0x109,0x049,0x148,0x019,0x118,0x058,0x00D,0x10C,0x04C,0x01C,0x103,0x043,0x142,0x013,0x112,0x052,0x007,0x106,0x046,0x016,
            0x181,0x0C1,0x1C0,0x091,0x190,0x0D0,0x085,0x184,0x0C4,0x0A8,0x0A2,0x08A,0x02A};
    public static boolean[] encode(String value){
        if(value==null||value.isBlank())return new boolean[0];
        for(char c:value.toCharArray())if(ALPHABET.indexOf(c)<0)return new boolean[0];
        String symbols="*"+value+"*";boolean[] modules=new boolean[20+symbols.length()*13-1];int at=10;
        for(int i=0;i<symbols.length();i++){
            char c=symbols.charAt(i);int pattern=c=='*'?0x094:PATTERNS[ALPHABET.indexOf(c)];
            for(int bit=8;bit>=0;bit--){int width=(pattern&(1<<bit))==0?1:2;for(int j=0;j<width;j++)modules[at++]=(bit%2==0);}
            if(i<symbols.length()-1)at++;
        }return modules;
    }
    public static void draw(Graphics2D g,String value,int x,int y,int width,int height){boolean[] modules=encode(value);if(modules.length==0)return;double unit=width/(double)modules.length;for(int i=0;i<modules.length;i++)if(modules[i])g.fill(new Rectangle2D.Double(x+i*unit,y,unit,height));}
}
