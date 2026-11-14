package vn.clinicmanagement.desktop.core.docx;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DOCXQuanLyTest {

    @Test
    void saveFailureMessageDoesNotExposeIOExceptionDetails() {
        String sensitiveDetails = "/var/data/clinic/patients/sensitive_file.docx (Permission denied - errno 13)";
        IOException exception = new IOException(sensitiveDetails);

        String message = DOCXQuanLy.toUserFacingSaveFailureMessage(exception);

        assertNotNull(message);
        assertEquals(
                "Không thể lưu tài liệu DOCX. Vui lòng chọn thư mục khác hoặc kiểm tra quyền truy cập và dung lượng lưu trữ, rồi thử lại.",
                message
        );
        assertFalse(message.contains(sensitiveDetails));
        assertFalse(message.contains("/var/data/clinic"));
        assertFalse(message.contains("Permission denied"));
    }
}
