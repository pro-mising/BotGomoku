package com.kob.backend.service.user.account;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface InfoService {
    public Map<String, String> getinfo();

    public Map<String, String> getOnlineStatus();

    JSONObject getProfileOverview();

    JSONObject updateAvatar(MultipartFile file);
}
