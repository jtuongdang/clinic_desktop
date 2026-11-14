package vn.clinicmanagement.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tách script T-SQL thành từng batch theo dòng phân cách độc lập {@code GO}.
 */
public final class SQLMayChuLoTach {

    private SQLMayChuLoTach() {
    }

    /**
     * Tách nội dung script SQL thành danh sách các batch theo thứ tự xuất hiện,
     * nhận diện dòng {@code GO} không phân biệt hoa thường cùng khoảng trắng bao quanh,
     * đồng thời loại bỏ các batch rỗng.
     *
     * @param sqlContent nội dung script T-SQL
     * @return danh sách các câu lệnh/batch không rỗng đã được cắt khoảng trắng hai đầu
     */
    public static List<String> split(String sqlContent) {
        if (sqlContent == null || sqlContent.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = sqlContent.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        List<String> batches = new ArrayList<>();
        StringBuilder currentBatch = new StringBuilder();

        for (String line : lines) {
            if (isGoBatchSeparator(line)) {
                addNonEmptyBatch(batches, currentBatch);
                currentBatch.setLength(0);
            } else {
                if (currentBatch.length() > 0) {
                    currentBatch.append('\n');
                }
                currentBatch.append(line);
            }
        }

        addNonEmptyBatch(batches, currentBatch);
        return Collections.unmodifiableList(batches);
    }

    public static List<String> splitBatches(String sqlContent) {
        return split(sqlContent);
    }

    /** Hàm `isGoBatchSeparator`: là `Go` lo `Separator`. */
    private static boolean isGoBatchSeparator(String line) {
        return "GO".equalsIgnoreCase(line.trim());
    }

    /** Hàm `addNonEmptyBatch`: thêm `Non` rỗng lo. */
    private static void addNonEmptyBatch(List<String> batches, StringBuilder batchBuilder) {
        String trimmed = batchBuilder.toString().trim();
        if (!trimmed.isEmpty()) {
            batches.add(trimmed);
        }
    }
}
