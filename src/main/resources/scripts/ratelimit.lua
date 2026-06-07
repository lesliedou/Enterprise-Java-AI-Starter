-- KEYS[1]: 令牌桶 Key
-- ARGV[1]: 令牌桶容量 (Capacity)
-- ARGV[2]: 令牌填充速率 (Tokens per second)
-- ARGV[3]: 当前时间戳 (Seconds)
-- ARGV[4]: 请求令牌数量 (Requested tokens)

local bucket_info = redis.call('HMGET', KEYS[1], 'last_tokens', 'last_refreshed')
local last_tokens = tonumber(bucket_info[1])
local last_refreshed = tonumber(bucket_info[2])

if not last_tokens then
    last_tokens = tonumber(ARGV[1])
    last_refreshed = tonumber(ARGV[3])
end

local capacity = tonumber(ARGV[1])
local rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- 计算自上次刷新以来填充的令牌数
local delta = math.max(0, now - last_refreshed)
local filled_tokens = math.min(capacity, last_tokens + (delta * rate))

local allowed = false
local new_tokens = filled_tokens

if filled_tokens >= requested then
    allowed = true
    new_tokens = filled_tokens - requested
end

redis.call('HMSET', KEYS[1], 'last_tokens', new_tokens, 'last_refreshed', now)
-- 设置过期时间，防止内存泄漏
redis.call('EXPIRE', KEYS[1], 60)

return allowed and 1 or 0
