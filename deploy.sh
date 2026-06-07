#!/bin/bash

# Enterprise AI Starter 一键部署脚本

echo "🚀 开始部署 Enterprise AI Platform..."

# 1. 编译后端
echo "📦 正在编译后端 Java 项目..."
./mvnw clean package -DskipTests

# 2. 构建 Docker 镜像
echo "🐳 正在构建 Docker 镜像..."
docker build -t enterprise-ai-backend:latest .
docker build -t enterprise-ai-frontend:latest ./frontend

# 3. 运行本地环境 (Docker Compose)
echo "🛠️ 正在启动本地容器环境..."
docker-compose up -d

echo "✅ 部署完成！"
echo "🌐 前端访问地址: http://localhost:3000"
echo "⚙️  后端 API 地址: http://localhost:8080"
echo "📊 Redis 端口: 6379"
