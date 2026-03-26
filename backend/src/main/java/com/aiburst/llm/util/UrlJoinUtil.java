package com.aiburst.llm.util;

public final class UrlJoinUtil {

    private UrlJoinUtil() {
    }

    public static String join(String base, String path) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        if (path == null || path.isEmpty()) {
            return b;
        }
        String p = path.startsWith("/") ? path : "/" + path;
        return b + p;
    }
}
