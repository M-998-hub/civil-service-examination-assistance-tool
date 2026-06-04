package com.macro.mall.tiny.modules.ums.strategy;

import cn.hutool.core.util.StrUtil;
import com.macro.mall.tiny.modules.ums.dto.ImportFieldMeta;
import com.macro.mall.tiny.modules.ums.model.PositionStats;
import com.macro.mall.tiny.modules.ums.service.PositionStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 报录比数据导入策略
 * position_stats 表独立存储，不依赖 position 表
 */
@Component
public class PositionStatsImportStrategy implements ImportStrategy<PositionStats> {

    @Autowired
    private PositionStatsService positionStatsService;

    @Override
    public ImportTypeEnum getImportType() {
        return ImportTypeEnum.POSITION_STATS;
    }

    @Override
    public List<ImportFieldMeta> getFieldMetas() {
        List<ImportFieldMeta> metas = new ArrayList<>();
        metas.add(new ImportFieldMeta("department",        "部门",       true,  "招录部门名称"));
        metas.add(new ImportFieldMeta("positionName",      "职位名称",   true,  "岗位名称"));
        metas.add(new ImportFieldMeta("registrationCount", "报名人数",   true,  "该岗位报名总人数"));
        metas.add(new ImportFieldMeta("minEntryScore",     "最低进面分", false, "最低进面分数"));
        metas.add(new ImportFieldMeta("maxEntryScore",     "最高进面分", false, "最高进面分数"));
        return metas;
    }

    @Override
    public PositionStats buildEntity(Map<Integer, String> rowData,
                                     Map<String, Object> mapping,
                                     Map<String, Object> extraParams) {
        PositionStats stats = new PositionStats();

        // 年份从 extraParams 获取
        Object yearObj = extraParams.get("year");
        if (yearObj != null) {
            try {
                stats.setYear(Integer.parseInt(yearObj.toString().trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        // 直接从 Excel 行读取部门和职位名称
        stats.setDepartment(getValue(rowData, getMappingIndex(mapping.get("department"))));
        stats.setPositionName(getValue(rowData, getMappingIndex(mapping.get("positionName"))));

        // 报名人数
        String regCount = getValue(rowData, getMappingIndex(mapping.get("registrationCount")));
        if (StrUtil.isNotBlank(regCount)) {
            try {
                stats.setRegistrationCount(Integer.parseInt(regCount.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        // 最低进面分
        String minScore = getValue(rowData, getMappingIndex(mapping.get("minEntryScore")));
        if (StrUtil.isNotBlank(minScore)) {
            try {
                stats.setMinEntryScore(new BigDecimal(minScore.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        // 最高进面分
        String maxScore = getValue(rowData, getMappingIndex(mapping.get("maxEntryScore")));
        if (StrUtil.isNotBlank(maxScore)) {
            try {
                stats.setMaxEntryScore(new BigDecimal(maxScore.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return stats;
    }

    @Override
    public String validate(PositionStats entity, int rowNum) {
        if (StrUtil.isBlank(entity.getDepartment())) {
            return "部门不能为空";
        }
        if (StrUtil.isBlank(entity.getPositionName())) {
            return "职位名称不能为空";
        }
        if (entity.getYear() == null) {
            return "年份不能为空";
        }
        if (entity.getYear() < 2000 || entity.getYear() > 2100) {
            return "年份应在2000-2100之间";
        }
        if (entity.getRegistrationCount() == null || entity.getRegistrationCount() < 0) {
            return "报名人数不能为空且不能小于0";
        }
        if (entity.getMinEntryScore() != null && entity.getMaxEntryScore() != null) {
            if (entity.getMinEntryScore().compareTo(entity.getMaxEntryScore()) > 0) {
                return "最低进面分不能大于最高进面分";
            }
        }
        return null;
    }

    @Override
    public String getDuplicateKey(PositionStats entity) {
        return entity.getYear() + "||" + entity.getDepartment() + "||" + entity.getPositionName();
    }

    @Override
    public Set<String> getExistingKeys(Map<String, Object> extraParams) {
        Set<String> keys = new HashSet<>();
        List<PositionStats> allStats = positionStatsService.list();
        for (PositionStats stats : allStats) {
            String key = stats.getYear() + "||" + stats.getDepartment() + "||" + stats.getPositionName();
            keys.add(key);
        }
        return keys;
    }

    @Override
    public void saveBatch(List<PositionStats> entityList) {
        positionStatsService.saveBatch(entityList);
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
}
