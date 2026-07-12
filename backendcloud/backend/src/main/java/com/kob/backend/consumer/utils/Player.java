package com.kob.backend.consumer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer id;
    private Integer botId;
    private String botCode;
    private Integer sx;
    private Integer sy;
    private List<Integer> steps;

    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) res.append(',');
            res.append(steps.get(i));
        }
        return res.toString();
    }
}
