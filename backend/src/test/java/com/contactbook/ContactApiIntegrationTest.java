package com.contactbook;

import com.contactbook.repository.ContactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void cleanDatabase() {
        contactRepository.deleteAll();
    }

    @Test
    void shouldCreateFavoriteAndSearchContactWithMultipleMethods() throws Exception {
        String request = """
                {
                  "name": "张三",
                  "company": "示例公司",
                  "jobTitle": "工程师",
                  "note": "重要客户",
                  "isFavorite": false,
                  "methods": [
                    {"methodType": "PHONE", "methodValue": "13800000000", "sortOrder": 0},
                    {"methodType": "PHONE", "methodValue": "13900000000", "sortOrder": 1},
                    {"methodType": "EMAIL", "methodValue": "zhangsan@example.com", "sortOrder": 2}
                  ]
                }
                """;

        MvcResult created = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("张三"))
                .andExpect(jsonPath("$.data.isFavorite").value(false))
                .andExpect(jsonPath("$.data.methods.length()").value(3))
                .andReturn();

        JsonNode response = objectMapper.readTree(created.getResponse().getContentAsString());
        long id = response.path("data").path("id").asLong();

        mockMvc.perform(patch("/api/contacts/{id}/favorite", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isFavorite\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite").value(true));

        mockMvc.perform(get("/api/contacts")
                        .param("keyword", "13800000000")
                        .param("isFavorite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].methods.length()").value(3));
    }

    @Test
    void shouldImportAndExportExcel() throws Exception {
        MockMultipartFile excel = new MockMultipartFile(
                "file",
                "contacts.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createExcel()
        );

        mockMvc.perform(multipart("/api/contacts/import").file(excel))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.success").value(1))
                .andExpect(jsonPath("$.data.failed").value(0));

        MvcResult exported = mockMvc.perform(get("/api/contacts/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        assertThat(exported.getResponse().getContentAsByteArray()).isNotEmpty();
        assertThat(exported.getResponse().getHeader("Content-Disposition"))
                .contains("attachment");
    }

    @Test
    void shouldReturnNotFoundForMissingContact() throws Exception {
        mockMvc.perform(get("/api/contacts/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(404));
    }

    private byte[] createExcel() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("联系人");
            String[] headers = {
                    "姓名", "公司", "职位", "备注", "收藏",
                    "电话", "邮箱", "微信", "QQ", "社交账号", "地址", "其他"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row row = sheet.createRow(1);
            String[] values = {
                    "李四", "测试公司", "产品经理", "Excel 导入", "是",
                    "13700000000\n13600000000", "lisi@example.com", "lisi_wx", "10001",
                    "微博:lisi", "福州市", ""
            };
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
