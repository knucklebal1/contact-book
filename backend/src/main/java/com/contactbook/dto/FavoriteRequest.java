package com.contactbook.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(
        @NotNull(message = "收藏状态不能为空")
        Boolean isFavorite
) {
}
