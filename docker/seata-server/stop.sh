#!/bin/bash
# ============================================================
# Seata Server 停止脚本
# 使用方式：cd docker/seata-server && ./stop.sh
# ============================================================

CONTAINER_NAME="seata-server"

if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "停止并移除容器 ${CONTAINER_NAME}..."
    docker rm -f "${CONTAINER_NAME}"
    echo "已停止。"
else
    echo "容器 ${CONTAINER_NAME} 不存在。"
fi
