-- [[
--  KEYS: [1] UserKey (중복 투표 확인용), [2] CountKey (투표 합계 Hash), [3] DirtySetKey (변경된 대상 세션 Set)
--  ARGV: [1] Value (투표 증명값/타임스탬프), [2] TTL (중복 방지 키 만료 시간), [3] OptionIndex (선택한 투표 옵션), [4] SessionId (세션 식별자)
--  Return: 1 (성공), 0 (중복 투표로 인한 실패)
-- ]]

if redis.call('SETNX', KEYS[1], ARGV[1]) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
    redis.call('HINCRBY', KEYS[2], ARGV[3], 1)
    redis.call('SADD', KEYS[3], ARGV[4])
    return 1
else
    return 0
end