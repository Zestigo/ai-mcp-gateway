-- KEYS[1]: 限流 Key (例如 mcp_gateway:ratelimit:gw_01:ak_abc)
-- ARGV[1]: 限制阈值 (次数)
-- ARGV[2]: 窗口时长 (秒)

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

-- 获取当前已访问次数
local current = redis.call('get', key)

if current and tonumber(current) >= limit then
    -- 超过阈值，返回 0 触发限流
    return 0
else
    -- 未超过阈值，计数器自开
    local res = redis.call('incr', key)
    if tonumber(res) == 1 then
        -- 如果是第一次访问，设置过期时间
        redis.call('expire', key, window)
    end
    -- 返回 1 允许放行
    return 1
end