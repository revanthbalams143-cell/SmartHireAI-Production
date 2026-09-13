package com.smarthire.backend.ai;

public final class AiServiceUrl {
    private AiServiceUrl() {}
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String value = raw.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) return value.replaceAll("/$", "");
        return "http://" + value.replaceAll("/$", "");
    }
}
