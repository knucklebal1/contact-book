package com.contactbook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContactRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "姓名不能超过 100 个字符")
        String name,

        @Size(max = 150, message = "公司不能超过 150 个字符")
        String company,

        @Size(max = 100, message = "职位不能超过 100 个字符")
        String jobTitle,

        @Size(max = 500, message = "备注不能超过 500 个字符")
        String note,

        Boolean isFavorite,

        @Size(max = 50, message = "每个联系人最多保存 50 个联系方式")
        List<@Valid MethodRequest> methods
) {
}
