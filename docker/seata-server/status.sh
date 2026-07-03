#!/bin/bash
# ============================================================
# Seata Server 状态查看脚本
# 使用方式：cd docker/seata-server && ./status.sh
# ============================================================

CONTAINER_NAME="seata-server"

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Seata Server 运行中:"
    docker ps --filter "name=${CONTAINER_NAME}" --format "  ID: {{.ID}}  Ports: {{.Ports}}  Status: {{.Status}}"
else
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "Seata Server 已停止（容器存在但未运行）。"
    else
        echo "Seata Server 未部署（容器不存在）。"
    fi
fi
