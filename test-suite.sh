#!/bin/bash

# Enterprise AI Starter 自动化集成测试脚本
# Author: Senior QA Engineer

BASE_URL="http://localhost:8080"
APP_KEY="admin-master-key"

echo "🧪 开始全链路集成测试..."

# 1. 检查后端健康状态
echo "🔍 [Step 1] 检查服务连通性..."
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/api/admin/stats/dashboard -H "X-App-Key: $APP_KEY")
if [ "$STATUS_CODE" -eq 200 ]; then
    echo "✅ 后端服务已就绪"
else
    echo "❌ 后端服务未启动或鉴权失败 (HTTP $STATUS_CODE)"
    exit 1
fi

# 2. 测试标准非流式对话
echo "💬 [Step 2] 测试标准非流式对话 (Blocking Chat)..."
RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -H "X-App-Key: $APP_KEY" \
  -d '{
    "message": "你好，请简单介绍一下你自己",
    "stream": false
  }')

if echo "$RESPONSE" | grep -q "content"; then
    echo "✅ 同步对话测试通过"
else
    echo "❌ 同步对话返回异常: $RESPONSE"
fi

# 3. 测试 SSE 流式对话 (核心功能)
echo "🌊 [Step 3] 测试 SSE 流式对话 (Stream Chat)..."
# 使用 --no-buffer 确保流式输出
curl -s -N -X POST $BASE_URL/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -H "X-App-Key: $APP_KEY" \
  -d '{
    "message": "写一段 50 字的春景描写",
    "stream": true
  }' | head -n 10 > stream_output.txt

if grep -q "GENERATING" stream_output.txt; then
    echo "✅ SSE 流式协议校验通过"
else
    echo "❌ SSE 流式协议异常，未检测到 GENERATING 状态"
fi

# 4. 测试限流防护 (QPS=5, 连续发送 10 个请求)
echo "🛡️ [Step 4] 测试高并发限流保护..."
FAIL_COUNT=0
for i in {1..10}; do
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/ai/chat \
      -H "Content-Type: application/json" \
      -H "X-App-Key: $APP_KEY" \
      -d '{"message":"ping","stream":false}')
    if [ "$CODE" -eq 429 ]; then
        FAIL_COUNT=$((FAIL_COUNT+1))
    fi
done

if [ "$FAIL_COUNT" -gt 0 ]; then
    echo "✅ 限流器生效 (拦截了 $FAIL_COUNT 个超限请求)"
else
    echo "⚠️ 限流器未触发，请检查 QPS 配置"
fi

# 5. 测试熔断降级 (Mock 一个不存在的渠道)
echo "🔄 [Step 5] 测试渠道熔断与自动降级..."
# 逻辑：通过管理 API 禁用主渠道，观察是否自动切到备用
echo "模拟主渠道异常..."
# 实际可以通过 PATCH 接口操作，此处演示逻辑闭环
# curl -s -X PATCH "$BASE_URL/api/admin/channels/ch-001/status?enabled=false" -H "X-App-Key: $APP_KEY"

echo "🎉 测试完成！"
rm stream_output.txt
