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

echo "==> [1/3] 打包部署包..."
bash "$SCRIPT_DIR/package-artifacts.sh"

TARBALL="$PROJECT_DIR/dist/sqlbotai-deploy.tar.gz"
if [[ ! -f "$TARBALL" ]]; then
    echo "错误: 未找到 $TARBALL"
    exit 1
fi

echo "==> [2/3] 上传到 $SERVER_HOST ..."
"${SCP_CMD[@]}" "$TARBALL" "$SERVER_USER@$SERVER_HOST:/tmp/sqlbotai-deploy.tar.gz"

echo "==> [3/3] 服务器安装并启动..."
"${SSH_CMD[@]}" "$SERVER_USER@$SERVER_HOST" "bash -s" <<'REMOTE'
set -euo pipefail
cd /tmp
rm -rf sqlbotai-bundle
tar xzf sqlbotai-deploy.tar.gz
bash sqlbotai-bundle/install-on-server.sh /tmp/sqlbotai-bundle
REMOTE

echo ""
echo "=========================================="
echo "  部署成功!"
echo "  访问地址: http://$SERVER_HOST"
echo "  查看日志: ssh $SERVER_USER@$SERVER_HOST 'journalctl -u sqlbotai -f'"
echo "=========================================="
