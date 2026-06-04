package com.macro.mall.tiny.modules.ums.service;

import com.macro.mall.tiny.modules.ums.dto.MatchResultDto;
import com.macro.mall.tiny.modules.ums.model.Position;
import com.macro.mall.tiny.modules.ums.model.UserArchive;
import com.macro.mall.tiny.modules.ums.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 岗位匹配服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService 单元测试")
class MatchServiceTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private MatchServiceImpl matchService;

    private UserArchive userArchive;

    @BeforeEach
    void setUp() {
        userArchive = new UserArchive();
        userArchive.setMajor("软件工程");
        userArchive.setEducation("本科");
        userArchive.setPoliticalStatus("中共党员");
        userArchive.setIsFreshGraduate(true);
    }

    // === 硬性条件：学历检查 ===

    @Nested
    @DisplayName("学历硬性条件检查")
    class EducationCheckTests {

        @Test
        @DisplayName("用户学历满足要求 - 通过")
        void checkEducation_userMeetsRequirement_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkEducation", "本科", "本科");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("用户学历超出要求 - 通过")
        void checkEducation_userExceedsRequirement_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkEducation", "硕士", "本科");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("用户学历不足 - 不通过")
        void checkEducation_userBelowRequirement_false() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkEducation", "大专", "本科");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("岗位无学历要求 - 通过")
        void checkEducation_noRequirement_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkEducation", "大专", null);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("岗位学历要求为空字符串 - 通过")
        void checkEducation_emptyRequirement_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkEducation", "本科", "");
            assertThat(result).isTrue();
        }
    }

    // === 硬性条件：政治面貌检查 ===

    @Nested
    @DisplayName("政治面貌硬性条件检查")
    class PoliticalStatusCheckTests {

        @Test
        @DisplayName("党员可以报考所有级别岗位")
        void checkPolitical_partyMemberMeetsAll_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "中共党员", "共青团员");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("群众不能报考要求党员的岗位")
        void checkPolitical_massesCantMeetParty_false() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "群众", "中共党员");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("岗位不限政治面貌 - 通过")
        void checkPolitical_noRequirement_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "群众", "不限");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("岗位政治面貌为空 - 通过")
        void checkPolitical_nullRequirement_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "群众", null);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("团员可以报考要求团员的岗位")
        void checkPolitical_leagueMemberMeetsLeague_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "共青团员", "共青团员");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("团员不能报考要求党员的岗位")
        void checkPolitical_leagueCantMeetParty_false() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "共青团员", "中共党员");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("未知政治面貌值 - 不通过")
        void checkPolitical_unknownStatus_false() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkPoliticalStatus", "无党派", "中共党员");
            assertThat(result).isFalse();
        }
    }

    // === 硬性条件：应届检查 ===

    @Nested
    @DisplayName("应届身份检查")
    class FreshGraduateCheckTests {

        @Test
        @DisplayName("岗位限应届，用户是应届 - 通过")
        void checkFreshGrad_required_userIsFresh_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkFreshGraduate", Boolean.TRUE, Boolean.TRUE);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("岗位限应届，用户非应届 - 不通过")
        void checkFreshGrad_required_userNotFresh_false() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkFreshGraduate", Boolean.FALSE, Boolean.TRUE);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("岗位不限应届 - 通过")
        void checkFreshGrad_notRequired_passes() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkFreshGraduate", Boolean.FALSE, Boolean.FALSE);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("岗位应届要求为null - 通过")
        void checkFreshGrad_nullRequirement_passes() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "checkFreshGraduate", Boolean.FALSE, (Boolean) null);
            assertThat(result).isTrue();
        }
    }

    // === 评分：专业匹配 ===

    @Nested
    @DisplayName("专业匹配评分")
    class MajorScoreTests {

        @Test
        @DisplayName("专业完全匹配 - 30分")
        void calculateScore_exactMajorMatch_30pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateMajorMatchScore", "软件工程", "软件工程");
            assertThat(score).isEqualTo(30);
        }

        @Test
        @DisplayName("专业属于同一大类 - 15分")
        void calculateScore_sameCategoryMatch_15pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateMajorMatchScore", "软件工程", "信息安全");
            assertThat(score).isEqualTo(15);
        }

        @Test
        @DisplayName("专业不匹配 - 0分")
        void calculateScore_noMajorMatch_0pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateMajorMatchScore", "法学", "软件工程");
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("岗位无专业要求 - 0分")
        void calculateScore_noMajorRequired_0pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateMajorMatchScore", "软件工程", (String) null);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("岗位要求包含用户专业（模糊匹配） - 30分")
        void calculateScore_fuzzyMatch_30pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateMajorMatchScore", "计算机", "计算机科学与技术");
            assertThat(score).isEqualTo(30);
        }
    }

    // === 评分：学历匹配 ===

    @Nested
    @DisplayName("学历匹配评分")
    class EducationScoreTests {

        @Test
        @DisplayName("学历超出要求 - 10分")
        void calculateScore_educationExceeds_10pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateEducationMatchScore", "硕士", "本科");
            assertThat(score).isEqualTo(10);
        }

        @Test
        @DisplayName("学历刚好满足 - 5分")
        void calculateScore_educationEquals_5pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateEducationMatchScore", "本科", "本科");
            assertThat(score).isEqualTo(5);
        }

        @Test
        @DisplayName("岗位无学历要求 - 5分")
        void calculateScore_noRequirement_5pts() {
            Integer score = ReflectionTestUtils.invokeMethod(
                    matchService, "calculateEducationMatchScore", "本科", (String) null);
            assertThat(score).isEqualTo(5);
        }
    }

    // === 专业分类 ===

    @Nested
    @DisplayName("专业分类判断")
    class CategoryTests {

        @Test
        @DisplayName("同一大类 - true")
        void isInSameCategory_sameCategory_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "isInSameCategory", "软件工程", "网络工程");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不同大类 - false")
        void isInSameCategory_diffCategory_false() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "isInSameCategory", "法学", "软件工程");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("岗位要求为大类名，用户专业在该大类 - true")
        void isInSameCategory_categoryNameMatch_true() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                    matchService, "isInSameCategory", "软件工程", "计算机类");
            assertThat(result).isTrue();
        }
    }

    // === 推荐整合测试 ===

    @Nested
    @DisplayName("推荐整合测试")
    class RecommendTests {

        @Test
        @DisplayName("过滤不满足硬条件的岗位并按分数排序")
        void recommend_filtersAndSortsByScore() {
            Position p1 = createPosition("计算机类", "本科", "不限", false);
            Position p2 = createPosition("软件工程", "本科", "不限", false);
            Position p3 = createPosition("软件工程", "博士", "不限", false); // 用户学历不足

            when(positionService.list(any())).thenReturn(Arrays.asList(p1, p2, p3));

            List<MatchResultDto> results = matchService.recommend(userArchive);

            // p3 因学历不够被过滤，p2 完全匹配分数高于 p1
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMatchScore()).isGreaterThanOrEqualTo(results.get(1).getMatchScore());
        }

        @Test
        @DisplayName("无可用岗位时返回空列表")
        void recommend_noPositions_emptyList() {
            when(positionService.list(any())).thenReturn(Collections.emptyList());

            List<MatchResultDto> results = matchService.recommend(userArchive);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("所有岗位都不满足硬条件时返回空列表")
        void recommend_allFiltered_emptyList() {
            Position p1 = createPosition("软件工程", "博士", "不限", false); // 学历不够

            when(positionService.list(any())).thenReturn(Collections.singletonList(p1));

            List<MatchResultDto> results = matchService.recommend(userArchive);

            assertThat(results).isEmpty();
        }
    }

    // === 辅助方法 ===

    private Position createPosition(String majorRequired, String educationRequired,
                                     String politicalRequired, boolean isFreshOnly) {
        Position p = new Position();
        p.setMajorRequired(majorRequired);
        p.setEducationRequired(educationRequired);
        p.setPoliticalStatusRequired(politicalRequired);
        p.setIsFreshOnly(isFreshOnly);
        p.setDepartment("测试部门");
        p.setPositionName("测试岗位");
        p.setYear(2026);
        p.setRecruitmentNumber(1);
        return p;
    }
}
