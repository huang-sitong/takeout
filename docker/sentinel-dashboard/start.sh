#!/bin/bash
# 启动 Sentinel Dashboard

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if pgrep -f "sentinel-dashboard" > /dev/null; then
    echo "⚠️  Sentinel Dashboard 已在运行"
    echo "   PID: $(pgrep -f 'sentinel-dashboard')"
    echo "   访问: http://localhost:8858"
    exit 0
fi

cd "$SCRIPT_DIR"

echo "🚀 启动 Sentinel Dashboard..."
nohup java -Dserver.port=8858 -jar sentinel-dashboard-1.8.6.jar > /tmp/sentinel.log 2>&1 &

sleep 3

if pgrep -f "sentinel-dashboard" > /dev/null; then
    echo "✅ Sentinel Dashboard 启动成功"
    echo "   PID: $(pgrep -f 'sentinel-dashboard')"
    echo "   访问: http://localhost:8858"
else
    echo "❌ 启动失败，查看日志: /tmp/sentinel.log"
    exit 1
fi
