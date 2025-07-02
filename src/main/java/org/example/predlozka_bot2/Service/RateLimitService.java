package org.example.predlozka_bot2.Service;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean isAllowed(Long userId, int maxRequests, int timeWindowSeconds){
        String key = "rate_limit:" + userId;

        Integer currentValue = (Integer) redisTemplate.opsForValue().get(key);

        if (currentValue == null){
            redisTemplate.opsForValue().set(key,1, Duration.ofSeconds(timeWindowSeconds));
            return true;
        } else if (currentValue >= maxRequests){
            return  false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }

    public Boolean isCallBackAllowed(Long userId, int maxCallback, int timeWindowSeconds){
        String key = "callback_rate_limit:" + userId;

        Integer currentValue = (Integer) redisTemplate.opsForValue().get(key);

        if (currentValue == null){
            redisTemplate.opsForValue().set(key,1, Duration.ofSeconds(timeWindowSeconds));
            return true;
        } else if (currentValue >= maxCallback){
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
