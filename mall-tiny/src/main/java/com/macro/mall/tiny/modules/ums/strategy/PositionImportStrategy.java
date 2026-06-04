package com.macro.mall.tiny.modules.ums.strategy;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.tiny.modules.ums.dto.ImportFieldMeta;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 岗位信息导入策略
 */
@Component
public class PositionImportStrategy implements ImportStrategy<Position> {

    @Autowired
    private PositionService positionService;

    /**
     * 有效的学历要求枚举
     */
    private static final Set<String> VALID_EDUCATION = new HashSet<String>() {{
        add("专科"); add("本科"); add("硕士研究生"); add("博士研究生");
        add("大专"); add("硕士"); add("博士");
    }};

    /**
     * 学历关键词提取配置（按优先级从高到低排序）
     */
    private static final List<String> EDUCATION_KEYWORDS = new ArrayList<String>() {{
        add("博士");
        add("硕士");
        add("本科");
        add("大专");
        add("专科");
    }};

    /**
     * 有效的政治面貌要求枚举
     */
    private static final Set<String> VALID_POLITICAL_STATUS = new HashSet<String>() {{
        add("不限"); add("中共党员"); add("共青团员"); add("民主党派"); add("群众");
    }};

    /**
     * 政治面貌关键词映射配置
     */
    private static final Map<String, List<String>> POLITICAL_STATUS_KEYWORDS = new LinkedHashMap<String, List<String>>() {{
        put("不限", new ArrayList<String>() {{
            add("不限"); add("无限制"); add("无"); add("不限制");
        }});
        put("群众", new ArrayList<String>() {{
            add("群众");
        }});
        put("共青团员", new ArrayList<String>() {{
            add("共青团员"); add("团员");
        }});
        put("中共党员", new ArrayList<String>() {{
            add("中共党员"); add("中共正式党员"); add("中共预备党员"); add("党员");
        }});
    }};

    @Override
    public ImportTypeEnum getImportType() {
        return ImportTypeEnum.POSITION;
    }

    @Override
    public List<ImportFieldMeta> getFieldMetas() {
        List<ImportFieldMeta> metas = new ArrayList<>();
        metas.add(new ImportFieldMeta("department", "部门", true, "招录部门名称"));
        metas.add(new ImportFieldMeta("positionName", "职位名称", true, "岗位名称"));
        metas.add(new ImportFieldMeta("majorRequired", "专业要求", false, "专业要求描述"));
        metas.add(new ImportFieldMeta("educationRequired", "学历要求", false, "如：本科、硕士研究生"));
        metas.add(new ImportFieldMeta("politicalStatusRequired", "政治面貌要求", false, "如：不限、中共党员"));
        metas.add(new ImportFieldMeta("isFreshOnly", "是否限应届", false, "1/是/true 表示限应届"));
        metas.add(new ImportFieldMeta("recruitmentNumber", "招录人数", false, "招录人数，默认为1"));
        return metas;
    }

    @Override
    public Position buildEntity(Map<Integer, String> rowData, Map<String, Object> mapping, Map<String, Object> extraParams) {
        Position position = new Position();

        if (mapping.containsKey("department")) {
            position.setDepartment(getValue(rowData, getMappingIndex(mapping.get("department"))));
        }
        if (mapping.containsKey("positionName")) {
            position.setPositionName(getValue(rowData, getMappingIndex(mapping.get("positionName"))));
        }
        if (mapping.containsKey("majorRequired")) {
            position.setMajorRequired(getValue(rowData, getMappingIndex(mapping.get("majorRequired"))));
        }
        if (mapping.containsKey("educationRequired")) {
            String edu = getValue(rowData, getMappingIndex(mapping.get("educationRequired")));
            if (StrUtil.isNotBlank(edu)) {
                position.setEducationRequired(parseEducation(edu.trim()));
            }
        }
        if (mapping.containsKey("politicalStatusRequired")) {
            String political = getValue(rowData, getMappingIndex(mapping.get("politicalStatusRequired")));
            if (StrUtil.isNotBlank(political)) {
                position.setPoliticalStatusRequired(parsePoliticalStatus(political.trim()));
            }
        }
        if (mapping.containsKey("isFreshOnly")) {
            String fresh = getValue(rowData, getMappingIndex(mapping.get("isFreshOnly")));
            position.setIsFreshOnly("1".equals(fresh) || "是".equals(fresh) || "true".equalsIgnoreCase(fresh));
        }
        if (mapping.containsKey("recruitmentNumber")) {
            String num = getValue(rowData, getMappingIndex(mapping.get("recruitmentNumber")));
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

        // 年份从 extraParams 获取
        Object yearObj = extraParams.get("year");
        if (yearObj != null) {
            try {
                position.setYear(Integer.parseInt(yearObj.toString().trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return position;
    }

    @Override
    public String validate(Position position, int rowNum) {
        if (StrUtil.isBlank(position.getDepartment())) {
            return "部门不能为空";
        }
        if (StrUtil.isBlank(position.getPositionName())) {
            return "职位名称不能为空";
        }
        if (position.getYear() == null) {
            return "年份不能为空或格式不正确";
        }
        if (position.getYear() < 2000 || position.getYear() > 2100) {
            return "年份应在2000-2100之间";
        }
        if (StrUtil.isNotBlank(position.getEducationRequired())
                && !VALID_EDUCATION.contains(position.getEducationRequired())) {
            return "学历要求值无效，有效值: 专科、本科、硕士研究生、博士研究生、大专、硕士、博士";
        }
        if (StrUtil.isNotBlank(position.getPoliticalStatusRequired())
                && !VALID_POLITICAL_STATUS.contains(position.getPoliticalStatusRequired())) {
            return "政治面貌要求值无效，有效值: 不限、中共党员、共青团员、民主党派、群众";
        }
        if (position.getRecruitmentNumber() != null && position.getRecruitmentNumber() < 1) {
            return "招录人数必须大于0";
        }
        return null;
    }

    @Override
    public String getDuplicateKey(Position position) {
        return position.getYear() + "||" + position.getDepartment() + "||" + position.getPositionName();
    }

    @Override
    public Set<String> getExistingKeys(Map<String, Object> extraParams) {
        Set<String> keys = new HashSet<>();
        List<Position> allPositions = positionService.list();
        for (Position position : allPositions) {
            String key = position.getYear() + "||" + position.getDepartment() + "||" + position.getPositionName();
            keys.add(key);
        }
        return keys;
    }

    @Override
    public void saveBatch(List<Position> entityList) {
        positionService.saveBatch(entityList);
    }

    // ==================== 私有辅助方法 ====================

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

    private String getValue(Map<Integer, String> data, Integer index) {
        if (index == null || index < 0) return null;
        return data.get(index);
    }

    private String parseEducation(String educationStr) {
        if (StrUtil.isBlank(educationStr)) return null;
        for (String keyword : EDUCATION_KEYWORDS) {
            if (educationStr.contains(keyword)) {
                if ("专科".equals(keyword)) return "大专";
                return keyword;
            }
        }
        return educationStr;
    }

    private String parsePoliticalStatus(String politicalStr) {
        if (StrUtil.isBlank(politicalStr)) return null;
        for (Map.Entry<String, List<String>> entry : POLITICAL_STATUS_KEYWORDS.entrySet()) {
            String standardValue = entry.getKey();
            List<String> keywords = entry.getValue();
            for (String keyword : keywords) {
                if (politicalStr.contains(keyword)) {
                    return standardValue;
                }
            }
        }
        return politicalStr;
    }
}
