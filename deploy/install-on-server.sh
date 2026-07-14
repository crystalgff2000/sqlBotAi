#!/bin/bash
# 在阿里云服务器上执行（需先将 sqlbotai-deploy.tar.gz 上传到 /tmp）
set -euo pipefail

BUNDLE_DIR="${1:-/tmp/sqlbotai-bundle}"
APP_HOME="${APP_HOME:-/opt/sqlbotai}"

if [[ ! -f "$BUNDLE_DIR/sqlBotAi.jar" ]]; then
    echo "错误: 未找到 $BUNDLE_DIR/sqlBotAi.jar"
    echo "用法: tar xzf /tmp/sqlbotai-deploy.tar.gz -C /tmp && bash install-on-server.sh /tmp/sqlbotai-bundle"
    exit 1
fi

cp "$BUNDLE_DIR/sqlbotai.service" /tmp/sqlbotai.service
cp "$BUNDLE_DIR/nginx-sqlbotai.conf" /tmp/nginx-sqlbotai.conf
cp "$BUNDLE_DIR/install-server.sh" /tmp/install-server.sh
chmod +x /tmp/install-server.sh

if [[ ! -f /etc/systemd/system/sqlbotai.service ]]; then
    APP_HOME="$APP_HOME" bash /tmp/install-server.sh
else
    cp /tmp/sqlbotai.service /etc/systemd/system/sqlbotai.service
    cp /tmp/nginx-sqlbotai.conf /etc/nginx/conf.d/sqlbotai.conf
    systemctl daemon-reload
    nginx -t && systemctl restart nginx
fi

mkdir -p "$APP_HOME"/{app,data,logs,config}
cp "$BUNDLE_DIR/sqlBotAi.jar" "$APP_HOME/app/sqlBotAi.jar"
rm -rf "$APP_HOME/app/exploded"
mkdir -p "$APP_HOME/app/exploded"
unzip -q -o "$APP_HOME/app/sqlBotAi.jar" -d "$APP_HOME/app/exploded"

if [[ -f "$BUNDLE_DIR/server.env" ]]; then
    # shellcheck source=/dev/null
    source "$BUNDLE_DIR/server.env"
fi

cat > "$APP_HOME/config/env" <<ENV
DB_PASSWORD=${DB_PASSWORD:-}
DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-}
DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY:-}
ENV
chmod 600 "$APP_HOME/config/env"

if [[ -f /root/.ai-data-query.cnf ]]; then
    cp /root/.ai-data-query.cnf "$APP_HOME/config/mysql.cnf"
    chmod 600 "$APP_HOME/config/mysql.cnf"
fi

id sqlbotai &>/dev/null || useradd -r -s /bin/false sqlbotai
chown -R sqlbotai:sqlbotai "$APP_HOME"
chown sqlbotai:sqlbotai "$APP_HOME/config/env"
[[ -f "$APP_HOME/config/mysql.cnf" ]] && chown sqlbotai:sqlbotai "$APP_HOME/config/mysql.cnf"

systemctl daemon-reload
systemctl enable sqlbotai
systemctl restart sqlbotai
sleep 5

if systemctl is-active sqlbotai >/dev/null; then
    echo ""
    echo "=========================================="
    echo "  部署成功!"
    echo "  访问: http://$(curl -s --max-time 2 ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')"
    echo "  日志: journalctl -u sqlbotai -f"
    echo "=========================================="
else
    echo "服务启动失败，请查看: journalctl -u sqlbotai -n 50 --no-pager"
    exit 1
fi
