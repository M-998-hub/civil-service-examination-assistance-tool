package com.m998.civilservice.modules.position.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.m998.civilservice.modules.importdata.dto.ImportResult;
import com.m998.civilservice.modules.position.dto.PositionFilterParam;
import com.m998.civilservice.modules.importdata.dto.PositionExcelDto;
import com.m998.civilservice.modules.importdata.listener.PositionExcelListener;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.mapper.PositionMapper;
import com.m998.civilservice.modules.position.service.PositionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 岗位表 服务实现类
 * </p>
 *
 * @since 2026-04-10
 */
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    @Override
    public Page<Position> filter(PositionFilterParam param) {
        Page<Position> page = new Page<>(param.getPageNum(), param.getPageSize());
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询未删除的
        wrapper.eq(Position::getStatus, 0);
        
        // 年份筛选
        if (param.getYear() != null) {
            wrapper.eq(Position::getYear, param.getYear());
        }
        
        // 部门名称模糊匹配
        if (StrUtil.isNotBlank(param.getDepartment())) {
            wrapper.like(Position::getDepartment, param.getDepartment());
        }
        
        // 学历要求精确匹配（优先使用等级筛选）
        if (param.getEducationLevel() != null) {
            // 等级筛选：该等级及以下可报岗位
            List<String> allowedEducations = getEducationLevelsByMax(param.getEducationLevel());
            if (!allowedEducations.isEmpty()) {
                wrapper.in(Position::getEducationRequired, allowedEducations);
            }
        } else if (StrUtil.isNotBlank(param.getEducationRequired())) {
            // 精确匹配（向后兼容）
            wrapper.eq(Position::getEducationRequired, param.getEducationRequired());
        }
        
        // 政治面貌要求（优先使用等级筛选）
        if (param.getPoliticalStatusLevel() != null) {
            // 等级筛选：该等级及以下可报岗位
            List<String> allowedStatuses = getPoliticalStatusesByMax(param.getPoliticalStatusLevel());
            if (!allowedStatuses.isEmpty()) {
                wrapper.in(Position::getPoliticalStatusRequired, allowedStatuses);
            }
        } else if (StrUtil.isNotBlank(param.getPoliticalStatusRequired())) {
            // 精确匹配（向后兼容）
            wrapper.eq(Position::getPoliticalStatusRequired, param.getPoliticalStatusRequired());
        }
        
        // 是否限应届
        if (param.getIsFreshOnly() != null) {
            wrapper.eq(Position::getIsFreshOnly, param.getIsFreshOnly());
        }
        
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importExcel(MultipartFile file) throws Exception {
        ImportResult importResult = new ImportResult();
        
        // 查询数据库中已存在的岗位（用于去重）
        Set<String> existingKeys = getExistingPositionKeys();
        
        // 创建监听器
        PositionExcelListener listener = new PositionExcelListener(this, importResult, existingKeys);
        
        try {
            // 使用 EasyExcel 读取文件
            EasyExcel.read(file.getInputStream(), PositionExcelDto.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            throw new RuntimeException("读取Excel文件失败: " + e.getMessage());
        }
        
        return importResult;
    }

    /**
     * 获取数据库中已存在的岗位唯一键集合
     * 格式：年份_部门_职位名称
     */
    private Set<String> getExistingPositionKeys() {
        Set<String> keys = new HashSet<>();
        List<Position> allPositions = list();
        for (Position position : allPositions) {
            String key = position.getYear() + "_" + position.getDepartment() + "_" + position.getPositionName();
            keys.add(key);
        }
        return keys;
    }

    /**
     * 学历等级映射
     * 大专=1，本科=2，硕士=3，博士=4
     */
    private static final Map<String, Integer> EDUCATION_LEVEL_MAP = new HashMap<>();
    static {
        EDUCATION_LEVEL_MAP.put("大专", 1);
        EDUCATION_LEVEL_MAP.put("专科", 1);
        EDUCATION_LEVEL_MAP.put("本科", 2);
        EDUCATION_LEVEL_MAP.put("硕士研究生", 3);
        EDUCATION_LEVEL_MAP.put("硕士", 3);
        EDUCATION_LEVEL_MAP.put("博士研究生", 4);
        EDUCATION_LEVEL_MAP.put("博士", 4);
    }

    /**
     * 根据最大学历等级获取允许的所有学历列表（该等级及以下）
     * @param maxLevel 最大学历等级（1=大专，2=本科，3=硕士，4=博士）
     * @return 允许报考的学历列表
     */
    private List<String> getEducationLevelsByMax(Integer maxLevel) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : EDUCATION_LEVEL_MAP.entrySet()) {
            if (entry.getValue() <= maxLevel) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 政治面貌等级映射
     * 不限=1，共青团员=2，中共党员=3
     */
    private static final Map<String, Integer> POLITICAL_STATUS_LEVEL_MAP = new HashMap<>();
    static {
        POLITICAL_STATUS_LEVEL_MAP.put("不限", 1);
        POLITICAL_STATUS_LEVEL_MAP.put("共青团员", 2);
        POLITICAL_STATUS_LEVEL_MAP.put("中共党员", 3);
    }

    /**
     * 根据最大政治面貌等级获取允许的所有政治面貌列表（该等级及以下）
     * @param maxLevel 最大政治面貌等级（1=不限，2=共青团员，3=中共党员）
     * @return 允许报考的政治面貌列表
     */
    private List<String> getPoliticalStatusesByMax(Integer maxLevel) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : POLITICAL_STATUS_LEVEL_MAP.entrySet()) {
            if (entry.getValue() <= maxLevel) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    @Override
    public Page<Position> adminPage(Integer pageNum, Integer pageSize, String department, Integer year) {
        Page<Position> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getStatus, 0);
        if (StrUtil.isNotBlank(department)) {
            wrapper.like(Position::getDepartment, department);
        }
        if (year != null) {
            wrapper.eq(Position::getYear, year);
        }
        wrapper.orderByDesc(Position::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public boolean deleteById(Long id) {
        Position position = new Position();
        position.setId(id);
        position.setStatus(1);
        return updateById(position);
    }

    @Override
    public boolean batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<Position> positions = new ArrayList<>();
        for (Long id : ids) {
            Position p = new Position();
            p.setId(id);
            p.setStatus(1);
            positions.add(p);
        }
        return updateBatchById(positions);
    }
}
