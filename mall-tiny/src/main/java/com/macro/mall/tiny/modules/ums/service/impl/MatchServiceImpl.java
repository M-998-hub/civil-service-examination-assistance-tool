package com.macro.mall.tiny.modules.ums.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.macro.mall.tiny.modules.ums.dto.MatchResultDto;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.model.UserArchive;
import com.macro.mall.tiny.modules.ums.service.MatchService;
import com.macro.mall.tiny.modules.ums.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 岗位匹配服务实现
 * Created by macro on 2026-04-03.
 */
@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchServiceImpl.class);

    @Autowired
    private PositionService positionService;

    // 学历等级映射
    private static final Map<String, Integer> EDUCATION_LEVEL = new HashMap<String, Integer>() {{
        put("大专", 1);
        put("本科", 2);
        put("硕士", 3);
        put("博士", 4);
    }};

    // 政治面貌等级映射（值越大要求越高）
    private static final Map<String, Integer> POLITICAL_STATUS_LEVEL = new HashMap<String, Integer>() {{
        put("不限", 0);
        put("群众", 1);
        put("共青团员", 2);
        put("中共党员", 3);
    }};

    // 专业大类映射（大类 -> 包含的专业列表）
    private static final Map<String, Set<String>> MAJOR_CATEGORIES = new HashMap<String, Set<String>>() {{
        put("计算机类", new HashSet<String>() {{
            add("计算机科学与技术");
            add("软件工程");
            add("网络工程");
            add("信息安全");
            add("物联网工程");
            add("数据科学与大数据技术");
            add("人工智能");
            add("计算机");
        }});
        put("经济类", new HashSet<String>() {{
            add("经济学");
            add("金融学");
            add("国际经济与贸易");
            add("财政学");
            add("经济");
            add("金融");
        }});
        put("管理类", new HashSet<String>() {{
            add("工商管理");
            add("市场营销");
            add("会计学");
            add("财务管理");
            add("人力资源管理");
            add("公共管理");
            add("管理");
        }});
        put("法学类", new HashSet<String>() {{
            add("法学");
            add("知识产权");
            add("法律");
        }});
        put("文学类", new HashSet<String>() {{
            add("汉语言文学");
            add("新闻学");
            add("广告学");
            add("英语");
            add("日语");
            add("文学");
        }});
        put("工学类", new HashSet<String>() {{
            add("机械工程");
            add("电气工程");
            add("土木工程");
            add("建筑学");
            add("工学");
        }});
        put("理学类", new HashSet<String>() {{
            add("数学");
            add("物理学");
            add("化学");
            add("生物科学");
            add("理学");
        }});
    }};

    @Override
    public List<MatchResultDto> recommend(UserArchive userArchive) {
        LOGGER.info("开始一键匹配 - 用户档案: 专业={}, 学历={}, 政治面貌={}, 是否应届={}",
                userArchive.getMajor(),
                userArchive.getEducation(),
                userArchive.getPoliticalStatus(),
                userArchive.getIsFreshGraduate());
        
        // 查询所有岗位
        List<Position> allPositions = positionService.list(new LambdaQueryWrapper<>());
        LOGGER.info("查询到 {} 个岗位", allPositions.size());
        
        List<MatchResultDto> results = new ArrayList<>();
        int checkedCount = 0;
        int passedCount = 0;
        
        for (Position position : allPositions) {
            checkedCount++;
            // 检查硬性条件
            if (!checkHardConditions(userArchive, position)) {
                continue;
            }
            passedCount++;
            
            // 计算匹配度
            MatchResultDto result = calculateMatchScore(userArchive, position);
            if (result != null) {
                results.add(result);
            }
        }
        
        LOGGER.info("匹配完成 - 检查岗位: {}, 通过硬性条件: {}, 最终匹配结果: {}",
                checkedCount, passedCount, results.size());
        
        // 按匹配度从高到低排序
        return results.stream()
                .sorted(Comparator.comparing(MatchResultDto::getMatchScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 检查硬性条件
     */
    private boolean checkHardConditions(UserArchive userArchive, Position position) {
        // 1. 学历检查：用户学历 >= 岗位要求学历
        if (!checkEducation(userArchive.getEducation(), position.getEducationRequired())) {
            return false;
        }
        
        // 2. 应届身份检查
        if (!checkFreshGraduate(userArchive.getIsFreshGraduate(), position.getIsFreshOnly())) {
            return false;
        }
        
        // 3. 政治面貌检查
        if (!checkPoliticalStatus(userArchive.getPoliticalStatus(), position.getPoliticalStatusRequired())) {
            return false;
        }
        
        return true;
    }

    /**
     * 检查学历条件
     */
    private boolean checkEducation(String userEducation, String requiredEducation) {
        if (StrUtil.isBlank(requiredEducation)) {
            return true; // 岗位无学历要求
        }
        
        int userLevel = EDUCATION_LEVEL.getOrDefault(userEducation, 0);
        int requiredLevel = EDUCATION_LEVEL.getOrDefault(requiredEducation, 0);
        
        return userLevel >= requiredLevel;
    }

    /**
     * 检查应届身份条件
     */
    private boolean checkFreshGraduate(Boolean isFreshGraduate, Boolean isFreshOnly) {
        if (isFreshOnly == null || !isFreshOnly) {
            return true; // 岗位不限制应届
        }
        
        // 岗位限应届，用户必须是应届生
        return isFreshGraduate != null && isFreshGraduate;
    }

    /**
     * 检查政治面貌条件
     * 规则：用户政治面貌等级 >= 岗位要求等级 才能报考
     * 中共党员(3) > 共青团员(2) > 群众(1) > 不限(0)
     * 
     * 用户「中共党员」 可报「不限」「共青团员」「中共党员」
     * 用户「共青团员」 可报「不限」「共青团员」
     * 用户「群众」 可报「不限」「群众」
     */
    private boolean checkPoliticalStatus(String userStatus, String requiredStatus) {
        LOGGER.debug("检查政治面貌 - 用户: {}, 岗位要求: {}", userStatus, requiredStatus);
        
        if (StrUtil.isBlank(requiredStatus) || "不限".equals(requiredStatus)) {
            LOGGER.debug("岗位无政治面貌要求，通过");
            return true; // 岗位无政治面貌要求
        }
        
        int userLevel = POLITICAL_STATUS_LEVEL.getOrDefault(userStatus, -1);
        int requiredLevel = POLITICAL_STATUS_LEVEL.getOrDefault(requiredStatus, -1);
        
        LOGGER.debug("政治面貌等级 - 用户: {}, 岗位: {}", userLevel, requiredLevel);
        
        if (userLevel < 0 || requiredLevel < 0) {
            LOGGER.warn("未知的政治面貌值 - 用户: {}, 岗位: {}", userStatus, requiredStatus);
            return false; // 未知的政治面貌值
        }
        
        // 用户等级 >= 岗位要求等级 才能报考
        boolean result = userLevel >= requiredLevel;
        LOGGER.debug("政治面貌检查结果: {}", result);
        return result;
    }

    /**
     * 计算匹配度分数
     */
    private MatchResultDto calculateMatchScore(UserArchive userArchive, Position position) {
        int score = 0;
        List<String> details = new ArrayList<>();
        
        // 1. 专业匹配
        int majorScore = calculateMajorMatchScore(userArchive.getMajor(), position.getMajorRequired());
        if (majorScore > 0) {
            score += majorScore;
            if (majorScore == 30) {
                details.add("专业完全匹配+30分");
            } else if (majorScore == 15) {
                details.add("专业属于相关大类+15分");
            }
        }
        
        // 2. 学历匹配
        int educationScore = calculateEducationMatchScore(userArchive.getEducation(), position.getEducationRequired());
        score += educationScore;
        if (educationScore == 10) {
            details.add("学历超出要求+10分");
        } else if (educationScore == 5) {
            details.add("学历刚好满足+5分");
        }
        
        // 3. 政治面貌匹配
        if (checkPoliticalStatusMatch(userArchive.getPoliticalStatus(), position.getPoliticalStatusRequired())) {
            score += 5;
            details.add("政治面貌满足+5分");
        }
        
        return new MatchResultDto(position, score, details);
    }

    /**
     * 计算专业匹配分数
     */
    private int calculateMajorMatchScore(String userMajor, String requiredMajor) {
        if (StrUtil.isBlank(requiredMajor)) {
            return 0; // 岗位无专业要求，不加也不减
        }
        
        if (StrUtil.isBlank(userMajor)) {
            return 0;
        }
        
        // 完全匹配
        if (requiredMajor.equals(userMajor)) {
            return 30;
        }
        
        // 岗位要求包含用户专业（模糊匹配）
        if (requiredMajor.contains(userMajor) || userMajor.contains(requiredMajor)) {
            return 30;
        }
        
        // 检查是否属于同一大类
        if (isInSameCategory(userMajor, requiredMajor)) {
            return 15;
        }
        
        return 0;
    }

    /**
     * 判断用户专业和岗位要求是否属于同一大类
     */
    private boolean isInSameCategory(String userMajor, String requiredMajor) {
        for (Map.Entry<String, Set<String>> entry : MAJOR_CATEGORIES.entrySet()) {
            Set<String> majors = entry.getValue();
            String categoryName = entry.getKey();
            
            // 检查岗位要求是否是大类名，用户专业是否在该大类中
            boolean requiredIsCategory = categoryName.equals(requiredMajor) || 
                    (requiredMajor != null && requiredMajor.contains(categoryName.replace("类", "")));
            boolean userInCategory = majors.contains(userMajor) || 
                    majors.stream().anyMatch(m -> userMajor != null && userMajor.contains(m));
            
            if (requiredIsCategory && userInCategory) {
                return true;
            }
            
            // 检查岗位要求的专业是否和用户专业在同一大类
            boolean requiredInCategory = majors.contains(requiredMajor) ||
                    majors.stream().anyMatch(m -> requiredMajor != null && requiredMajor.contains(m));
            
            if (requiredInCategory && userInCategory) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 计算学历匹配分数
     */
    private int calculateEducationMatchScore(String userEducation, String requiredEducation) {
        if (StrUtil.isBlank(requiredEducation)) {
            return 5; // 岗位无学历要求
        }
        
        int userLevel = EDUCATION_LEVEL.getOrDefault(userEducation, 0);
        int requiredLevel = EDUCATION_LEVEL.getOrDefault(requiredEducation, 0);
        
        if (userLevel > requiredLevel) {
            return 10; // 学历超出要求
        } else if (userLevel == requiredLevel) {
            return 5; // 学历刚好满足
        }
        
        return 0;
    }

    /**
     * 检查政治面貌是否匹配（用于加分判断）
     * 规则与用户报考检查一致
     */
    private boolean checkPoliticalStatusMatch(String userStatus, String requiredStatus) {
        if (StrUtil.isBlank(requiredStatus) || "不限".equals(requiredStatus)) {
            return true;
        }
        
        int userLevel = POLITICAL_STATUS_LEVEL.getOrDefault(userStatus, -1);
        int requiredLevel = POLITICAL_STATUS_LEVEL.getOrDefault(requiredStatus, -1);
        
        if (userLevel < 0 || requiredLevel < 0) {
            return false;
        }
        
        // 用户等级 >= 岗位要求等级
        return userLevel >= requiredLevel;
    }
}
