-- KEYS: user vote, counts, dirty sessions, closed marker, revision
-- ARGV: session ID, option index, start/end epoch milliseconds, option count, DB counts...
local option = tonumber(ARGV[2])
local optionCount = tonumber(ARGV[5])
if not option or option ~= math.floor(option) or option < 0 or option >= optionCount then
    return -2
end
if redis.call('EXISTS', KEYS[4]) == 1 then
    return -1
end

-- Recheck the deadline at the atomic write, including requests delayed after Java validation.
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local endTime = tonumber(ARGV[4])
if now < tonumber(ARGV[3]) or now >= endTime then
    return -3
end
if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end

if redis.call('EXISTS', KEYS[2]) == 0 then
    for i = 0, optionCount - 1 do
        redis.call('HSET', KEYS[2], tostring(i), ARGV[6 + i])
    end
end
redis.call('HINCRBY', KEYS[2], ARGV[2], 1)
redis.call('INCR', KEYS[5])
redis.call('SET', KEYS[1], tostring(now), 'PXAT', endTime + 86400000)
redis.call('SADD', KEYS[3], ARGV[1])
-- Unsaved counts must survive a prolonged DB outage instead of expiring before commit.
redis.call('PERSIST', KEYS[2])
redis.call('PERSIST', KEYS[5])
return 1
