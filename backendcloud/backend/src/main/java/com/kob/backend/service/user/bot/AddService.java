package com.kob.backend.service.user.bot;

import org.apache.tomcat.util.codec.binary.StringUtils;

import java.util.Map;

public interface AddService {
    Map<String, String> add(Map<String, String> data);
}
