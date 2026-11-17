package com.contactbook.service;

import com.contactbook.common.BusinessException;
import com.contactbook.dto.ContactRequest;
import com.contactbook.dto.ExcelParseResult;
import com.contactbook.dto.ImportError;
import com.contactbook.dto.MethodRequest;
import com.contactbook.dto.ParsedContactRow;
import com.contactbook.entity.Contact;
import com.contactbook.entity.ContactMethod;
import com.contactbook.entity.MethodType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final List<String> HEADERS = List.of(
            "姓名", "公司", "职位", "备注", "收藏",
            "电话", "邮箱", "微信", "QQ", "社交账号", "地址", "其他"
    );
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public ExcelParseResult parse(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BusinessException("Excel 中没有工作表");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            validateHeaders(headerRow);

            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            List<ParsedContactRow> rows = new ArrayList<>();
            List<ImportError> errors = new ArrayList<>();
            int total = 0;

            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }
                total++;
                try {
                    ContactRequest request = parseRow(row, formatter);
                    rows.add(new ParsedContactRow(rowIndex + 1, request));
                } catch (BusinessException exception) {
                    errors.add(new ImportError(rowIndex + 1, exception.getMessage()));
                } catch (RuntimeException exception) {
                    errors.add(new ImportError(rowIndex + 1, "该行数据格式不正确"));
                }
            }

            return new ExcelParseResult(total, rows, errors);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException("Excel 文件损坏或格式不正确");
        }
    }

    public byte[] export(List<Contact> contacts) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = createContactSheet(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);

            int rowIndex = 1;
            for (Contact contact : contacts) {
                Row row = sheet.createRow(rowIndex++);
                Map<MethodType, List<String>> groupedMethods = groupMethods(contact);

                setText(row, 0, contact.getName(), bodyStyle);
                setText(row, 1, contact.getCompany(), bodyStyle);
                setText(row, 2, contact.getJobTitle(), bodyStyle);
                setText(row, 3, contact.getNote(), bodyStyle);
                setText(row, 4, contact.isFavorite() ? "是" : "否", bodyStyle);
                setText(row, 5, join(groupedMethods.get(MethodType.PHONE)), bodyStyle);
                setText(row, 6, join(groupedMethods.get(MethodType.EMAIL)), bodyStyle);
                setText(row, 7, join(groupedMethods.get(MethodType.WECHAT)), bodyStyle);
                setText(row, 8, join(groupedMethods.get(MethodType.QQ)), bodyStyle);
                setText(row, 9, join(groupedMethods.get(MethodType.SOCIAL)), bodyStyle);
                setText(row, 10, join(groupedMethods.get(MethodType.ADDRESS)), bodyStyle);
                setText(row, 11, join(groupedMethods.get(MethodType.OTHER)), bodyStyle);
            }

            return write(workbook);
        } catch (IOException exception) {
            throw new BusinessException("生成 Excel 文件失败");
        }
    }

    public byte[] createTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            createContactSheet(workbook);

            Sheet instructions = workbook.createSheet("导入说明");
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[][] content = {
                    {"字段", "填写说明"},
                    {"姓名", "必填，最多 100 个字符"},
                    {"收藏", "填写“是”或“否”"},
                    {"联系方式", "同一类型的多个值可在单元格内换行，或使用分号分隔"},
                    {"社交账号", "可写成“微博:账号名”或“小红书:账号名”"},
                    {"邮箱", "填写邮箱时必须符合 xxx@example.com 格式"},
                    {"地址", "每个地址可单独占一行"},
                    {"注意", "请勿修改“联系人”工作表中的表头"}
            };

            for (int i = 0; i < content.length; i++) {
                Row row = instructions.createRow(i);
                for (int j = 0; j < content[i].length; j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(content[i][j]);
                    if (i == 0) {
                        cell.setCellStyle(titleStyle);
                    }
                }
            }
            instructions.setColumnWidth(0, 18 * 256);
            instructions.setColumnWidth(1, 65 * 256);

            return write(workbook);
        } catch (IOException exception) {
            throw new BusinessException("生成导入模板失败");
        }
    }

    private Sheet createContactSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("联系人");
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS.get(i));
            cell.setCellStyle(headerStyle);
        }

        int[] widths = {16, 20, 16, 28, 10, 22, 26, 18, 16, 24, 32, 24};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, 0, 0, HEADERS.size() - 1
        ));
        return sheet;
    }

    private ContactRequest parseRow(Row row, DataFormatter formatter) {
        String name = requiredLimited(cell(row, 0, formatter), "姓名", 100);
        String company = limited(cell(row, 1, formatter), "公司", 150);
        String jobTitle = limited(cell(row, 2, formatter), "职位", 100);
        String note = limited(cell(row, 3, formatter), "备注", 500);
        boolean favorite = parseFavorite(cell(row, 4, formatter));

        List<MethodRequest> methods = new ArrayList<>();
        addMethods(methods, MethodType.PHONE, cell(row, 5, formatter), null);
        addEmailMethods(methods, cell(row, 6, formatter));
        addMethods(methods, MethodType.WECHAT, cell(row, 7, formatter), null);
        addMethods(methods, MethodType.QQ, cell(row, 8, formatter), null);
        addLabeledMethods(methods, MethodType.SOCIAL, cell(row, 9, formatter));
        addMethods(methods, MethodType.ADDRESS, cell(row, 10, formatter), null);
        addLabeledMethods(methods, MethodType.OTHER, cell(row, 11, formatter));

        if (methods.size() > 50) {
            throw new BusinessException("每个联系人最多保存 50 个联系方式");
        }

        return new ContactRequest(name, company, jobTitle, note, favorite, methods);
    }

    private void addEmailMethods(List<MethodRequest> methods, String value) {
        for (String email : splitValues(value)) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new BusinessException("邮箱格式不正确：" + email);
            }
            addMethod(methods, MethodType.EMAIL, null, email);
        }
    }

    private void addLabeledMethods(List<MethodRequest> methods, MethodType type, String value) {
        for (String item : splitValues(value)) {
            String label = null;
            String methodValue = item;
            int separator = firstSeparator(item);
            if (separator > 0 && separator < item.length() - 1) {
                label = item.substring(0, separator).trim();
                methodValue = item.substring(separator + 1).trim();
            }
            if (label != null && label.length() > 32) {
                throw new BusinessException("自定义平台名称不能超过 32 个字符");
            }
            if (label != null && methodValue.isEmpty()) {
                throw new BusinessException("联系方式内容不能为空");
            }
            addMethod(methods, type, label, methodValue);
        }
    }

    private void addMethods(
            List<MethodRequest> methods,
            MethodType type,
            String rawValue,
            String label
    ) {
        for (String value : splitValues(rawValue)) {
            addMethod(methods, type, label, value);
        }
    }

    private void addMethod(List<MethodRequest> methods, MethodType type, String label, String value) {
        String methodValue = value == null ? null : value.trim();
        if (methodValue == null || methodValue.isEmpty()) {
            throw new BusinessException("联系方式内容不能为空");
        }
        if (methodValue.length() > 500) {
            throw new BusinessException("联系方式不能超过 500 个字符");
        }
        methods.add(new MethodRequest(type, label, methodValue, methods.size()));
    }

    private void validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException("Excel 缺少表头");
        }
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        for (int i = 0; i < HEADERS.size(); i++) {
            String actual = normalizeHeader(cell(headerRow, i, formatter));
            if (!HEADERS.get(i).equals(actual)) {
                throw new BusinessException(
                        "Excel 表头不正确，第 " + (i + 1) + " 列应为“" + HEADERS.get(i) + "”"
                );
            }
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < HEADERS.size(); i++) {
            if (cell(row, i, formatter) != null) {
                return false;
            }
        }
        return true;
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean parseFavorite(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (List.of("是", "true", "1", "yes", "y").contains(normalized)) {
            return true;
        }
        if (List.of("否", "false", "0", "no", "n").contains(normalized)) {
            return false;
        }
        throw new BusinessException("收藏字段只能填写“是”或“否”");
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\R|[;；]"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private int firstSeparator(String value) {
        int chinese = value.indexOf('：');
        int english = value.indexOf(':');
        if (chinese < 0) {
            return english;
        }
        if (english < 0) {
            return chinese;
        }
        return Math.min(chinese, english);
    }

    private String requiredLimited(String value, String fieldName, int maxLength) {
        String normalized = limited(value, fieldName, maxLength);
        if (normalized == null) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String limited(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }

    private Map<MethodType, List<String>> groupMethods(Contact contact) {
        Map<MethodType, List<String>> grouped = new EnumMap<>(MethodType.class);
        for (MethodType type : MethodType.values()) {
            grouped.put(type, new ArrayList<>());
        }
        for (ContactMethod method : contact.getMethods()) {
            String value = formatMethod(method);
            grouped.computeIfAbsent(method.getMethodType(), ignored -> new ArrayList<>()).add(value);
        }
        return grouped;
    }

    private String formatMethod(ContactMethod method) {
        if ((method.getMethodType() == MethodType.SOCIAL || method.getMethodType() == MethodType.OTHER)
                && method.getCustomLabel() != null
                && !method.getCustomLabel().isBlank()) {
            return method.getCustomLabel() + ":" + method.getMethodValue();
        }
        return method.getMethodValue();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().collect(Collectors.joining("\n"));
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private void setText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private byte[] write(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
