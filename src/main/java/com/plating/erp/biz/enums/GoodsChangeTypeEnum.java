package com.plating.erp.biz.enums;

import lombok.Getter;

/**
 * 变更类型枚举
 *
 * @author Plating ERP Team
 */
@Getter
public enum GoodsChangeTypeEnum {

    ORDER_CREATE("ORDER_CREATE", "开单创建"),
    ORDER_UPDATE("ORDER_UPDATE", "开单更新"),
    NODE_UPDATE("NODE_UPDATE", "节点更新"),
    NODE_STATUS_CHANGE("NODE_STATUS_CHANGE", "节点状态变更"),
    STATUS_AUTO_UPDATE("STATUS_AUTO_UPDATE", "开单状态自动更新"),
    ROLLBACK("ROLLBACK", "节点回退");

    private final String code;
    private final String label;

    GoodsChangeTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static GoodsChangeTypeEnum fromCode(String code) {
        for (GoodsChangeTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
