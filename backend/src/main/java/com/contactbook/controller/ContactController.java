package com.contactbook.controller;

import com.contactbook.common.ApiResponse;
import com.contactbook.common.PageResponse;
import com.contactbook.dto.ContactRequest;
import com.contactbook.dto.ContactResponse;
import com.contactbook.dto.FavoriteRequest;
import com.contactbook.dto.ImportResult;
import com.contactbook.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/contacts")
@Tag(name = "联系人管理", description = "联系人、收藏和 Excel 导入导出接口")
public class ContactController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "分页查询联系人")
    public ApiResponse<PageResponse<ContactResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.ok(contactService.list(keyword, isFavorite, page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询联系人详情")
    public ApiResponse<ContactResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(contactService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增联系人")
    public ResponseEntity<ApiResponse<ContactResponse>> create(
            @Valid @RequestBody ContactRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(contactService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑联系人及其联系方式")
    public ApiResponse<ContactResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ContactRequest request
    ) {
        return ApiResponse.ok(contactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除联系人")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/favorite")
    @Operation(summary = "设置或取消收藏")
    public ApiResponse<ContactResponse> setFavorite(
            @PathVariable Long id,
            @Valid @RequestBody FavoriteRequest request
    ) {
        return ApiResponse.ok(contactService.setFavorite(id, request.isFavorite()));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "从 Excel 导入联系人")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(contactService.importExcel(file));
    }

    @GetMapping("/export")
    @Operation(summary = "导出联系人到 Excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isFavorite
    ) {
        byte[] file = contactService.exportExcel(keyword, isFavorite);
        return fileResponse(file, "联系人导出.xlsx");
    }

    @GetMapping("/import-template")
    @Operation(summary = "下载 Excel 导入模板")
    public ResponseEntity<byte[]> downloadTemplate() {
        return fileResponse(contactService.createImportTemplate(), "联系人导入模板.xlsx");
    }

    private ResponseEntity<byte[]> fileResponse(byte[] content, String filename) {
        String disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(XLSX_MEDIA_TYPE)
                .contentLength(content.length)
                .body(content);
    }
}
