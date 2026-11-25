-- Seed session timeout defaults only when the parameter key does not exist.
IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'SESSION_IDLE_TIMEOUT_MINUTES')
BEGIN
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('SESSION_IDLE_TIMEOUT_MINUTES', N'Thời gian không hoạt động của phiên', N'15',
            N'Số phút không thao tác trước khi tự khóa phiên');
END;

IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'SESSION_ABSOLUTE_TIMEOUT_HOURS')
BEGIN
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('SESSION_ABSOLUTE_TIMEOUT_HOURS', N'Tuổi thọ tối đa của phiên', N'12',
            N'Số giờ tối đa cho một phiên đăng nhập');
END;
