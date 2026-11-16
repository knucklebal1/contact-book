package com.contactbook.dto;

import com.contactbook.entity.ContactMethod;
import com.contactbook.entity.MethodType;

public record MethodResponse(
        Long id,
        MethodType methodType,
        String customLabel,
        String methodValue,
        int sortOrder
) {

    public static MethodResponse from(ContactMethod method) {
        return new MethodResponse(
                method.getId(),
                method.getMethodType(),
                method.getCustomLabel(),
                method.getMethodValue(),
                method.getSortOrder()
        );
    }
}
