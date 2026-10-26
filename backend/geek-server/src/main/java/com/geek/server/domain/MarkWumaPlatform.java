package com.geek.server.domain;

/** Independent platforms fulfilled manually without verification codes or upstream queries. */
public enum MarkWumaPlatform {
    TAIDIXIONG("wuma_taidixiong", "Taidixiong无码"),
    TENGXUN("wuma_tengxun", "Tengxun无码"),
    QIHU360("wuma_360", "360无码");

    private final String code;
    private final String displayName;

    MarkWumaPlatform(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }

    public static boolean contains(String code) {
        if (code == null) return false;
        for (MarkWumaPlatform platform : values()) {
            if (platform.code.equalsIgnoreCase(code.trim())) return true;
        }
        return false;
    }
}
