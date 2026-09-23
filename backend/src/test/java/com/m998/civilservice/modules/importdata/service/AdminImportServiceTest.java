package com.m998.civilservice.modules.importdata.service;

import com.m998.civilservice.common.exception.ApiException;
import com.m998.civilservice.modules.importdata.dto.ExecuteImportParam;
import com.m998.civilservice.modules.importdata.service.impl.AdminImportServiceImpl;
import com.m998.civilservice.modules.importdata.strategy.ImportStrategy;
import com.m998.civilservice.modules.importdata.strategy.ImportTypeEnum;
import com.m998.civilservice.modules.importdata.strategy.PositionImportStrategy;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("岗位数据导入策略单元测试")
class AdminImportServiceTest {

    @Mock
    private PositionService positionService;
    @Mock
    private ImportTemplateService templateService;
    @InjectMocks
    private PositionImportStrategy strategy;
    @InjectMocks
    private AdminImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        Map<ImportTypeEnum, ImportStrategy<?>> strategies = new HashMap<>();
        strategies.put(ImportTypeEnum.POSITION, strategy);
        ReflectionTestUtils.setField(importService, "strategyMap", strategies);
    }

    @Test
    @DisplayName("根据字段映射构建并规范化岗位实体")
    void buildEntity_normalizesImportedValues() {
        Map<Integer, String> row = new HashMap<>();
        row.put(0, "组织部");
        row.put(1, "科员");
        row.put(2, "本科及以上");
        row.put(3, "无限制");
        row.put(4, "是");
        row.put(5, "3");
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("department", 0);
        mapping.put("positionName", 1);
        mapping.put("educationRequired", 2);
        mapping.put("politicalStatusRequired", 3);
        mapping.put("isFreshOnly", 4);
        mapping.put("recruitmentNumber", 5);

        Position position = strategy.buildEntity(row, mapping, Collections.singletonMap("year", "2026"));

        assertThat(position.getDepartment()).isEqualTo("组织部");
        assertThat(position.getEducationRequired()).isEqualTo("本科");
        assertThat(position.getPoliticalStatusRequired()).isEqualTo("不限");
        assertThat(position.getIsFreshOnly()).isTrue();
        assertThat(position.getRecruitmentNumber()).isEqualTo(3);
        assertThat(position.getYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("必填字段缺失时返回明确校验信息")
    void validate_missingRequiredField_returnsMessage() {
        Position position = validPosition();
        position.setDepartment("");
        assertThat(strategy.validate(position, 2)).contains("部门");
    }

    @Test
    @DisplayName("非法年份和枚举值不能通过校验")
    void validate_invalidValues_returnsMessage() {
        Position invalidYear = validPosition();
        invalidYear.setYear(1999);
        assertThat(strategy.validate(invalidYear, 2)).contains("2000-2100");
        Position invalidEducation = validPosition();
        invalidEducation.setEducationRequired("高中");
        assertThat(strategy.validate(invalidEducation, 2)).contains("学历要求值无效");
    }

    @Test
    @DisplayName("会话不存在时抛出业务异常")
    void executeImport_missingSession_throwsApiException() {
        ExecuteImportParam param = new ExecuteImportParam();
        param.setSessionId("missing-session");
        param.setImportType(ImportTypeEnum.POSITION);
        assertThatThrownBy(() -> importService.executeImport(param, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("导入会话");
    }

    private Position validPosition() {
        Position position = new Position();
        position.setDepartment("组织部");
        position.setPositionName("科员");
        position.setYear(2026);
        position.setEducationRequired("本科");
        position.setPoliticalStatusRequired("中共党员");
        position.setRecruitmentNumber(1);
        return position;
    }
}
