package com.kob.backend.service.bot.evaluation;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

public interface BotEvaluationService {
    JSONObject runEvaluation(Map<String, String> data);

    JSONObject analyzeWithDeepSeek(Map<String, String> data);

    JSONObject getLatestReport(Map<String, String> data);
}
