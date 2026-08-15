package com.plating.erp.biz.enums;

import lombok.Getter;

/**
 * 货物节点状态枚举
 * 状态流转：PENDING → PROCESSING → PROCESSED → COMPLETED
 *
 * @author Plating ERP Team
 */
@Getter
public enum GoodsNodeStatusEnum {

    PENDING("PENDING", "待处理", 0),
    PROCESSING("PROCESSING", "加工中", 1),
    PROCESSED("PROCESSED", "加工完成", 2),
    COMPLETED("COMPLETED", "完成", 3),
    ROLLED_BACK("ROLLED_BACK", "已回退", 4);

    private final String code;
    private final String label;
    private final int order;

    GoodsNodeStatusEnum(String code, String label, int order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    /**
     * 判断目标状态是否可以推进到下一个状态
     */
    public boolean canTransitionTo(GoodsNodeStatusEnum target) {
        return this.order < target.order;
    }

    public static GoodsNodeStatusEnum fromCode(String code) {
        for (GoodsNodeStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
