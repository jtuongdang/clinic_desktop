#!/usr/bin/env bash
# ==============================================================================
# TIỆN ÍCH DỌN DẸP / XÓA SẠCH DỮ LIỆU GIAO DỊCH TEST TRONG CSDL (SQL SERVER)
# ==============================================================================
# Hướng dẫn sử dụng:
#   ./scripts/reset-database.sh         -> Xóa sạch dữ liệu test trên CSDL đang liên kết
#   ./scripts/reset-database.sh test    -> Chuyển sang CSDL test (clinicDb_test) và xóa sạch
#   ./scripts/reset-database.sh main    -> Chuyển sang CSDL chính (clinicDb) và xóa sạch
# ==============================================================================

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
JAVA_BIN="/opt/homebrew/opt/openjdk/bin/java"
if [ ! -f "$JAVA_BIN" ]; then
    JAVA_BIN="java"
fi

TARGET_MODE="${1:-current}"

if [ "$TARGET_MODE" = "test" ]; then
    "$DIR/scripts/switch-db.sh" test
elif [ "$TARGET_MODE" = "main" ]; then
    "$DIR/scripts/switch-db.sh" main
fi

echo "================================================================="
echo " TIỆN ÍCH XÓA SẠCH DỮ LIỆU KHÁM CHỮA BỆNH TEST (EHC HIS)"
echo "================================================================="
CURRENT_DB=$(grep "databaseName=" "$DIR/db.properties" 2>/dev/null | sed 's/.*databaseName=\([^;]*\).*/\1/' || echo "clinicDb")
echo "▶ CSDL mục tiêu: $CURRENT_DB"
echo "-----------------------------------------------------------------"
echo "CẢNH BÁO: Thao tác này sẽ xóa sạch toàn bộ Bệnh nhân, Lượt khám,"
echo "Đơn thuốc, Viện phí, Cận lâm sàng... nhưng BẢO TOÀN toàn bộ"
echo "Danh mục (Thuốc, Dịch vụ, ICD-10, Kho, Phòng khám) và Tài khoản."
echo "-----------------------------------------------------------------"

CP="/Users/jtuong/.m2/repository/com/microsoft/sqlserver/mssql-jdbc/12.6.1.jre11/mssql-jdbc-12.6.1.jre11.jar:\
$DIR/clinic-domain/target/classes:\
$DIR/clinic-application/target/classes:\
$DIR/clinic-infrastructure/target/classes:\
$DIR/clinic-desktop/target/classes"

if [ -f "$DIR/clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar" ]; then
    "$JAVA_BIN" -cp "$DIR/clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar:$CP" vn.clinicmanagement.infrastructure.persistence.DatabaseResetTool
else
    "$JAVA_BIN" -cp "$CP" vn.clinicmanagement.infrastructure.persistence.DatabaseResetTool
fi
