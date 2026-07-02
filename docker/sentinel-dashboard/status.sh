#!/bin/bash
# 查看 Sentinel Dashboard 状态

PID=$(pgrep -f "sentinel-dashboard")

if [ -z "$PID" ]; then
    echo "❌ Sentinel Dashboard 未运行"
    exit 0
fi

echo "✅ Sentinel Dashboard 运行中"
echo "   PID: $PID"
echo "   端口: 8858"
echo "   访问: http://localhost:8858"
echo "   日志: /tmp/sentinel.log"
