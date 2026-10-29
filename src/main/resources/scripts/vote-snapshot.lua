-- KEYS: counts, revision, closed marker, dirty sessions
-- ARGV: mode (read/close), session ID, original end epoch milliseconds
if ARGV[1] == 'close' then
    if redis.call('EXISTS', KEYS[3]) == 0 then
        local time = redis.call('TIME')
        local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
        local endTime = tonumber(ARGV[3])
        local closedAt = math.min(now, endTime)
        -- Keep the fence through the original deadline to reject stale in-flight requests.
        redis.call('SET', KEYS[3], tostring(closedAt), 'PXAT', math.max(now, endTime) + 86400000)
    end
    redis.call('SADD', KEYS[4], ARGV[2])
end

redis.call('PERSIST', KEYS[1])
redis.call('PERSIST', KEYS[2])
local result = {redis.call('GET', KEYS[2]) or '0', redis.call('GET', KEYS[3]) or ''}
local counts = redis.call('HGETALL', KEYS[1])
for i = 1, #counts do
    result[#result + 1] = counts[i]
end
return result
