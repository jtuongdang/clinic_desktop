package vn.clinicmanagement.desktop.core.docx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class DocxManagerTest {

    @Test
    void saveFailureMessageDoesNotExposeIOExceptionDetails() {
        String pathMarker = "/synthetic/path/DOCX_PATH_MARKER.docx";
        String detailMarker = "SYNTHETIC_OS_DETAIL_MARKER";
        IOException exception = new IOException("Synthetic failure at " + pathMarker + ": " + detailMarker);

        String userMessage = DocxManager.toUserFacingSaveFailureMessage(exception);

        assertEquals(
                "Không thể lưu tài liệu DOCX. Vui lòng chọn thư mục khác hoặc kiểm tra quyền truy cập và dung lượng lưu trữ, rồi thử lại.",
                userMessage);
        assertFalse(userMessage.contains(exception.getMessage()));
        assertFalse(userMessage.contains(pathMarker));
        assertFalse(userMessage.contains(detailMarker));
    }
}
