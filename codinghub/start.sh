#!/bin/bash

# CodingHub 启动脚本
# 读取外置配置文件启动 Java 服务

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_JAR="${SCRIPT_DIR}/iaihub.jar"
CONFIG_FILE="${SCRIPT_DIR}/config/application.yaml"

if [ ! -f "$APP_JAR" ]; then
    echo "错误: 未找到应用包 $APP_JAR"
    exit 1
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "错误: 未找到配置文件 $CONFIG_FILE"
    exit 1
fi

echo "启动 CodingHub..."
echo "配置文件: $CONFIG_FILE"

java -jar "$APP_JAR" --spring.config.location="$CONFIG_FILE"