#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/deploy.env"

if [[ -f "$ENV_FILE" ]]; then
    # shellcheck source=/dev/null
    source "$ENV_FILE"
fi

SERVER_HOST="${SERVER_HOST:?请在 deploy/deploy.env 中设置 SERVER_HOST}"
SERVER_USER="${SERVER_USER:-root}"
SERVER_PORT="${SERVER_PORT:-22}"
APP_HOME="${APP_HOME:-/opt/sqlbotai}"
SSH_KEY="${SSH_KEY:-}"
SSH_PASSWORD="${SSH_PASSWORD:-}"

# 构建 SSH/SCP 命令
SSH_BASE=(ssh -p "$SERVER_PORT" -o StrictHostKeyChecking=accept-new)
SCP_BASE=(scp -P "$SERVER_PORT" -o StrictHostKeyChecking=accept-new)
[[ -n "$SSH_KEY" ]] && SSH_BASE+=(-i "$SSH_KEY") && SCP_BASE+=(-i "$SSH_KEY")

if [[ -n "$SSH_PASSWORD" ]]; then
    if ! command -v sshpass &>/dev/null; then
        echo "错误: 密码登录需要 sshpass，请执行: brew install hudochenkov/sshpass/sshpass"
        exit 1
    fi
    export SSHPASS="$SSH_PASSWORD"
    SSH_CMD=(sshpass -e "${SSH_BASE[@]}")
    SCP_CMD=(sshpass -e "${SCP_BASE[@]}")
else
    SSH_CMD=("${SSH_BASE[@]}")
    SCP_CMD=("${SCP_BASE[@]}")
fi

JAR="$PROJECT_DIR/target/sqlBotAi-1.0.0.jar"

echo "==> [1/4] 打包项目..."
cd "$PROJECT_DIR"
mvn clean package -DskipTests -q

if [[ ! -f "$JAR" ]]; then
    echo "错误: 未找到 $JAR"
    exit 1
fi

echo "==> [2/4] 上传文件到 $SERVER_HOST ..."
"${SCP_CMD[@]}" "$JAR" "$SERVER_USER@$SERVER_HOST:/tmp/sqlBotAi.jar"
"${SCP_CMD[@]}" "$SCRIPT_DIR/sqlbotai.service" "$SERVER_USER@$SERVER_HOST:/tmp/sqlbotai.service"
"${SCP_CMD[@]}" "$SCRIPT_DIR/nginx-sqlbotai.conf" "$SERVER_USER@$SERVER_HOST:/tmp/nginx-sqlbotai.conf"
"${SCP_CMD[@]}" "$SCRIPT_DIR/install-server.sh" "$SERVER_USER@$SERVER_HOST:/tmp/install-server.sh"

echo "==> [3/4] 安装/更新服务器环境..."
"${SSH_CMD[@]}" "$SERVER_USER@$SERVER_HOST" "bash -s" <<REMOTE
set -euo pipefail
APP_HOME="$APP_HOME"

if [[ ! -f /etc/systemd/system/sqlbotai.service ]]; then
    chmod +x /tmp/install-server.sh
    APP_HOME="\$APP_HOME" bash /tmp/install-server.sh
else
    cp /tmp/sqlbotai.service /etc/systemd/system/sqlbotai.service
    cp /tmp/nginx-sqlbotai.conf /etc/nginx/conf.d/sqlbotai.conf
    systemctl daemon-reload
    nginx -t && systemctl restart nginx
fi

mkdir -p "\$APP_HOME"/{app,data,logs,config}
cp /tmp/sqlBotAi.jar "\$APP_HOME/app/sqlBotAi.jar"
rm -rf "\$APP_HOME/app/exploded"
mkdir -p "\$APP_HOME/app/exploded"
unzip -q -o "\$APP_HOME/app/sqlBotAi.jar" -d "\$APP_HOME/app/exploded"
chown -R sqlbotai:sqlbotai "\$APP_HOME"

cat > "\$APP_HOME/config/env" <<ENV
DB_PASSWORD=${DB_PASSWORD:-}
DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-}
DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY:-}
ENV
chmod 600 "\$APP_HOME/config/env"
chown sqlbotai:sqlbotai "\$APP_HOME/config/env"
REMOTE

echo "==> [4/4] 启动服务..."
"${SSH_CMD[@]}" "$SERVER_USER@$SERVER_HOST" "systemctl restart sqlbotai && sleep 5 && systemctl is-active sqlbotai"

echo ""
echo "=========================================="
echo "  部署成功!"
echo "  访问地址: http://$SERVER_HOST"
echo "  查看日志: ssh $SERVER_USER@$SERVER_HOST 'journalctl -u sqlbotai -f'"
echo "=========================================="
