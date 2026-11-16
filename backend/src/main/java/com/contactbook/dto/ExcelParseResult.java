package com.contactbook.dto;

import java.util.List;

public record ExcelParseResult(
        int total,
        List<ParsedContactRow> rows,
        List<ImportError> errors
) {
}
