package com.plating.erp.biz.enums;

import lombok.Getter;

/**
 * 货物开单状态枚举
 *
 * @author Plating ERP Team
 */
@Getter
public enum GoodsOrderStatusEnum {

    DRAFT("DRAFT", "草稿"),
    PENDING_DISTRIBUTE("PENDING_DISTRIBUTE", "待分发"),
    PROCESSING("PROCESSING", "加工中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String label;

    GoodsOrderStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static GoodsOrderStatusEnum fromCode(String code) {
        for (GoodsOrderStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
