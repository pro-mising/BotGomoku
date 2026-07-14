package com.kob.backend.service.impl.bot.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekAnalysisMessage {
    private Integer reportId;
    private Integer botId;
}
