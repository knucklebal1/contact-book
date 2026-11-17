package com.contactbook.dto;

import com.contactbook.entity.MethodType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MethodRequest(
        @NotNull(message = "联系方式类型不能为空")
        MethodType methodType,

        @Size(max = 32, message = "自定义平台名称不能超过 32 个字符")
        String customLabel,

        @NotNull(message = "联系方式内容不能为空")
        @Size(max = 500, message = "联系方式不能超过 500 个字符")
        String methodValue,

        @Min(value = 0, message = "排序值不能小于 0")
        Integer sortOrder
) {
}
