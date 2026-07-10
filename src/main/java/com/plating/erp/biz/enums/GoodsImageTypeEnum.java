package com.plating.erp.biz.enums;

import lombok.Getter;

/**
 * 货物图片类型枚举
 *
 * @author Plating ERP Team
 */
@Getter
public enum GoodsImageTypeEnum {

    SAMPLE("SAMPLE", "样品照"),
    BEFORE_PROCESS("BEFORE_PROCESS", "加工前"),
    AFTER_PROCESS("AFTER_PROCESS", "加工后");

    private final String code;
    private final String label;

    GoodsImageTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static GoodsImageTypeEnum fromCode(String code) {
        for (GoodsImageTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
