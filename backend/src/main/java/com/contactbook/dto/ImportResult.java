package com.contactbook.dto;

import java.util.List;

public record ImportResult(
        int total,
        int success,
        int failed,
        List<ImportError> errors
) {
}
