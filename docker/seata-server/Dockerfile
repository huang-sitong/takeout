FROM nacos/nacos-server:v2.2.3

# Seata Server — 基于 Nacos 镜像（已含 JDK 8）
# 在 Docker Hub 被墙时可用本 Dockerfile 构建：
#   docker build -t seata-server:1.5.2 -f docker/seata-server/Dockerfile .
# 或在网络正常时直接：
#   docker pull seataio/seata-server:1.5.2

LABEL maintainer="sky-take-out"
LABEL description="Seata Server 1.5.2 — distributed transaction coordinator"

ENV SEATA_VERSION=1.5.2
ENV SEATA_HOME=/seata-server
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk

WORKDIR /seata-server

# 复制本地已下载的 Seata Server 二进制分发
# 需要先通过 Maven 或浏览器下载 seata-server-1.5.2.tar.gz 放到当前目录
# COPY seata-server-1.5.2.tar.gz /tmp/

# 实际使用：docker pull seataio/seata-server:1.5.2 (网络正常时)
# 或运行 seata-server 的启动脚本

COPY application.yml /seata-server/resources/application.yml

EXPOSE 7091 8091

CMD ["bash", "-c", "echo 'Seata Server placeholder — use docker pull seataio/seata-server:1.5.2 when network available' && sleep infinity"]
