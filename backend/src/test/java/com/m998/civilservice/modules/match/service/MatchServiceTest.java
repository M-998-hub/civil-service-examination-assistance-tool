package com.m998.civilservice.modules.match.service;

import com.m998.civilservice.modules.match.dto.MatchResultDto;
import com.m998.civilservice.modules.match.config.MatchConfig;
import com.m998.civilservice.modules.match.engine.MatchEngine;
import com.m998.civilservice.modules.match.rule.MatchRule;
import com.m998.civilservice.modules.match.rule.ScoreRule;
import com.m998.civilservice.modules.match.rule.filter.EducationFilter;
import com.m998.civilservice.modules.match.rule.filter.FreshGraduateFilter;
import com.m998.civilservice.modules.match.rule.filter.MajorFilter;
import com.m998.civilservice.modules.match.rule.filter.PoliticalStatusFilter;
import com.m998.civilservice.modules.match.rule.score.EducationScoreRule;
import com.m998.civilservice.modules.match.rule.score.MajorScoreRule;
import com.m998.civilservice.modules.match.rule.score.PoliticalStatusScoreRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("岗位匹配规则引擎单元测试")
class MatchServiceTest {

    private MatchEngine engine;
    private UserArchive archive;

    @BeforeEach
    void setUp() {
        MatchConfig config = createConfig();
        EducationFilter educationFilter = injectConfig(new EducationFilter(), config);
        MajorFilter majorFilter = injectConfig(new MajorFilter(), config);
        PoliticalStatusFilter politicalFilter = injectConfig(new PoliticalStatusFilter(), config);
        FreshGraduateFilter freshGraduateFilter = new FreshGraduateFilter();
        EducationScoreRule educationScorer = injectConfig(new EducationScoreRule(), config);
        MajorScoreRule majorScorer = injectConfig(new MajorScoreRule(), config);
        PoliticalStatusScoreRule politicalScorer = injectConfig(new PoliticalStatusScoreRule(), config);

        engine = new MatchEngine();
        ReflectionTestUtils.setField(engine, "filters", Arrays.<MatchRule>asList(
                educationFilter, majorFilter, politicalFilter, freshGraduateFilter));
        ReflectionTestUtils.setField(engine, "scorers", Arrays.<ScoreRule>asList(
                educationScorer, majorScorer, politicalScorer));

        archive = new UserArchive();
        archive.setMajor("软件工程");
        archive.setEducation("本科");
        archive.setPoliticalStatus("中共党员");
        archive.setIsFreshGraduate(true);
    }

    @Test
    @DisplayName("过滤硬性条件不满足的岗位")
    void recommend_filtersIneligiblePositions() {
        Position eligible = position("软件工程", "本科", "不限", false);
        Position educationTooHigh = position("软件工程", "博士", "不限", false);
        Position freshOnly = position("软件工程", "本科", "不限", true);

        List<MatchResultDto> results = engine.recommend(
                archive, Arrays.asList(eligible, educationTooHigh, freshOnly));

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(result -> result.getPosition() != educationTooHigh);
    }

    @Test
    @DisplayName("专业精确匹配优先于大类匹配")
    void recommend_sortsByScoreDescending() {
        Position categoryMatch = position("网络工程", "本科", "不限", false);
        Position exactMatch = position("软件工程", "本科", "中共党员", false);

        List<MatchResultDto> results = engine.recommend(
                archive, Arrays.asList(categoryMatch, exactMatch));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getPosition()).isSameAs(exactMatch);
        assertThat(results.get(0).getMatchScore()).isGreaterThan(results.get(1).getMatchScore());
        assertThat(results.get(0).getMatchDetails()).contains("专业完全匹配+30分");
    }

    @Test
    @DisplayName("空岗位列表返回空结果")
    void recommend_emptyPositions_returnsEmptyList() {
        assertThat(engine.recommend(archive, Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("非应届用户不能通过限应届规则")
    void recommend_nonFreshUser_failsFreshOnlyPosition() {
        archive.setIsFreshGraduate(false);
        Position freshOnly = position("软件工程", "本科", "不限", true);
        assertThat(engine.recommend(archive, Collections.singletonList(freshOnly))).isEmpty();
    }

    private MatchConfig createConfig() {
        MatchConfig config = new MatchConfig();
        config.setEducationLevels(mapOf("大专", 1, "本科", 2, "硕士", 3, "博士", 4));
        config.setPoliticalStatusLevels(mapOf("不限", 0, "群众", 1, "共青团员", 2, "中共党员", 3));
        Map<String, List<String>> categories = new LinkedHashMap<>();
        categories.put("计算机类", Arrays.asList("计算机科学与技术", "软件工程", "网络工程", "信息安全"));
        config.setMajorCategories(categories);
        config.setScoreWeights(mapOf(
                "majorExact", 30,
                "majorCategory", 15,
                "educationExceed", 10,
                "educationMatch", 5,
                "politicalStatus", 5));
        return config;
    }

    private <T> T injectConfig(T rule, MatchConfig config) {
        ReflectionTestUtils.setField(rule, "config", config);
        return rule;
    }

    private Map<String, Integer> mapOf(Object... values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], (Integer) values[i + 1]);
        }
        return result;
    }

    private Position position(String major, String education, String political, boolean freshOnly) {
        Position position = new Position();
        position.setDepartment("测试部门");
        position.setPositionName(major + "岗位");
        position.setYear(2026);
        position.setRecruitmentNumber(1);
        position.setMajorRequired(major);
        position.setEducationRequired(education);
        position.setPoliticalStatusRequired(political);
        position.setIsFreshOnly(freshOnly);
        return position;
    }
}
