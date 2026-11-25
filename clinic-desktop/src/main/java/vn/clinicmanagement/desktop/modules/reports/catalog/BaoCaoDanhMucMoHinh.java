package vn.clinicmanagement.desktop.modules.reports.catalog;

import javax.swing.table.AbstractTableModel;
import java.text.Normalizer;
import java.util.*;

/** Flattened, expandable category/group/template tree for a native Swing table. */
public final class BaoCaoDanhMucMoHinh extends AbstractTableModel {
    public enum Kind { CATEGORY, GROUP, REPORT }
    public record Row(String key,String title,int depth,Kind kind,BaoCaoMau report,String category,String group) {}
    private final List<BaoCaoMau> entries=new ArrayList<>();
    private final List<Row> rows=new ArrayList<>();
    private final Set<String> collapsed=new HashSet<>();
    private String keyword="",category="",group="";
    public void replace(List<BaoCaoMau> values){entries.clear();entries.addAll(values);rebuild();}
    public List<BaoCaoMau> entries(){return List.copyOf(entries);}
    public Row row(int index){return rows.get(index);}
    public void filter(String keyword,String category,String group){this.keyword=normalize(keyword);this.category=category;this.group=group;rebuild();}
    public boolean expanded(Row row){return !keyword.isEmpty()||!collapsed.contains(row.key());}
    public void toggle(int index){Row row=rows.get(index);if(row.kind()==Kind.REPORT)return;if(!collapsed.add(row.key()))collapsed.remove(row.key());rebuild();}
    public void expandAll(){collapsed.clear();rebuild();}
    public boolean move(String key,int direction){
        if(direction!=1&&direction!=-1)return false;
        Row selected=rows.stream().filter(r->r.key().equals(key)).findFirst().orElse(null);
        if(selected==null)return false;
        if(selected.kind()==Kind.REPORT){
            List<BaoCaoMau> siblings=entries.stream().filter(e->e.category().equals(selected.category())&&e.group().equals(selected.group())).toList();
            int current=-1;for(int i=0;i<siblings.size();i++)if(siblings.get(i).key().equals(key))current=i;
            int target=current+direction;
            if(target>=0&&target<siblings.size()){
                Collections.swap(entries,entries.indexOf(siblings.get(current)),entries.indexOf(siblings.get(target)));rebuild();return true;
            }
            if(siblings.size()>1)return false;
        }
        boolean categories=selected.kind()==Kind.CATEGORY;
        List<String> order=new ArrayList<>(entries.stream().filter(e->categories||e.category().equals(selected.category()))
                .map(e->categories?e.category():e.group()).distinct().toList());
        int current=order.indexOf(categories?selected.category():selected.group()),target=current+direction;
        if(current<0||target<0||target>=order.size())return false;
        Collections.swap(order,current,target);
        List<BaoCaoMau> reordered=new ArrayList<>();
        List<String> categoryOrder=categories?order:entries.stream().map(BaoCaoMau::category).distinct().toList();
        for(String cat:categoryOrder){
            List<String> groupOrder=!categories&&cat.equals(selected.category())?order:entries.stream().filter(e->e.category().equals(cat)).map(BaoCaoMau::group).distinct().toList();
            for(String group:groupOrder)entries.stream().filter(e->e.category().equals(cat)&&e.group().equals(group)).forEach(reordered::add);
        }
        entries.clear();entries.addAll(reordered);rebuild();return true;
    }
    public void add(BaoCaoMau entry){entries.add(entry);rebuild();}
    public void attach(String key,String path,String file){
        for(int i=0;i<entries.size();i++)if(entries.get(i).key().equals(key)){
            var e=entries.get(i);entries.set(i,new BaoCaoMau(e.key(),e.category(),e.group(),e.name(),file,e.reportId(),path));break;
        }rebuild();
    }
    public int indexOf(String key){for(int i=0;i<rows.size();i++)if(rows.get(i).key().equals(key))return i;return -1;}
    private void rebuild(){
        rows.clear();Map<String,Map<String,List<BaoCaoMau>>> grouped=new LinkedHashMap<>();
        for(var e:entries){
            if(!category.isEmpty()&&!e.category().equals(category))continue;
            if(!group.isEmpty()&&!e.group().equals(group))continue;
            if(!normalize(e.category()+" "+e.group()+" "+e.name()+" "+e.fileName()+" "+e.reportId()).contains(keyword))continue;
            grouped.computeIfAbsent(e.category(),ignored->new LinkedHashMap<>()).computeIfAbsent(e.group(),ignored->new ArrayList<>()).add(e);
        }
        grouped.forEach((cat,groups)->{
            Row c=new Row("c:"+cat,cat,0,Kind.CATEGORY,null,cat,"");rows.add(c);
            if(collapsed.contains(c.key())&&keyword.isEmpty())return;
            groups.forEach((name,items)->{
                Row g=new Row("g:"+cat+"/"+name,name,1,Kind.GROUP,null,cat,name);rows.add(g);
                if(collapsed.contains(g.key())&&keyword.isEmpty())return;
                for(var e:items)rows.add(new Row(e.key(),e.name(),2,Kind.REPORT,e,cat,name));
            });
        });fireTableDataChanged();
    }
    public static String normalize(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).strip();}
    @Override public int getRowCount(){return rows.size();}
    @Override public int getColumnCount(){return 3;}
    @Override public String getColumnName(int col){return new String[]{"Tên Báo Cáo","Tên Tập Tin","ID"}[col];}
    @Override public Object getValueAt(int row,int col){var item=rows.get(row);return switch(col){case 0->item.title();case 1->item.report()==null?"":item.report().fileName();default->item.report()==null?"":item.report().reportId();};}
    @Override public boolean isCellEditable(int row,int col){return false;}
}
