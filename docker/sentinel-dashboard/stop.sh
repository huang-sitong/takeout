#!/bin/bash
# 停止 Sentinel Dashboard

PID=$(pgrep -f "sentinel-dashboard")

if [ -z "$PID" ]; then
    echo "⚠️  Sentinel Dashboard 未运行"
    exit 0
fi

echo "🛑 停止 Sentinel Dashboard (PID: $PID)..."
kill $PID

sleep 2

if pgrep -f "sentinel-dashboard" > /dev/null; then
    echo "⚠️  进程未响应，强制终止..."
    kill -9 $PID
fi

echo "✅ Sentinel Dashboard 已停止"
