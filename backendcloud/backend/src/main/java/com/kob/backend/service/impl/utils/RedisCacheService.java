package com.kob.backend.service.impl.utils;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.pojo.BotEvaluationReport;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
public class RedisCacheService {
    private static final Duration ONLINE_TTL = Duration.ofSeconds(120);
    private static final Duration LATEST_REPORT_TTL = Duration.ofMinutes(30);

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void markOnline(User user) {
        if (user == null) return;
        JSONObject value = new JSONObject();
        value.put("userId", user.getId());
        value.put("username", user.getUsername());
        value.put("photo", user.getPhoto());
        value.put("connectedAt", new Date());
        safeSet(RedisKey.onlineUser(user.getId()), value.toJSONString(), ONLINE_TTL);
    }

    public void markOffline(Integer userId) {
        if (userId == null) return;
        safeDelete(RedisKey.onlineUser(userId));
    }

    public boolean isOnline(Integer userId) {
        if (userId == null) return false;
        try {
            Boolean exists = redisTemplate.hasKey(RedisKey.onlineUser(userId));
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            return false;
        }
    }

    public void cacheLatestEvaluationReport(Integer userId, Integer reportId) {
        if (userId == null || reportId == null) return;
        safeSet(RedisKey.latestBotEvaluationReport(userId), reportId.toString(), LATEST_REPORT_TTL);
    }

    public Integer getLatestEvaluationReportId(Integer userId) {
        if (userId == null) return null;
        try {
            String value = redisTemplate.opsForValue().get(RedisKey.latestBotEvaluationReport(userId));
            if (value == null || value.isBlank()) return null;
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void safeSet(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ignored) {
        }
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }
}
