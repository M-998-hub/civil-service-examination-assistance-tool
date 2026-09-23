package com.m998.civilservice.modules.importdata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.common.exception.ApiException;
import com.m998.civilservice.common.api.ResultCode;
import com.m998.civilservice.modules.importdata.dto.ExecuteImportParam;
import com.m998.civilservice.modules.importdata.dto.ExecuteImportResult;
import com.m998.civilservice.modules.importdata.dto.UploadResult;
import com.m998.civilservice.modules.importdata.model.ImportTemplate;
import com.m998.civilservice.modules.importdata.service.AdminImportService;
import com.m998.civilservice.modules.importdata.service.ImportTemplateService;
import com.m998.civilservice.modules.importdata.service.impl.AdminImportServiceImpl;
import com.m998.civilservice.modules.importdata.strategy.ImportStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 导入Controller集成测试
 */
@WebMvcTest(AdminImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminImportController 集成测试")
class AdminImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminImportServiceImpl adminImportService;

    @MockBean
    private ImportTemplateService templateService;

    @MockBean
    private List<ImportStrategy<?>> strategies;

    @Test
    @DisplayName("上传有效Excel - 返回200和预览数据")
    void upload_validExcel_returns200() throws Exception {
        UploadResult uploadResult = new UploadResult();
        uploadResult.setSessionId("test-session-123");
        uploadResult.setTotalRows(10);
        uploadResult.setColumns(Arrays.asList("部门", "职位"));
        uploadResult.setPreviewData(Collections.emptyList());

        when(adminImportService.uploadAndPreview(any())).thenReturn(uploadResult);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test content".getBytes());

        mockMvc.perform(multipart("/admin/import/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("test-session-123"))
                .andExpect(jsonPath("$.data.totalRows").value(10));
    }

    @Test
    @DisplayName("上传空文件 - 返回失败")
    void upload_emptyFile_returnsFailed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        mockMvc.perform(multipart("/admin/import/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("上传文件不能为空"));
    }

    @Test
    @DisplayName("上传非Excel文件 - 返回失败")
    void upload_wrongFormat_returnsFailed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt",
                "text/plain",
                "not an excel".getBytes());

        mockMvc.perform(multipart("/admin/import/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("文件格式不正确，请上传Excel文件（.xlsx或.xls）"));
    }

    @Test
    @DisplayName("执行导入成功 - 返回200和导入结果")
    void execute_validSession_returns200() throws Exception {
        ExecuteImportResult importResult = new ExecuteImportResult();
        importResult.setSuccessCount(10);
        importResult.setFailCount(0);
        importResult.setSkipCount(2);

        when(adminImportService.executeImport(any(ExecuteImportParam.class), isNull()))
                .thenReturn(importResult);

        ExecuteImportParam param = new ExecuteImportParam();
        param.setSessionId("valid-session");
        param.setYear("2026");
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("department", 0);
        mapping.put("positionName", 1);
        param.setMapping(mapping);

        mockMvc.perform(post("/admin/import/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param))
                        .principal(() -> "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successCount").value(10));
    }

    @Test
    @DisplayName("执行导入(会话过期) - 返回错误码1001")
    void execute_expiredSession_returnsFailed() throws Exception {
        when(adminImportService.executeImport(any(ExecuteImportParam.class), isNull()))
                .thenThrow(new ApiException(ResultCode.IMPORT_SESSION_EXPIRED));

        ExecuteImportParam param = new ExecuteImportParam();
        param.setSessionId("expired-session");
        param.setYear("2026");
        param.setMapping(Collections.singletonMap("department", 0));

        mockMvc.perform(post("/admin/import/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(param))
                        .principal(() -> "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
