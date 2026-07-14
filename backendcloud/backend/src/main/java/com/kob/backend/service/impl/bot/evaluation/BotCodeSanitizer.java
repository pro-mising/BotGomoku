package com.kob.backend.service.impl.bot.evaluation;

public class BotCodeSanitizer {
    private BotCodeSanitizer() {
    }

    public static String sanitize(String code) {
        if (code == null) return null;
        String sanitized = decodeHtmlEntities(code);
        sanitized = sanitized.replaceAll("(?i)</?span[^>]*>", "");
        sanitized = sanitized.replaceAll("(?i)\\s*class=(\"|')code-[^\"']+\\1>", "");
        sanitized = sanitized.replaceAll("(?i)\\s*class=code-[^\\s>]+>", "");
        return sanitized;
    }

    private static String decodeHtmlEntities(String code) {
        return code
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&");
    }
}
