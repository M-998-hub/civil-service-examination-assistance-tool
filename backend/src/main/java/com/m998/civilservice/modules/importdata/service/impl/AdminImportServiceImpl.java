package com.m998.civilservice.modules.importdata.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.m998.civilservice.modules.importdata.dto.*;
import com.m998.civilservice.modules.importdata.model.ImportTemplate;
import com.m998.civilservice.modules.importdata.service.AdminImportService;
import com.m998.civilservice.modules.importdata.service.ImportTemplateService;
import com.m998.civilservice.modules.importdata.strategy.ImportStrategy;
import com.m998.civilservice.modules.importdata.strategy.ImportTypeEnum;
import com.m998.civilservice.common.api.ResultCode;
import com.m998.civilservice.common.exception.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理端数据导入服务实现
 * 使用策略模式支持多种导入类型
 */
@Service
public class AdminImportServiceImpl implements AdminImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminImportServiceImpl.class);

    @Value("${import.temp-path:./temp/import/}")
    private String tempPath;

    @Autowired
    private ImportTemplateService templateService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 类型 → 策略 的索引Map（通过 ApplicationContext 收集，避免泛型通配符注入问题）
     */
    private Map<ImportTypeEnum, ImportStrategy<?>> strategyMap;

    /**
     * 会话信息缓存
     */
    private final Map<String, SessionInfo> sessionCache = new ConcurrentHashMap<>();

    // 保证该方法在bean初始化后自动执行
    @PostConstruct
    // 忽视泛型警告
    @SuppressWarnings("rawtypes")
    public void init() {
        FileUtil.mkdir(tempPath);
        // 使用 ApplicationContext 收集所有 ImportStrategy bean，避免 List<ImportStrategy<?>> 泛型注入问题
        Map<String, ImportStrategy> beans = applicationContext.getBeansOfType(ImportStrategy.class);
        strategyMap = new HashMap<>();
        for (ImportStrategy<?> strategy : beans.values()) {
            strategyMap.put(strategy.getImportType(), strategy);
        }
        LOGGER.info("已注册导入策略: {}", strategyMap.keySet());
    }

    /**
     * 获取指定类型的策略
     */
    public ImportStrategy<?> getStrategy(ImportTypeEnum importType) {
        ImportStrategy<?> strategy = strategyMap.get(importType);
        if (strategy == null) {
            Asserts.fail("不支持的导入类型: " + importType);
        }
        return strategy;
    }

    /**
     * 获取所有支持的导入类型
     */
    public List<Map<String, String>> getSupportedTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        for (ImportTypeEnum type : ImportTypeEnum.values()) {
            Map<String, String> item = new HashMap<>();
            item.put("code", type.getCode());
            item.put("label", type.getLabel());
            types.add(item);
        }
        return types;
    }

    @Override
    public UploadResult uploadAndPreview(MultipartFile file) throws Exception {
        // 生成会话ID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 保存临时文件
        String filename = sessionId + "_" + file.getOriginalFilename();
        File tempFile = new File(tempPath + filename);
        FileUtil.writeBytes(file.getBytes(), tempFile);

        // 读取Excel预览数据
        UploadResult result = new UploadResult();
        result.setSessionId(sessionId);

        List<List<String>> previewData = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        final int[] totalRows = {0};

        // 使用EasyExcel读取
        EasyExcel.read(tempFile, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
                for (int i = 0; i < headMap.size(); i++) {
                    ReadCellData<?> cellData = headMap.get(i);
                    columns.add(cellData.getStringValue());
                }
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                totalRows[0]++;
                if (previewData.size() < 5) {
                    List<String> row = new ArrayList<>();
                    for (int i = 0; i < columns.size(); i++) {
                        row.add(data.getOrDefault(i, ""));
                    }
                    previewData.add(row);
                }
            }

            // 读完后做什么(此处为空实现)
            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().doRead();

        result.setColumns(columns);
        result.setPreviewData(previewData);
        result.setTotalRows(totalRows[0]);

        // 缓存会话信息
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setFilePath(tempFile.getAbsolutePath());
        sessionInfo.setColumns(columns);
        sessionInfo.setCreateTime(new Date());
        sessionCache.put(sessionId, sessionInfo);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExecuteImportResult executeImport(ExecuteImportParam param, Long userId) throws Exception {
        // 确定导入类型，默认为 POSITION（兼容旧接口）
        ImportTypeEnum importType = param.getImportType() != null
                ? param.getImportType()
                : ImportTypeEnum.POSITION;

        ImportStrategy<?> strategy = getStrategy(importType);
        return doImport(strategy, param, userId);
    }

    /**
     * 通用导入流程模板
     */
    @SuppressWarnings("unchecked")
    private <T> ExecuteImportResult doImport(ImportStrategy<T> strategy, ExecuteImportParam param, Long userId) throws Exception {
        // 获取会话信息
        SessionInfo sessionInfo = sessionCache.get(param.getSessionId());
        if (sessionInfo == null) {
            Asserts.fail(ResultCode.IMPORT_SESSION_EXPIRED);
        }

        File tempFile = new File(sessionInfo.getFilePath());
        if (!tempFile.exists()) {
            Asserts.fail(ResultCode.IMPORT_FILE_INVALID);
        }

        ExecuteImportResult result = new ExecuteImportResult();
        Map<String, Object> mapping = param.getMapping();

        // 构建额外参数
        Map<String, Object> extraParams = new HashMap<>();
        if (StrUtil.isNotBlank(param.getYear())) {
            extraParams.put("year", param.getYear().trim());
        }

        // 获取已存在数据的唯一键用于去重
        Set<String> existingKeys = strategy.getExistingKeys(extraParams);
        Set<String> sessionKeys = new HashSet<>();

        // 批量读取并导入
        List<T> batchList = new ArrayList<>();
        final int[] currentRow = {1};

        EasyExcel.read(tempFile, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                currentRow[0]++;
                String rowData = buildRowData(data, mapping);

                // 构建实体
                T entity = strategy.buildEntity(data, mapping, extraParams);

                // 校验
                String validateMsg = strategy.validate(entity, currentRow[0]);
                if (validateMsg != null) {
                    result.addFailDetail(currentRow[0], rowData, validateMsg);
                    result.setFailCount(result.getFailCount() + 1);
                    return;
                }

                // 去重检查
                String key = strategy.getDuplicateKey(entity);
                if (existingKeys.contains(key) || sessionKeys.contains(key)) {
                    result.setSkipCount(result.getSkipCount() + 1);
                    return;
                }

                sessionKeys.add(key);
                batchList.add(entity);

                // 批量插入
                if (batchList.size() >= 1000) {
                    strategy.saveBatch(batchList);
                    result.setSuccessCount(result.getSuccessCount() + batchList.size());
                    batchList.clear();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                if (!batchList.isEmpty()) {
                    strategy.saveBatch(batchList);
                    result.setSuccessCount(result.getSuccessCount() + batchList.size());
                }
            }
        }).sheet().doRead();

        // 保存为模板
        if (Boolean.TRUE.equals(param.getSaveAsTemplate()) && StrUtil.isNotBlank(param.getTemplateName())) {
            saveTemplate(param, userId);
        }

        // 清理会话
        sessionCache.remove(param.getSessionId());

        return result;
    }

    /**
     * 构建行数据字符串（把Excel行数据变成可读字符串的工具方法，用于记录导入失败时的详细错误信息）
     */
    private String buildRowData(Map<Integer, String> data, Map<String, Object> mapping) {
        StringBuilder sb = new StringBuilder();

        // 遍历映射关系
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            Integer index = getMappingIndex(entry.getValue());
            sb.append(entry.getKey()).append(":").append(index != null ? data.get(index) : "");
        }
        return sb.toString();
    }

    // 将映射值转换为整数索引，确保格式统一
    private Integer getMappingIndex(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 保存为模板
     */
    private void saveTemplate(ExecuteImportParam param, Long userId) {
        ImportTemplate template = new ImportTemplate();
        template.setTemplateName(param.getTemplateName());
        template.setColumnMapping(JSONUtil.toJsonStr(param.getMapping()));
        template.setImportType(param.getImportType() != null ? param.getImportType().getCode() : ImportTypeEnum.POSITION.getCode());
        templateService.saveTemplate(template, userId);
    }

    /**
     * 定时清理过期文件（每小时执行）
     */
    @Scheduled(fixedRate = 3600000)
    @Override
    public void cleanExpiredFiles() {
        long expireTime = System.currentTimeMillis() - 3600000;
        Iterator<Map.Entry<String, SessionInfo>> iterator = sessionCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SessionInfo> entry = iterator.next();
            SessionInfo info = entry.getValue();
            if (info.getCreateTime().getTime() < expireTime) {
                FileUtil.del(info.getFilePath());
                iterator.remove();
                LOGGER.info("Cleaned expired session: {}", entry.getKey());
            }
        }
    }

    /**
     * 会话信息
     */
    private static class SessionInfo {
        private String sessionId;
        private String filePath;
        private List<String> columns;
        private Date createTime;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
    }
}
