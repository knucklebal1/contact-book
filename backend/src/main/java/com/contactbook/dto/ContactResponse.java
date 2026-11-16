package com.contactbook.dto;

import com.contactbook.entity.Contact;

import java.time.LocalDateTime;
import java.util.List;

public record ContactResponse(
        Long id,
        String name,
        String company,
        String jobTitle,
        String note,
        boolean isFavorite,
        List<MethodResponse> methods,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getCompany(),
                contact.getJobTitle(),
                contact.getNote(),
                contact.isFavorite(),
                contact.getMethods().stream().map(MethodResponse::from).toList(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
