package com.plating.erp.base.vo;

import jakarta.validation.constraints.NotBlank;

public class MenuVo {
    public record MenuCreateReq(@NotBlank(message = "menuName不能为空") String menuName, @NotBlank(message = "path不能为空") String path) {
    }

    public record MenuUpdateReq(@NotBlank(message = "menuName不能为空") String menuName, @NotBlank(message = "path不能为空") String path) {
    }
}
