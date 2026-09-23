package com.m998.civilservice.modules.importdata.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.m998.civilservice.modules.importdata.dto.ImportResult;
import com.m998.civilservice.modules.importdata.dto.PositionExcelDto;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.service.PositionService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel 导入岗位监听器
 */
public class PositionExcelListener implements ReadListener<PositionExcelDto> {

    /**
     * 每隔 BATCH_COUNT 条存储数据库，避免内存溢出
     */
    private static final int BATCH_COUNT = 1000;

    /**
     * 缓存的数据列表
     */
    private List<Position> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    /**
     * 岗位服务
     */
    private final PositionService positionService;

    /**
     * 导入结果
     */
    private final ImportResult importResult;

    /**
     * 用于去重的集合（年份+部门+职位名称）
     */
    private final Set<String> existingKeys;

    /**
     * 当前处理行号
     */
    private int currentRow = 1;

    /**
     * 有效的学历要求枚举
     */
    private static final Set<String> VALID_EDUCATION = new HashSet<String>() {{
        add("专科");
        add("本科");
        add("硕士研究生");
        add("博士研究生");
        add("大专");
        add("硕士");
        add("博士");
    }};

    /**
     * 有效的政治面貌要求枚举
     */
    private static final Set<String> VALID_POLITICAL_STATUS = new HashSet<String>() {{
        add("不限");
        add("中共党员");
        add("共青团员");
        add("民主党派");
        add("群众");
    }};

    public PositionExcelListener(PositionService positionService, ImportResult importResult, Set<String> existingKeys) {
        this.positionService = positionService;
        this.importResult = importResult;
        this.existingKeys = existingKeys;
    }

    @Override
    public void invoke(PositionExcelDto data, AnalysisContext context) {
        currentRow++;
        
        // 校验必填字段
        String validateMsg = validateData(data);
        if (validateMsg != null) {
            importResult.addFailReason("第" + currentRow + "行: " + validateMsg);
            importResult.setFailCount(importResult.getFailCount() + 1);
            return;
        }

        // 构建唯一键
        String key = data.getYear() + "_" + data.getDepartment() + "_" + data.getPositionName();
        
        // 去重检查
        if (existingKeys.contains(key)) {
            importResult.addFailReason("第" + currentRow + "行: 该岗位已存在（年份+部门+职位名称重复）");
            importResult.setFailCount(importResult.getFailCount() + 1);
            return;
        }
        
        // 添加到已存在集合，防止 Excel 内重复
        existingKeys.add(key);

        // 转换为实体
        Position position = convertToPosition(data);
        cachedDataList.add(position);

        // 达到批量插入阈值时保存
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 保存剩余数据
        saveData();
    }

    /**
     * 校验数据
     */
    private String validateData(PositionExcelDto data) {
        // 部门不能为空
        if (StrUtil.isBlank(data.getDepartment())) {
            return "部门不能为空";
        }
        
        // 职位名称不能为空
        if (StrUtil.isBlank(data.getPositionName())) {
            return "职位名称不能为空";
        }
        
        // 年份不能为空
        if (StrUtil.isBlank(data.getYear())) {
            return "年份不能为空";
        }
        
        // 校验年份格式
        try {
            int year = Integer.parseInt(data.getYear().trim());
            if (year < 2000 || year > 2100) {
                return "年份格式不正确，应为2000-2100之间的整数";
            }
        } catch (NumberFormatException e) {
            return "年份格式不正确，应为整数";
        }
        
        // 校验学历要求
        if (StrUtil.isNotBlank(data.getEducationRequired())) {
            if (!VALID_EDUCATION.contains(data.getEducationRequired().trim())) {
                return "学历要求值无效，有效值: 专科、本科、硕士研究生、博士研究生、大专、硕士、博士";
            }
        }
        
        // 校验政治面貌要求
        if (StrUtil.isNotBlank(data.getPoliticalStatusRequired())) {
            if (!VALID_POLITICAL_STATUS.contains(data.getPoliticalStatusRequired().trim())) {
                return "政治面貌要求值无效，有效值: 不限、中共党员、共青团员、民主党派、群众";
            }
        }
        
        // 校验是否限应届
        if (StrUtil.isNotBlank(data.getIsFreshOnly())) {
            String fresh = data.getIsFreshOnly().trim();
            if (!"0".equals(fresh) && !"1".equals(fresh) && !"是".equals(fresh) && !"否".equals(fresh)) {
                return "是否限应届值无效，有效值: 0、1、是、否";
            }
        }
        
        // 校验招录人数
        if (StrUtil.isNotBlank(data.getRecruitmentNumber())) {
            try {
                int num = Integer.parseInt(data.getRecruitmentNumber().trim());
                if (num < 1) {
                    return "招录人数必须大于0";
                }
            } catch (NumberFormatException e) {
                return "招录人数格式不正确，应为整数";
            }
        }
        
        return null;
    }

    /**
     * 转换为实体
     */
    private Position convertToPosition(PositionExcelDto dto) {
        Position position = new Position();
        position.setDepartment(dto.getDepartment().trim());
        position.setPositionName(dto.getPositionName().trim());
        position.setMajorRequired(StrUtil.isBlank(dto.getMajorRequired()) ? null : dto.getMajorRequired().trim());
        
        if (StrUtil.isNotBlank(dto.getEducationRequired())) {
            position.setEducationRequired(dto.getEducationRequired().trim());
        }
        
        if (StrUtil.isNotBlank(dto.getPoliticalStatusRequired())) {
            position.setPoliticalStatusRequired(dto.getPoliticalStatusRequired().trim());
        }
        
        // 是否限应届转换
        if (StrUtil.isNotBlank(dto.getIsFreshOnly())) {
            String fresh = dto.getIsFreshOnly().trim();
            position.setIsFreshOnly("1".equals(fresh) || "是".equals(fresh));
        } else {
            position.setIsFreshOnly(false);
        }
        
        // 招录人数
        if (StrUtil.isNotBlank(dto.getRecruitmentNumber())) {
            position.setRecruitmentNumber(Integer.parseInt(dto.getRecruitmentNumber().trim()));
        } else {
            position.setRecruitmentNumber(1);
        }
        
        // 年份
        position.setYear(Integer.parseInt(dto.getYear().trim()));
        
        return position;
    }

    /**
     * 保存数据到数据库
     */
    private void saveData() {
        if (cachedDataList.isEmpty()) {
            return;
        }
        
        int successCount = positionService.saveBatch(cachedDataList) ? cachedDataList.size() : 0;
        importResult.setSuccessCount(importResult.getSuccessCount() + successCount);
        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }
}
