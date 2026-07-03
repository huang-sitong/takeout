#!/bin/bash
# ============================================================
# Seata Server 启动脚本 (Docker)
# 使用方式：cd docker/seata-server && ./start.sh
# ============================================================

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

CONTAINER_NAME="seata-server"
IMAGE="seataio/seata-server:1.5.2"

# 检查是否已存在同名容器
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "容器 ${CONTAINER_NAME} 已存在，先移除..."
    docker rm -f "${CONTAINER_NAME}" 2>/dev/null
fi

echo "启动 Seata Server..."
docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart=always \
    -p 8091:8091 \
    -p 7091:7091 \
    -v "${SCRIPT_DIR}/application.yml:/seata-server/resources/application.yml:ro" \
    "${IMAGE}"

echo "Seata Server 已启动 (PID: $(docker inspect -f '{{.State.Pid}}' ${CONTAINER_NAME}))"
echo "  控制台: http://localhost:7091"
echo "  TC 服务端口: 8091"
echo "  查看日志: docker logs -f ${CONTAINER_NAME}"
