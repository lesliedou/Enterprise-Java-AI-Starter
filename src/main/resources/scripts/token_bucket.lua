-- KEYS[1]: 限流 Key (如 ai:ratelimit:app123)
-- ARGV[1]: 令牌桶容量 (Capacity)
-- ARGV[2]: 令牌填充速率 (Tokens per second)
-- ARGV[3]: 当前时间戳 (Seconds)
-- ARGV[4]: 请求令牌数量 (Requested tokens, 默认为 1)

local bucket_info = redis.call('HMGET', KEYS[1], 'last_tokens', 'last_refreshed')
local last_tokens = tonumber(bucket_info[1])
local last_refreshed = tonumber(bucket_info[2])

local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

if not last_tokens then
    -- 首次初始化
    last_tokens = capacity
    last_refreshed = now
end

-- 计算自上次刷新以来填充的令牌数
local delta = math.max(0, now - last_refreshed)
local filled_tokens = math.min(capacity, last_tokens + (delta * rate))

local allowed = false
local new_tokens = filled_tokens

if filled_tokens >= requested then
    allowed = true
    new_tokens = filled_tokens - requested
end

-- 更新 Redis
redis.call('HMSET', KEYS[1], 'last_tokens', new_tokens, 'last_refreshed', now)
-- 设置 60 秒过期，自动清理冷数据
redis.call('EXPIRE', KEYS[1], 60)

return allowed and 1 or 0
