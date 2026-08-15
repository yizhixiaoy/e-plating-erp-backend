package com.plating.erp.common.api.response;

import java.util.List;

public record PageResult<T>(List<T> records, long total) {
}
