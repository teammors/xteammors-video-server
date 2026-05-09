package com.xteam.video.enums;

public enum ThirdPartyService {
    SEEDANCE_2_0("seedance_2_0", "Seedance 2.0"),
    VIDU_2_0("vidu_2_0", "Vidu 2.0"),
    PIXVERSE("pixverse", "PixVerse"),
    VEO_3_0("veo_3_0", "Veo 3.0"),
    HAPPY_HORSE("happy_horse", "HappyHorse");

    private final String code;
    private final String desc;

    ThirdPartyService(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ThirdPartyService fromCode(String code) {
        for (ThirdPartyService service : values()) {
            if (service.code.equals(code)) {
                return service;
            }
        }
        return SEEDANCE_2_0;
    }
}