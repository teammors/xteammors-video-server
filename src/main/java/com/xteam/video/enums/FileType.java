package com.xteam.video.enums;

public enum FileType {
    IMAGE("image", "图片"),
    VIDEO("video", "视频");

    private final String code;
    private final String desc;

    FileType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FileType fromCode(String code) {
        for (FileType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return IMAGE;
    }
}