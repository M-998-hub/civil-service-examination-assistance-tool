package com.macro.mall.tiny.modules.ums.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.macro.mall.tiny.modules.ums.dto.*;
import com.macro.mall.tiny.modules.ums.model.ImportTemplate;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.service.AdminImportService;
import com.macro.mall.tiny.modules.ums.service.ImportTemplateService;
import com.macro.mall.tiny.modules.ums.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理端数据导入服务实现
 * Created by macro on 2026-04-03.
 */
@Service
public class AdminImportServiceImpl implements AdminImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminImportServiceImpl.class);

    @Value("${import.temp-path:./temp/import/}")
    private String tempPath;

    @Autowired
    private PositionService positionService;

    @Autowired
    private ImportTemplateService templateService;

    /**
     * 会话信息缓存
     */
    private final Map<String, SessionInfo> sessionCache = new ConcurrentHashMap<>();

    /**
     * 有效的学历要求枚举
     */
    private static final Set<String> VALID_EDUCATION = new HashSet<String>() {{
        add("专科"); add("本科"); add("硕士研究生"); add("博士研究生");
        add("大专"); add("硕士"); add("博士");
    }};

    /**
     * 学历关键词提取配置（按优先级从高到低排序）
     * 博士 > 硕士 > 本科 > 大专
     */
    private static final List<String> EDUCATION_KEYWORDS = new ArrayList<String>() {{
        add("博士");      // 最高优先级
        add("硕士");      // 其次
        add("本科");      // 再次
        add("大专");      // 最低优先级
        add("专科");      // 同大专
    }};

    /**
     * 有效的政治面貌要求枚举
     */
    private static final Set<String> VALID_POLITICAL_STATUS = new HashSet<String>() {{
        add("不限"); add("中共党员"); add("共青团员"); add("民主党派"); add("群众");
    }};

    /**
     * 政治面貌关键词映射配置
     * key: 标准值
     * value: 该标准值对应的所有可能关键词（按优先级排序）
     * 
     * 优先级规则：优先选择要求更低的（从低到高）
     * 不限 < 群众 < 共青团员 < 中共党员
     */
    private static final Map<String, List<String>> POLITICAL_STATUS_KEYWORDS = new LinkedHashMap<String, List<String>>() {{
        // 不限（最低要求，优先匹配）
        put("不限", new ArrayList<String>() {{
            add("不限"); add("无限制"); add("无"); add("不限制");
        }});
        // 群众（较低要求）
        put("群众", new ArrayList<String>() {{
            add("群众");
        }});
        // 团员（较高要求）
        put("共青团员", new ArrayList<String>() {{
            add("共青团员"); add("团员");
        }});
        // 党员（最高要求，最后匹配）
        put("中共党员", new ArrayList<String>() {{
            add("中共党员"); add("中共正式党员"); add("中共预备党员"); add("党员");
        }});
    }};

    @PostConstruct
    public void init() {
        // 创建临时目录
        FileUtil.mkdir(tempPath);
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
                // 读取表头
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
        // 获取会话信息
        SessionInfo sessionInfo = sessionCache.get(param.getSessionId());
        if (sessionInfo == null) {
            throw new RuntimeException("会话不存在或已过期，请重新上传文件");
        }

        File tempFile = new File(sessionInfo.getFilePath());
        if (!tempFile.exists()) {
            throw new RuntimeException("临时文件不存在，请重新上传");
        }

        ExecuteImportResult result = new ExecuteImportResult();
        Map<String, Object> mapping = param.getMapping();
        
        // 解析年份（优先使用专门的year字段，否则从mapping中获取）
        Integer yearValue = null;
        if (StrUtil.isNotBlank(param.getYear())) {
            try {
                yearValue = Integer.parseInt(param.getYear().trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException("年份格式不正确: " + param.getYear());
            }
        }
        final Integer finalYearValue = yearValue;

        // 获取已存在的岗位用于去重
        Set<String> existingKeys = getExistingPositionKeys();
        Set<String> sessionKeys = new HashSet<>();

        // 批量读取并导入
        List<Position> batchList = new ArrayList<>();
        final int[] currentRow = {1}; // 从第2行开始（第1行是表头）

        EasyExcel.read(tempFile, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                currentRow[0]++;
                String rowData = buildRowData(data, mapping);

                // 构建Position对象
                Position position = buildPosition(data, mapping, finalYearValue);

                // 校验数据
                String validateMsg = validatePosition(position, currentRow[0]);
                if (validateMsg != null) {
                    result.addFailDetail(currentRow[0], rowData, validateMsg);
                    result.setFailCount(result.getFailCount() + 1);
                    return;
                }

                // 去重检查
                String key = position.getYear() + "_" + position.getDepartment() + "_" + position.getPositionName();
                if (existingKeys.contains(key) || sessionKeys.contains(key)) {
                    result.setSkipCount(result.getSkipCount() + 1);
                    return;
                }

                sessionKeys.add(key);
                batchList.add(position);

                // 批量插入
                if (batchList.size() >= 1000) {
                    positionService.saveBatch(batchList);
                    result.setSuccessCount(result.getSuccessCount() + batchList.size());
                    batchList.clear();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // 保存剩余数据
                if (!batchList.isEmpty()) {
                    positionService.saveBatch(batchList);
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
     * 构建Position对象
     */
    private Position buildPosition(Map<Integer, String> data, Map<String, Object> mapping, Integer yearValue) {
        Position position = new Position();

        if (mapping.containsKey("department")) {
            position.setDepartment(getValue(data, getMappingIndex(mapping.get("department"))));
        }
        if (mapping.containsKey("positionName")) {
            position.setPositionName(getValue(data, getMappingIndex(mapping.get("positionName"))));
        }
        if (mapping.containsKey("majorRequired")) {
            position.setMajorRequired(getValue(data, getMappingIndex(mapping.get("majorRequired"))));
        }
        if (mapping.containsKey("educationRequired")) {
            String edu = getValue(data, getMappingIndex(mapping.get("educationRequired")));
            if (StrUtil.isNotBlank(edu)) {
                position.setEducationRequired(parseEducation(edu.trim()));
            }
        }
        if (mapping.containsKey("politicalStatusRequired")) {
            String political = getValue(data, getMappingIndex(mapping.get("politicalStatusRequired")));
            if (StrUtil.isNotBlank(political)) {
                position.setPoliticalStatusRequired(parsePoliticalStatus(political.trim()));
            }
        }
        if (mapping.containsKey("isFreshOnly")) {
            String fresh = getValue(data, getMappingIndex(mapping.get("isFreshOnly")));
            position.setIsFreshOnly("1".equals(fresh) || "是".equals(fresh) || "true".equalsIgnoreCase(fresh));
        }
        if (mapping.containsKey("recruitmentNumber")) {
            String num = getValue(data, getMappingIndex(mapping.get("recruitmentNumber")));
            if (StrUtil.isNotBlank(num)) {
                try {
                    position.setRecruitmentNumber(Integer.parseInt(num.trim()));
                } catch (NumberFormatException e) {
                    position.setRecruitmentNumber(1);
                }
            } else {
                position.setRecruitmentNumber(1);
            }
        } else {
            position.setRecruitmentNumber(1);
        }
        
        // 年份优先使用参数传入的值
        if (yearValue != null) {
            position.setYear(yearValue);
        }

        return position;
    }

    /**
     * 从mapping值中获取列索引
     */
    private Integer getMappingIndex(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getValue(Map<Integer, String> data, Integer index) {
        if (index == null || index < 0) {
            return null;
        }
        return data.get(index);
    }

    /**
     * 解析政治面貌字段，提取标准值
     * 按优先级从低到高匹配：不限 < 群众 < 共青团员 < 中共党员
     * 优先选择要求更低的（让更多人可以报考）
     * 
     * 示例：
     * - "中共党员或共青团员" -> "共青团员"（取要求更低的团员）
     * - "中共党员" -> "中共党员"
     * - "共青团员或群众" -> "群众"（取要求更低的群众）
     * - "不限" -> "不限"
     * - "无限制" -> "不限"
     * 
     * @param politicalStr Excel中的政治面貌字符串
     * @return 标准政治面貌值
     */
    private String parsePoliticalStatus(String politicalStr) {
        if (StrUtil.isBlank(politicalStr)) {
            return null;
        }
        
        // 按优先级顺序匹配关键词（LinkedHashMap保持插入顺序）
        for (Map.Entry<String, List<String>> entry : POLITICAL_STATUS_KEYWORDS.entrySet()) {
            String standardValue = entry.getKey();
            List<String> keywords = entry.getValue();
            
            for (String keyword : keywords) {
                if (politicalStr.contains(keyword)) {
                    return standardValue;
                }
            }
        }
        
        // 如果没有匹配到任何关键词，返回原始值（后续校验会处理）
        return politicalStr;
    }
     /* 按优先级从高到低匹配：博士 > 硕士 > 本科 > 大专/专科
     * 示例：
     * - "本科及以上" -> "本科"
     * - "仅限硕士" -> "硕士"
     * - "本科或硕士研究生" -> "本科"
     * - "大专及以上" -> "大专"
     * 
     * @param educationStr Excel中的学历字符串
     * @return 标准学历值
     */
    private String parseEducation(String educationStr) {
        if (StrUtil.isBlank(educationStr)) {
            return null;
        }
        
        // 按优先级顺序匹配关键词
        for (String keyword : EDUCATION_KEYWORDS) {
            if (educationStr.contains(keyword)) {
                // 将"专科"统一转换为"大专"
                if ("专科".equals(keyword)) {
                    return "大专";
                }
                return keyword;
            }
        }
        
        // 如果没有匹配到任何关键词，返回原始值（后续校验会处理）
        return educationStr;
    }

    /**
     * 校验Position数据
     */
    private String validatePosition(Position position, int rowNum) {
        // 部门不能为空
        if (StrUtil.isBlank(position.getDepartment())) {
            return "部门不能为空";
        }
        // 职位名称不能为空
        if (StrUtil.isBlank(position.getPositionName())) {
            return "职位名称不能为空";
        }
        // 年份不能为空
        if (position.getYear() == null) {
            return "年份不能为空或格式不正确";
        }
        // 年份范围校验
        if (position.getYear() < 2000 || position.getYear() > 2100) {
            return "年份应在2000-2100之间";
        }
        // 学历要求校验
        if (StrUtil.isNotBlank(position.getEducationRequired()) 
                && !VALID_EDUCATION.contains(position.getEducationRequired())) {
            return "学历要求值无效，有效值: 专科、本科、硕士研究生、博士研究生、大专、硕士、博士";
        }
        // 政治面貌要求校验
        if (StrUtil.isNotBlank(position.getPoliticalStatusRequired()) 
                && !VALID_POLITICAL_STATUS.contains(position.getPoliticalStatusRequired())) {
            return "政治面貌要求值无效，有效值: 不限、中共党员、共青团员、民主党派、群众";
        }
        // 招录人数校验
        if (position.getRecruitmentNumber() != null && position.getRecruitmentNumber() < 1) {
            return "招录人数必须大于0";
        }
        return null;
    }

    /**
     * 构建行数据字符串
     */
    private String buildRowData(Map<Integer, String> data, Map<String, Object> mapping) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            Integer index = getMappingIndex(entry.getValue());
            sb.append(entry.getKey()).append(":").append(index != null ? data.get(index) : "");
        }
        return sb.toString();
    }

    /**
     * 获取已存在的岗位唯一键
     */
    private Set<String> getExistingPositionKeys() {
        Set<String> keys = new HashSet<>();
        List<Position> allPositions = positionService.list();
        for (Position position : allPositions) {
            String key = position.getYear() + "_" + position.getDepartment() + "_" + position.getPositionName();
            keys.add(key);
        }
        return keys;
    }

    /**
     * 保存为模板
     */
    private void saveTemplate(ExecuteImportParam param, Long userId) {
        ImportTemplate template = new ImportTemplate();
        template.setTemplateName(param.getTemplateName());
        template.setColumnMapping(JSONUtil.toJsonStr(param.getMapping()));
        templateService.saveTemplate(template, userId);
    }

    /**
     * 定时清理过期文件（每小时执行）
     */
    @Scheduled(fixedRate = 3600000)
    @Override
    public void cleanExpiredFiles() {
        long expireTime = System.currentTimeMillis() - 3600000; // 1小时前
        Iterator<Map.Entry<String, SessionInfo>> iterator = sessionCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SessionInfo> entry = iterator.next();
            SessionInfo info = entry.getValue();
            if (info.getCreateTime().getTime() < expireTime) {
                // 删除临时文件
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
