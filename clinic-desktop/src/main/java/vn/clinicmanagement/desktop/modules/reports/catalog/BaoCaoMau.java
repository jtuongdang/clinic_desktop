package vn.clinicmanagement.desktop.modules.reports.catalog;

/** Catalog metadata; a file name does not imply that its template is installed. */
public record BaoCaoMau(String key,String category,String group,String name,String fileName,String reportId,String localPath) {
    public BaoCaoMau {
        if(key==null||key.isBlank()||category==null||category.isBlank()||group==null||group.isBlank()||name==null||name.isBlank())
            throw new IllegalArgumentException("Nhập nhóm và tên báo cáo.");
        fileName=fileName==null?"":fileName;reportId=reportId==null?"":reportId;localPath=localPath==null?"":localPath;
    }
}
