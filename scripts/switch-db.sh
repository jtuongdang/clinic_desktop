#!/usr/bin/env bash
# ==============================================================================
# TIỆN ÍCH CHUYỂN ĐỔI CƠ SỞ DỮ LIỆU: CLINIC_DB (CHÍNH) <-> CLINIC_DB_TEST (TEST)
# ==============================================================================

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
TARGET_MAIN="$DIR/db.properties"
TARGET_INFRA="$DIR/clinic-infrastructure/src/main/resources/db.properties"

MODE="${1:-status}"

if [ "$MODE" = "test" ]; then
    echo "🔄 Đang chuyển kết nối sang CSDL TEST: clinicDb_test..."
    cp "$DIR/db.test.properties" "$TARGET_MAIN"
    cp "$DIR/db.test.properties" "$TARGET_INFRA"
    echo "✅ Đã liên kết ứng dụng với CSDL TEST (databaseName=clinicDb_test) thành công!"
    echo "▶ Khởi động ứng dụng ngay: ./run-app.sh"
elif [ "$MODE" = "main" ]; then
    echo "🔄 Đang chuyển kết nối sang CSDL CHÍNH: clinicDb..."
    sed -i '' 's/databaseName=clinicDb_test/databaseName=clinicDb/g' "$TARGET_MAIN"
    sed -i '' 's/databaseName=clinicDb_test/databaseName=clinicDb/g' "$TARGET_INFRA"
    echo "✅ Đã liên kết ứng dụng với CSDL CHÍNH (databaseName=clinicDb) thành công!"
elif [ "$MODE" = "status" ]; then
    CURRENT_DB=$(grep "databaseName=" "$TARGET_MAIN" | sed 's/.*databaseName=\([^;]*\).*/\1/')
    echo "================================================================="
    echo "📊 TRẠNG THÁI KẾT NỐI CSDL HIỆN TẠI:"
    echo "▶ Database đang liên kết: $CURRENT_DB"
    echo "================================================================="
    echo "Cách chuyển đổi:"
    echo "  ./scripts/switch-db.sh test   -> Chuyển sang CSDL test (clinicDb_test)"
    echo "  ./scripts/switch-db.sh main   -> Chuyển về CSDL chính (clinicDb)"
    echo "  ./scripts/switch-db.sh status -> Xem trạng thái hiện tại"
else
    echo "Lệnh không hợp lệ. Sử dụng: $0 [test|main|status]"
    exit 1
fi
