-- KEYS: counts, revision, closed marker, dirty sessions
-- ARGV: session ID, committed snapshot revision, finished (1/0)
if (redis.call('GET', KEYS[2]) or '0') ~= ARGV[2] then
    return 0
end

if ARGV[3] == '1' then
    if redis.call('EXISTS', KEYS[3]) == 0 then
        return 0
    end
    redis.call('DEL', KEYS[1], KEYS[2])
else
    -- A closing request must remain pending even if no additional vote changed the revision.
    if redis.call('EXISTS', KEYS[3]) == 1 then
        return 0
    end
end
redis.call('SREM', KEYS[4], ARGV[1])
return 1
