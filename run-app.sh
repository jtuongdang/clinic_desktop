#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA_BIN="/opt/homebrew/opt/openjdk/bin/java"
if [ ! -f "$JAVA_BIN" ]; then
    JAVA_BIN="java"
fi

# Thiết lập kết nối mặc định tới clinicDb (SQL Server localhost) nếu chưa có biến môi trường
export DB_URL="${DB_URL:-jdbc:sqlserver://localhost:1433;databaseName=clinicDb;encrypt=true;trustServerCertificate=true}"
export DB_USERNAME="${DB_USERNAME:-sa}"
export DB_PASSWORD="${DB_PASSWORD:-Password123!}"

if [ -f "$DIR/clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar" ]; then
    echo "=========================================================="
    echo " Khởi động Hệ Thống Quản Lý Bệnh Viện EHC HIS (Desktop)  "
    echo " CSDL: $DB_URL                                            "
    echo " Tài khoản: $DB_USERNAME                                  "
    echo " Artifact: clinic-desktop-1.0.0-SNAPSHOT.jar              "
    echo "=========================================================="
    exec "$JAVA_BIN" -jar "$DIR/clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar" "$@"
else
    CP="/Users/jtuong/.m2/repository/com/microsoft/sqlserver/mssql-jdbc/12.6.1.jre11/mssql-jdbc-12.6.1.jre11.jar:\
/Users/jtuong/.m2/repository/com/formdev/flatlaf/3.4.1/flatlaf-3.4.1.jar:\
$DIR/clinic-domain/target/classes:\
$DIR/clinic-application/target/classes:\
$DIR/clinic-infrastructure/target/classes:\
$DIR/clinic-desktop/target/classes"

    echo "Đang khởi động Phần Mềm Quản Lý Bệnh Viện EHC (Desktop Swing từ Classes)..."
    exec "$JAVA_BIN" -cp "$CP" vn.clinicmanagement.desktop.Main "$@"
fi
