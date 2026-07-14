#!/bin/bash
set -euo pipefail

APP_HOME="${APP_HOME:-/opt/sqlbotai}"
APP_USER="${APP_USER:-sqlbotai}"

echo "==> 安装 Java 17..."
if command -v apt-get &>/dev/null; then
    apt-get update -qq
    apt-get install -y openjdk-17-jre-headless nginx unzip python3 python3-pip mysql-client
elif command -v yum &>/dev/null; then
    yum install -y java-17-openjdk-headless nginx unzip python3 python3-pip mysql
else
    echo "不支持的系统，请手动安装 Java 17 和 Nginx"
    exit 1
fi

echo "==> 安装 Python Pillow（图表渲染依赖，兼容 Python 3.6）..."
python3 -m pip install --upgrade 'pip<21' 'setuptools<60' wheel
python3 -m pip install 'Pillow<9'
python3 -c "from PIL import Image; print('Pillow OK:', Image.__version__)"

echo "==> 创建应用用户和目录..."
id "$APP_USER" &>/dev/null || useradd -r -s /bin/false "$APP_USER"
mkdir -p "$APP_HOME"/{app,data,logs,config}
chown -R "$APP_USER:$APP_USER" "$APP_HOME"

echo "==> 安装 systemd 服务..."
cp /tmp/sqlbotai.service /etc/systemd/system/sqlbotai.service
systemctl daemon-reload
systemctl enable sqlbotai

echo "==> 配置 Nginx 反向代理..."
cp /tmp/nginx-sqlbotai.conf /etc/nginx/conf.d/sqlbotai.conf
# 禁用 nginx.conf 中的默认欢迎页，避免覆盖反向代理
if grep -q 'listen.*80 default_server' /etc/nginx/nginx.conf; then
    sed -i '/^    server {/,/^    }/s/^/# /' /etc/nginx/nginx.conf
fi
nginx -t && systemctl enable nginx && systemctl restart nginx

echo "==> 服务器环境准备完成"
echo "    应用目录: $APP_HOME"
echo "    服务管理: systemctl {start|stop|restart|status} sqlbotai"
