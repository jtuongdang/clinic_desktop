package vn.clinicmanagement.application.catalog;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Snapshot includes the catalog version, so a stale form cannot overwrite a newer update. */
public record DichVuCauHinh(int id, long version, Map<String,String> values) {
    public DichVuCauHinh { values=Map.copyOf(values); }
    public String value(String key){return values.getOrDefault(key,"");}
    public static BigDecimal number(String text) {
        if(text==null||text.isBlank())return null;
        String input=text.trim();
        if(!input.matches("-?(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,4})?"))
            throw new IllegalArgumentException("Số không hợp lệ. Dùng dấu phẩy phân cách hàng nghìn, dấu chấm cho phần thập phân.");
        return new BigDecimal(input.replace(",",""));
    }
    public void validate(){
        if(id<=0||version<0||value("code").isBlank()||value("name").isBlank())throw new IllegalArgumentException("Mã và tên dịch vụ không được để trống.");
        if(value("code").length()>50||value("name").length()>255||value("group").length()>100||value("note").length()>500)
            throw new IllegalArgumentException("Mã tối đa 50, tên 255, loại dịch vụ 100 và ghi chú 500 ký tự.");
        if(!Set.of("Khám bệnh","Xét nghiệm","Chẩn đoán hình ảnh","Chuyên khoa").contains(value("category")))
            throw new IllegalArgumentException("Chọn nhóm dịch vụ hợp lệ.");
        for(String key:Set.of("insurancePrice","hospitalPrice","servicePrice","foreignPrice","defaultQuantity","filmQuantity","reuseDays","duration","minimumDuration")){
            var n=number(value(key));if(n!=null&&(n.signum()<0||n.precision()-n.scale()>16||n.scale()>2))
                throw new IllegalArgumentException("Giá, số lượng và thời gian phải không âm, tối đa 2 chữ số thập phân.");
        }
        for(String key:Set.of("inNetworkPercent","outNetworkPercent")){
            var n=number(value(key));if(n!=null&&(n.signum()<0||n.compareTo(BigDecimal.valueOf(100))>0))
                throw new IllegalArgumentException("Tỷ lệ thanh toán phải từ 0 đến 100%.");
        }
        for(String key:Set.of("insuranceLimit","materialLimit","drugLimit")){
            var n=number(value(key));if(n!=null&&n.compareTo(BigDecimal.valueOf(-1))<0)
                throw new IllegalArgumentException("Định mức phải từ -1 trở lên.");
        }
        if(number(value("insurancePrice"))==null||number(value("servicePrice"))==null)
            throw new IllegalArgumentException("Nhập giá BHYT và giá dịch vụ; có thể nhập 0.");
        validateLabSettings();
        if(!value("consumedMaterialsDefinition").isBlank())VatTuHaoPhiCauHinh.decode(value("consumedMaterialsDefinition")).validate();
        if(values.size()>150||values.entrySet().stream().anyMatch(e->e.getValue().length()>(e.getKey().equals("consumedMaterialsDefinition")?256000:16000)))throw new IllegalArgumentException("Dữ liệu cấu hình quá dài.");
    }

    private void validateLabSettings() {
        if (value("labSpecimenType").length() > 150)
            throw new IllegalArgumentException("Loại bệnh phẩm tối đa 150 ký tự.");
        for (String key : Set.of("labPrintDecimals", "labPrintIndent")) {
            String setting = value(key);
            if (!setting.isBlank() && !setting.matches("[0-9]{1,3}"))
                throw new IllegalArgumentException("Số chữ số thập phân và số tab lùi phải phải là số nguyên từ 0 đến 999.");
        }
        if (Boolean.parseBoolean(value("labPrintTubeHen")) && Boolean.parseBoolean(value("labSkipTubeHen")))
            throw new IllegalArgumentException("Chỉ chọn một trong hai tùy chọn in hoặc không in ống HEN.");
    }
}
