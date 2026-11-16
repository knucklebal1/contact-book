package com.contactbook.service;

import com.contactbook.common.BusinessException;
import com.contactbook.common.NotFoundException;
import com.contactbook.common.PageResponse;
import com.contactbook.dto.ContactRequest;
import com.contactbook.dto.ContactResponse;
import com.contactbook.dto.ExcelParseResult;
import com.contactbook.dto.ImportError;
import com.contactbook.dto.ImportResult;
import com.contactbook.dto.MethodRequest;
import com.contactbook.dto.ParsedContactRow;
import com.contactbook.entity.Contact;
import com.contactbook.entity.ContactMethod;
import com.contactbook.repository.ContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ContactService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_IMPORT_SIZE = 5L * 1024 * 1024;

    private final ContactRepository contactRepository;
    private final ExcelService excelService;

    public ContactService(ContactRepository contactRepository, ExcelService excelService) {
        this.contactRepository = contactRepository;
        this.excelService = excelService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> list(
            String keyword,
            Boolean favorite,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(
                safePage - 1,
                safePageSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        Page<Contact> contacts = contactRepository.search(normalizeKeyword(keyword), favorite, pageRequest);
        return PageResponse.from(contacts, ContactResponse::from);
    }

    @Transactional(readOnly = true)
    public ContactResponse get(Long id) {
        return ContactResponse.from(getEntity(id));
    }

    @Transactional
    public ContactResponse create(ContactRequest request) {
        Contact contact = new Contact();
        applyRequest(contact, request);
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = getEntity(id);
        applyRequest(contact, request);
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public void delete(Long id) {
        Contact contact = getEntity(id);
        contactRepository.delete(contact);
    }

    @Transactional
    public ContactResponse setFavorite(Long id, boolean favorite) {
        Contact contact = getEntity(id);
        contact.setFavorite(favorite);
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public ImportResult importExcel(MultipartFile file) {
        validateImportFile(file);

        ExcelParseResult parsed;
        try (InputStream inputStream = file.getInputStream()) {
            parsed = excelService.parse(inputStream);
        } catch (IOException exception) {
            throw new BusinessException("无法读取 Excel 文件");
        }

        List<ImportError> errors = new ArrayList<>(parsed.errors());
        int success = 0;
        for (ParsedContactRow row : parsed.rows()) {
            try {
                Contact contact = new Contact();
                applyRequest(contact, row.contact());
                contactRepository.save(contact);
                success++;
            } catch (BusinessException exception) {
                errors.add(new ImportError(row.rowNumber(), exception.getMessage()));
            }
        }

        errors.sort(Comparator.comparingInt(ImportError::row));
        return new ImportResult(parsed.total(), success, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(String keyword, Boolean favorite) {
        List<Contact> contacts = contactRepository.searchAll(normalizeKeyword(keyword), favorite);
        return excelService.export(contacts);
    }

    public byte[] createImportTemplate() {
        return excelService.createTemplate();
    }

    private Contact getEntity(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("联系人不存在"));
    }

    private void applyRequest(Contact contact, ContactRequest request) {
        String name = trimToNull(request.name());
        if (name == null) {
            throw new BusinessException("姓名不能为空");
        }

        contact.setName(name);
        contact.setCompany(trimToNull(request.company()));
        contact.setJobTitle(trimToNull(request.jobTitle()));
        contact.setNote(trimToNull(request.note()));
        contact.setFavorite(Boolean.TRUE.equals(request.isFavorite()));

        contact.clearMethods();
        if (request.methods() != null) {
            int defaultOrder = 0;
            for (MethodRequest methodRequest : request.methods()) {
                if (methodRequest.methodType() == null) {
                    throw new BusinessException("联系方式类型不能为空");
                }
                String value = trimToNull(methodRequest.methodValue());
                if (value == null) {
                    throw new BusinessException("联系方式内容不能为空");
                }

                ContactMethod method = new ContactMethod();
                method.setMethodType(methodRequest.methodType());
                method.setCustomLabel(trimToNull(methodRequest.customLabel()));
                method.setMethodValue(value);
                method.setSortOrder(methodRequest.sortOrder() == null
                        ? defaultOrder
                        : methodRequest.sortOrder());
                contact.addMethod(method);
                defaultOrder++;
            }
        }
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        if (file.getSize() > MAX_IMPORT_SIZE) {
            throw new BusinessException("Excel 文件不能超过 5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("只支持 .xlsx 格式的 Excel 文件");
        }
    }

    private String normalizeKeyword(String keyword) {
        return trimToNull(keyword);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
