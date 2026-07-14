package com.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("bot_evaluation_report")
public class BotEvaluationReport {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer botId;
    private String botName;
    private String mode;
    private String reportJson;
    private String analysisStatus;
    private String analysisFindings;
    private String analysisWeaknesses;
    private String analysisSuggestions;
    private String optimizedCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createtime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date modifytime;
}
