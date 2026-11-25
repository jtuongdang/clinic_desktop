package vn.clinicmanagement.desktop.modules.system.services;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Explicitly saved display order on this workstation; does not change service prices. */
public final class DichVuThuTuLuuTru {
    private final Path path;
    public DichVuThuTuLuuTru(){this(Path.of(System.getProperty("user.home"),".clinic-management","service-order.xml"));}
    public DichVuThuTuLuuTru(Path path){this.path=path;}
    public List<DichVuHang> apply(List<DichVuHang> items)throws IOException {
        if(!Files.exists(path))return List.copyOf(items);
        Properties p=new Properties();try(var input=Files.newInputStream(path)){p.loadFromXML(input);}
        List<DichVuHang> sorted=new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(item->{try{return Integer.parseInt(p.getProperty(item.key(),"2147483647"));}catch(NumberFormatException ex){return Integer.MAX_VALUE;}}));return sorted;
    }
    public void save(List<DichVuHang> items)throws IOException {
        Properties p=new Properties();for(int i=0;i<items.size();i++)p.setProperty(items.get(i).key(),String.valueOf(i));
        Path parent=path.toAbsolutePath().getParent();Files.createDirectories(parent);Path temp=Files.createTempFile(parent,"service-order-",".xml");
        try{
            try(var output=Files.newOutputStream(temp)){p.storeToXML(output,"Thứ tự hiển thị danh mục dịch vụ trên máy này","UTF-8");}
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
