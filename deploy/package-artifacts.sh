#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUNDLE_DIR="$PROJECT_DIR/dist/sqlbotai-bundle"
JAR="$PROJECT_DIR/target/sqlBotAi-1.0.0.jar"

echo "==> 打包 JAR..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q

if [[ ! -f "$JAR" ]]; then
    echo "错误: 未找到 $JAR"
    exit 1
fi

echo "==> 组装部署包..."
rm -rf "$BUNDLE_DIR"
mkdir -p "$BUNDLE_DIR"
cp "$JAR" "$BUNDLE_DIR/sqlBotAi.jar"
cp "$SCRIPT_DIR/sqlbotai.service" "$BUNDLE_DIR/"
cp "$SCRIPT_DIR/nginx-sqlbotai.conf" "$BUNDLE_DIR/"
cp "$SCRIPT_DIR/install-server.sh" "$BUNDLE_DIR/"
cp "$SCRIPT_DIR/install-on-server.sh" "$BUNDLE_DIR/"

if [[ -f "$SCRIPT_DIR/deploy.env" ]]; then
    grep -E '^(DEEPSEEK_API_KEY|DASHSCOPE_API_KEY|DB_PASSWORD)=' "$SCRIPT_DIR/deploy.env" > "$BUNDLE_DIR/server.env" || true
fi

tar czf "$PROJECT_DIR/dist/sqlbotai-deploy.tar.gz" -C "$PROJECT_DIR/dist" sqlbotai-bundle
ls -lh "$PROJECT_DIR/dist/sqlbotai-deploy.tar.gz"
echo "部署包已生成: dist/sqlbotai-deploy.tar.gz"
