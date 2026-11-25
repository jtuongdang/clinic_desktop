package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.modules.reports.catalog.*;
import vn.clinicmanagement.desktop.modules.reports.components.BaoCaoDanhMucBoCuc;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Report-template catalog. Numerical statistics remain available through the existing report view. */
public final class DanhMucMauBaoCaoManHinh extends CoSoManHinhManHinh {
    private record TreeItem(String title,String category,String group){@Override public String toString(){return title;}}
    private static final Set<String> EXTENSIONS=Set.of("doc","docx","xls","xlsx","pdf","csv");
    private final BaoCaoDanhMucLuuTru store;
    private final BaoCaoDanhMucMoHinh model=new BaoCaoDanhMucMoHinh();
    private final BaoCaoDanhMucBoCuc ui;
    private final Runnable statisticsOpener;
    private Consumer<BaoCaoMau> reportOpener;
    public void setReportOpener(Consumer<BaoCaoMau> opener){this.reportOpener=opener;}
    private String category="",group="";
    private boolean buildingTree,dirty,busy;
    private long revision;
    private final List<SwingWorker<?,?>> workers=new ArrayList<>();

    public DanhMucMauBaoCaoManHinh(){this(new BaoCaoDanhMucLuuTru(),null);}
    public DanhMucMauBaoCaoManHinh(Runnable statisticsOpener){this(new BaoCaoDanhMucLuuTru(),statisticsOpener);}
    public DanhMucMauBaoCaoManHinh(BaoCaoDanhMucLuuTru store,Runnable statisticsOpener){
        super("report-templates","Danh Mục Mẫu Báo Cáo","");this.store=Objects.requireNonNull(store);this.statisticsOpener=statisticsOpener;
        ui=new BaoCaoDanhMucBoCuc(this,model);
        bindActions();
        try{model.replace(store.load());rebuildTree();}catch(Exception ex){ui.status.setText("Không đọc được danh mục báo cáo trên máy này.");}
    }
    @Override public void doLayout(){if(ui!=null)ui.resize(this);super.doLayout();}
    private void bindActions(){
        ui.keyword.getDocument().addDocumentListener(new DocumentListener(){
            @Override public void insertUpdate(DocumentEvent e){applyFilter();}
            @Override public void removeUpdate(DocumentEvent e){applyFilter();}
            @Override public void changedUpdate(DocumentEvent e){applyFilter();}
        });
        ui.search.addActionListener(e->applyFilter());ui.keyword.addActionListener(e->applyFilter());
        ui.tree.addTreeSelectionListener(e->{
            if(buildingTree)return;
            var node=(DefaultMutableTreeNode)ui.tree.getLastSelectedPathComponent();
            if(node!=null&&node.getUserObject() instanceof TreeItem item){category=item.category();group=item.group();applyFilter();
                if(!group.isBlank()&&node.isLeaf()){
                    var matches=model.entries().stream().filter(r->r.category().equals(category)&&r.group().equals(group)).toList();
                    if(matches.size()==1&&reportOpener!=null){var report=matches.get(0);int row=model.indexOf(report.key());if(row>=0)ui.table.setRowSelectionInterval(row,row);reportOpener.accept(report);}
                }}
        });
        ui.table.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                if(!SwingUtilities.isLeftMouseButton(e))return;
                int index=ui.table.rowAtPoint(e.getPoint());if(index<0)return;
                var row=model.row(index);
                if(row.kind()!=BaoCaoDanhMucMoHinh.Kind.REPORT&&(e.getClickCount()==2||e.getX()<Math.round((23+row.depth()*22)*ui.scale())))model.toggle(index);
                else if(row.kind()==BaoCaoDanhMucMoHinh.Kind.REPORT&&e.getClickCount()==1)openSelectedTemplate();
            }
        });
        ui.table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"openReport");
        ui.table.getActionMap().put("openReport",new AbstractAction(){@Override public void actionPerformed(ActionEvent e){openSelectedTemplate();}});
        ui.refresh.addActionListener(e->refreshData());ui.up.addActionListener(e->moveSelected(-1));ui.down.addActionListener(e->moveSelected(1));
        ui.save.addActionListener(e->saveOrder());ui.fetch.addActionListener(e->chooseDirectory());ui.add.addActionListener(e->addTemplate());
        JPopupMenu extended=new JPopupMenu();
        JMenuItem statistics=new JMenuItem("Báo cáo thống kê lượt khám, doanh thu và kho");statistics.setEnabled(statisticsOpener!=null);
        statistics.addActionListener(e->{if(statisticsOpener!=null)statisticsOpener.run();});extended.add(statistics);
        JMenuItem expand=new JMenuItem("Mở rộng toàn bộ danh mục");expand.addActionListener(e->{model.expandAll();expandTree();});extended.add(expand);
        ui.extended.addActionListener(e->extended.show(ui.extended,0,ui.extended.getHeight()));
        JPopupMenu menu=new JPopupMenu();
        JMenuItem open=new JMenuItem("Mở tệp mẫu đã chọn");open.addActionListener(e->openLinkedTemplateFile());menu.add(open);
        JMenuItem link=new JMenuItem("Liên kết tệp mẫu...");link.addActionListener(e->linkSelected());menu.add(link);
        JMenuItem all=new JMenuItem("Tất cả báo cáo");all.addActionListener(e->{ui.tree.setSelectionRow(0);ui.keyword.setText("");});menu.add(all);
        ui.menu.addActionListener(e->menu.show(ui.menu,0,ui.menu.getHeight()));
        shortcut("F5","refresh",this::refreshData);shortcut("control F","search",()->ui.keyword.requestFocusInWindow());shortcut("control S","save",this::saveOrder);
    }
    private void shortcut(String key,String name,Runnable task){
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key),name);
        getActionMap().put(name,new AbstractAction(){@Override public void actionPerformed(ActionEvent e){task.run();}});
    }
    private void applyFilter(){model.filter(ui.keyword.getText(),category,group);}
    private void rebuildTree(){
        buildingTree=true;
        var root=new DefaultMutableTreeNode(new TreeItem("Tất Cả Báo Cáo ("+model.entries().size()+")","",""));
        Map<String,Map<String,Long>> groups=new LinkedHashMap<>();
        for(var e:model.entries())groups.computeIfAbsent(e.category(),ignored->new LinkedHashMap<>()).merge(e.group(),1L,Long::sum);
        groups.forEach((cat,items)->{
            long count=items.values().stream().mapToLong(Long::longValue).sum();
            var parent=new DefaultMutableTreeNode(new TreeItem(cat+" ("+count+")",cat,""));root.add(parent);
            items.forEach((name,total)->parent.add(new DefaultMutableTreeNode(new TreeItem(name+" ("+total+")",cat,name))));
        });
        ui.tree.setModel(new DefaultTreeModel(root));expandTree();ui.tree.setSelectionRow(0);
        category="";group="";buildingTree=false;applyFilter();
    }
    private void expandTree(){for(int row=0;row<ui.tree.getRowCount();row++)ui.tree.expandRow(row);}
    private void changed(){dirty=true;revision++;ui.status.setText("Có thay đổi chưa lưu.");}
    private BaoCaoDanhMucMoHinh.Row selected(){int row=ui.table.getSelectedRow();return row>=0&&row<model.getRowCount()?model.row(row):null;}
    public void moveSelected(int direction){
        if(busy)return;var item=selected();if(item==null)return;
        if(model.move(item.key(),direction)){
            changed();rebuildTree();int row=model.indexOf(item.key());if(row>=0)ui.table.setRowSelectionInterval(row,row);
        }
    }
    public void saveOrder(){
        if(busy)return;var snapshot=model.entries();long version=revision;
        runBackground(()->{store.save(snapshot);return Boolean.TRUE;},ignored->{
            if(revision==version)dirty=false;ui.status.setText(dirty?"Đã lưu phiên bản trước; còn thay đổi mới chưa lưu.":"Đã lưu danh mục và thứ tự sắp xếp trên máy này.");
        },"Không lưu được danh mục. Các thay đổi vẫn được giữ trên màn hình.");
    }
    @Override public void refreshData(){
        if(busy)return;
        if(dirty&&(GraphicsEnvironment.isHeadless()||JOptionPane.showConfirmDialog(this,"Tải lại sẽ bỏ các thay đổi chưa lưu. Tiếp tục?","Làm mới danh mục",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION))return;
        runBackground(store::load,entries->{model.replace(entries);dirty=false;revision++;rebuildTree();ui.status.setText(" ");},"Không tải lại được danh mục báo cáo.");
    }
    private void chooseDirectory(){
        if(busy||GraphicsEnvironment.isHeadless())return;
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Chọn thư mục chứa mẫu báo cáo");chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path directory=chooser.getSelectedFile().toPath();
        runBackground(()->{
            try(Stream<Path> files=Files.list(directory)){return files.filter(Files::isRegularFile).filter(DanhMucMauBaoCaoManHinh::supported).sorted().toList();}
        },files->{
            int count=0;
            for(Path path:files){
                String file=path.getFileName().toString();var existing=model.entries().stream().filter(e->e.fileName().equalsIgnoreCase(file)).toList();
                if(!existing.isEmpty())for(var match:existing)model.attach(match.key(),path.toAbsolutePath().toString(),file);
                else{String name=file.replaceFirst("\\.[^.]+$","");model.add(new BaoCaoMau(UUID.randomUUID().toString(),"Mẫu báo cáo đã liên kết",name,name,file,"",path.toAbsolutePath().toString()));}
                count++;
            }
            if(count>0){changed();rebuildTree();}ui.status.setText("Đã đọc "+count+" tệp mẫu. Nhấn Lưu Thứ Tự Sắp Xếp để lưu danh mục.");
        },"Không đọc được thư mục mẫu báo cáo.");
    }
    private void addTemplate(){
        if(busy||GraphicsEnvironment.isHeadless())return;
        JTextField categoryField=new JTextField(category.isBlank()?"Điện tiếp (phòng khám)":category),groupField=new JTextField(group),nameField=new JTextField(),idField=new JTextField(),fileField=new JTextField();
        JPanel fields=new JPanel(new GridLayout(5,2,8,8));
        for(Object[] field:new Object[][]{{"Danh mục",categoryField},{"Nhóm báo cáo",groupField},{"Tên báo cáo",nameField},{"ID",idField},{"Đường dẫn tệp mẫu",fileField}}){fields.add(new JLabel((String)field[0]));fields.add((Component)field[1]);}
        if(JOptionPane.showConfirmDialog(this,fields,"Thêm mẫu báo cáo",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{
            String path=fileField.getText().strip();String filename="";
            if(!path.isBlank()){Path file=Path.of(path).toAbsolutePath();if(!Files.isRegularFile(file)||!supported(file))throw new IllegalArgumentException("Chọn tệp Word, Excel, PDF hoặc CSV đang có trên máy.");filename=file.getFileName().toString();path=file.toString();}
            model.add(new BaoCaoMau(UUID.randomUUID().toString(),categoryField.getText().strip(),groupField.getText().strip(),nameField.getText().strip(),filename,idField.getText().strip(),path));
            changed();rebuildTree();
        }catch(IllegalArgumentException ex){ui.status.setText(ex.getMessage());}
    }
    private void linkSelected(){var row=selected();if(row!=null&&row.report()!=null)chooseFile(row.report(),false);}
    private void openSelectedTemplate(){
        var row=selected();if(row==null)return;
        if(row.report()==null){int index=ui.table.getSelectedRow();model.toggle(index);return;}
        var report=row.report();
        if(reportOpener!=null){reportOpener.accept(report);return;}
        if(!report.localPath().isBlank()&&Files.isRegularFile(Path.of(report.localPath()))){openFile(Path.of(report.localPath()));return;}
        ui.status.setText("Chọn loại phiếu trong màn Quản lý phiếu để xem dữ liệu.");
    }
    private void openLinkedTemplateFile(){
        var row=selected();if(row==null||row.report()==null)return;
        String path=row.report().localPath();
        if(path.isBlank()||!Files.isRegularFile(Path.of(path))){ui.status.setText("Chưa có tệp mẫu trên máy. Chọn Menu → Liên kết tệp mẫu để bổ sung.");return;}
        openFile(Path.of(path));
    }
    private void chooseFile(BaoCaoMau report,boolean open){
        if(GraphicsEnvironment.isHeadless())return;
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Liên kết tệp mẫu: "+report.name());
        chooser.setFileFilter(new FileNameExtensionFilter("Mẫu báo cáo Word, Excel, PDF, CSV","doc","docx","xls","xlsx","pdf","csv"));
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath().toAbsolutePath();if(!Files.isRegularFile(path)||!supported(path)){ui.status.setText("Tệp mẫu không được hỗ trợ.");return;}
        model.attach(report.key(),path.toString(),path.getFileName().toString());changed();if(open)openFile(path);
    }
    private void openFile(Path file){
        if(!supported(file)||!Desktop.isDesktopSupported()){ui.status.setText("Không mở được tệp mẫu trên hệ thống này.");return;}
        runBackground(()->{Desktop.getDesktop().open(file.toFile());return Boolean.TRUE;},ignored->{},"Không mở được tệp mẫu bằng ứng dụng mặc định.");
    }
    private static boolean supported(Path file){String name=file.getFileName().toString();int dot=name.lastIndexOf('.');return dot>=0&&EXTENSIONS.contains(name.substring(dot+1).toLowerCase(Locale.ROOT));}
    @FunctionalInterface private interface Task<T>{T run()throws Exception;}
    private <T> void runBackground(Task<T> task,Consumer<T> done,String error){
        if(busy)return;busy=true;for(JButton button:List.of(ui.refresh,ui.fetch,ui.add,ui.save))button.setEnabled(false);
        SwingWorker<T,Void> worker=new SwingWorker<>(){
            @Override protected T doInBackground()throws Exception{return task.run();}
            @Override protected void done(){
                workers.remove(this);busy=false;for(JButton button:List.of(ui.refresh,ui.fetch,ui.add,ui.save))button.setEnabled(true);
                if(isCancelled())return;
                try{done.accept(get());}catch(Exception ex){ui.status.setText(error);}
            }
        };workers.add(worker);worker.execute();
    }
    @Override public boolean canClose(){return !dirty||(!GraphicsEnvironment.isHeadless()&&JOptionPane.showConfirmDialog(this,"Danh mục có thay đổi chưa lưu. Đóng cửa sổ?","Danh mục báo cáo",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);}
    public BaoCaoDanhMucBoCuc getCatalogUi(){return ui;}
    public BaoCaoDanhMucMoHinh getCatalogModel(){return model;}
}
