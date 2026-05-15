package com.plating.erp.iam.vo;

import java.util.List;

public class RoleResponseVo {
    public record RoleMenusResponse(Long roleId, List<Long> menuIds) {
    }
}
