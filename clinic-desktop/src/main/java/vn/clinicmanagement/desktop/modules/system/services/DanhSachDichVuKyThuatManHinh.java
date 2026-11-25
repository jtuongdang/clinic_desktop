package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/** Technical-service catalog with independent hierarchy, search and display-order state. */
public final class DanhSachDichVuKyThuatManHinh extends CoSoManHinhManHinh {
    private record TreeItem(String title,String category,String group){@Override public String toString(){return title;}}
    private final DichVuDanhMucNguon source;
    private final DichVuThuTuLuuTru orderStore;
    private final DichVuDanhMucMoHinh model=new DichVuDanhMucMoHinh();
    private final DichVuDanhMucBoCuc ui;
    @FunctionalInterface public interface Editor { void open(DichVuHang item,java.util.function.Consumer<vn.clinicmanagement.application.catalog.DichVuCauHinh> saved); }
    private final Editor editor;
    private String category="",group="";
    private boolean rebuilding,dirty,loading,saving;
    private SwingWorker<List<DichVuHang>,Void> loader;
    private long revision;

    public DanhSachDichVuKyThuatManHinh(){this(DichVuDanhMucNguon.production(),new DichVuThuTuLuuTru());}
    public DanhSachDichVuKyThuatManHinh(DichVuDanhMucNguon source,DichVuThuTuLuuTru store){
        this(source,store,null);
    }
    public DanhSachDichVuKyThuatManHinh(DichVuDanhMucNguon source,DichVuThuTuLuuTru store,Editor editor){
        super("system-service-list","Danh Sách Dịch Vụ Kỹ Thuật","");this.source=Objects.requireNonNull(source);orderStore=Objects.requireNonNull(store);
        this.editor=editor!=null?editor:(item,saved)->{
            if(!GraphicsEnvironment.isHeadless())new DichVuCauHinhHopThoai(SwingUtilities.getWindowAncestor(this),item,source,saved).setVisible(true);
        };
        ui=new DichVuDanhMucBoCuc(this,model);bind();rebuildTree();
    }
    @Override public void doLayout(){if(ui!=null)ui.resize(this);super.doLayout();}
    private void bind(){
        ui.keyword.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e){filter();}
            @Override public void removeUpdate(DocumentEvent e){filter();}
            @Override public void changedUpdate(DocumentEvent e){filter();}
        });
        ui.search.addActionListener(e->filter());ui.keyword.addActionListener(e->filter());ui.refresh.addActionListener(e->refreshData());
        ui.tree.addTreeSelectionListener(e->{
            if(rebuilding)return;var node=(DefaultMutableTreeNode)ui.tree.getLastSelectedPathComponent();
            if(node!=null&&node.getUserObject() instanceof TreeItem item){category=item.category();group=item.group();filter();}
        });
        ui.table.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                if(!SwingUtilities.isLeftMouseButton(e))return;
                int index=ui.table.rowAtPoint(e.getPoint());if(index<0)return;
                ui.table.setRowSelectionInterval(index,index);
                var row=model.row(index);
                if(row.kind()!=DichVuDanhMucMoHinh.Kind.ITEM&&(e.getClickCount()==2||e.getX()<Math.round((29+row.depth()*24)*ui.scale())))model.toggle(index);
                else if(e.getClickCount()==2)showDetails();
            }
        });
        ui.table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"editService");
        ui.table.getActionMap().put("editService",new AbstractAction(){public void actionPerformed(ActionEvent e){showDetails();}});
        ui.up.addActionListener(e->move(-1));ui.down.addActionListener(e->move(1));ui.save.addActionListener(e->saveOrder());
        JPopupMenu menu=new JPopupMenu();
        JMenuItem detail=new JMenuItem("Thông tin dịch vụ");detail.addActionListener(e->showDetails());menu.add(detail);
        JMenuItem expand=new JMenuItem("Mở rộng tất cả nhóm");expand.addActionListener(e->{model.expandAll();expandTree();});menu.add(expand);
        JMenuItem all=new JMenuItem("Tất cả dịch vụ");all.addActionListener(e->{ui.tree.setSelectionRow(0);ui.keyword.setText("");});menu.add(all);
        JMenuItem close=new JMenuItem("Đóng");close.addActionListener(e->{var window=SwingUtilities.getWindowAncestor(this);if(window instanceof DichVuDanhMucHopThoai)window.dispose();});menu.add(close);
        ui.menu.addActionListener(e->menu.show(ui.menu,0,ui.menu.getHeight()));
        shortcut("control F","find",()->ui.keyword.requestFocusInWindow());shortcut("meta F","findMac",()->ui.keyword.requestFocusInWindow());
        shortcut("F5","refresh",this::refreshData);shortcut("control S","save",this::saveOrder);
    }
    private void shortcut(String key,String name,Runnable task){getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key),name);getActionMap().put(name,new AbstractAction(){@Override public void actionPerformed(ActionEvent e){task.run();}});}
    private void filter(){model.filter(ui.keyword.getText(),category,group);}
    private void rebuildTree(){
        rebuilding=true;var root=new DefaultMutableTreeNode(new TreeItem("Tất Cả Dịch Vụ ("+model.entries().size()+")","",""));
        Map<String,Map<String,Long>> groups=new LinkedHashMap<>();
        for(var entry:model.entries())groups.computeIfAbsent(entry.category(),ignored->new LinkedHashMap<>()).merge(entry.group(),1L,Long::sum);
        groups.forEach((category,names)->{
            long total=names.values().stream().mapToLong(Long::longValue).sum();
            var parent=new DefaultMutableTreeNode(new TreeItem(category+" ("+total+")",category,""));root.add(parent);
            names.forEach((group,count)->parent.add(new DefaultMutableTreeNode(new TreeItem(group+" ("+count+")",category,group))));
        });
        ui.tree.setModel(new DefaultTreeModel(root));expandTree();ui.tree.setSelectionRow(0);category="";group="";rebuilding=false;filter();
    }
    private void expandTree(){for(int i=0;i<ui.tree.getRowCount();i++)ui.tree.expandRow(i);}
    public void move(int direction){
        if(loading)return;int index=ui.table.getSelectedRow();if(index<0)return;String key=model.row(index).key();
        if(model.move(key,direction)){dirty=true;revision++;rebuildTree();int row=model.indexOf(key);if(row>=0)ui.table.setRowSelectionInterval(row,row);message("Có thay đổi thứ tự chưa lưu.");}
    }
    @Override public void refreshData(){
        if(loading)return;
        if(dirty&&(GraphicsEnvironment.isHeadless()||JOptionPane.showConfirmDialog(this,"Làm mới sẽ bỏ thứ tự chưa lưu. Tiếp tục?","Làm mới",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))return;
        loading=true;ui.refresh.setEnabled(false);message("Đang tải danh mục dịch vụ...");
        loader=new SwingWorker<>(){
            @Override protected List<DichVuHang> doInBackground()throws Exception{return orderStore.apply(source.load());}
            @Override protected void done(){
                loading=false;ui.refresh.setEnabled(true);if(isCancelled())return;
                try{model.replace(get());dirty=false;revision++;rebuildTree();message(" ");}
                catch(Exception ex){model.replace(List.of());rebuildTree();message("Không tải được danh mục dịch vụ. Kiểm tra phiên làm việc và kết nối rồi nhấn F5.");}
            }
        };loader.execute();
    }
    public void saveOrder(){
        if(loading||saving)return;var snapshot=model.entries();long version=revision;saving=true;
        ui.save.setEnabled(false);
        new SwingWorker<Boolean,Void>(){
            @Override protected Boolean doInBackground()throws Exception{orderStore.save(snapshot);return true;}
            @Override protected void done(){
                saving=false;ui.save.setEnabled(true);
                try{get();if(version==revision)dirty=false;message(dirty?"Đã lưu phiên bản trước; còn thay đổi mới chưa lưu.":"Đã lưu thứ tự hiển thị trên máy này.");}
                catch(Exception ex){message("Không lưu được thứ tự. Các thay đổi vẫn được giữ trên màn hình.");}
            }
        }.execute();
    }
    private void showDetails(){
        int index=ui.table.getSelectedRow();if(index<0)return;var row=model.row(index);
        if(row.item()==null){model.toggle(index);return;}
        editor.open(row.item(),updated->{
            model.replace(model.entries().stream().map(item->item.id()==updated.id()?DichVuHang.from(updated):item).toList());revision++;
            rebuildTree();int selected=model.indexOf(String.valueOf(updated.id()));if(selected>=0)ui.table.setRowSelectionInterval(selected,selected);
            message("Đã cập nhật dịch vụ "+updated.value("code")+".");
        });
    }
    private void message(String value){ui.status.setText(value);revalidate();}
    @Override public void onHide(){if(loader!=null)loader.cancel(true);loading=false;}
    @Override public boolean canClose(){return !dirty||(!GraphicsEnvironment.isHeadless()&&JOptionPane.showConfirmDialog(this,"Đóng mà chưa lưu thứ tự mới?","Danh sách dịch vụ",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);}
    public DichVuDanhMucBoCuc getCatalogUi(){return ui;}
    public DichVuDanhMucMoHinh getCatalogModel(){return model;}
}
