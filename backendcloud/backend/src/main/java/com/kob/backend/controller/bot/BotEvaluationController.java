package com.kob.backend.controller.bot;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.service.bot.evaluation.BotEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BotEvaluationController {
    @Autowired
    private BotEvaluationService botEvaluationService;

    @PostMapping("/bot/evaluation/run/")
    public JSONObject runEvaluation(@RequestParam Map<String, String> data) {
        return botEvaluationService.runEvaluation(data);
    }

    @PostMapping("/bot/evaluation/deepseek/")
    public JSONObject analyzeWithDeepSeek(@RequestParam Map<String, String> data) {
        return botEvaluationService.analyzeWithDeepSeek(data);
    }

    @GetMapping("/bot/evaluation/latest/")
    public JSONObject getLatestReport(@RequestParam Map<String, String> data) {
        return botEvaluationService.getLatestReport(data);
    }
}
