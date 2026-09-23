package com.m998.civilservice.modules.match.rule.score;

import cn.hutool.core.util.StrUtil;
import com.m998.civilservice.modules.match.config.MatchConfig;
import com.m998.civilservice.modules.match.rule.ScoreResult;
import com.m998.civilservice.modules.match.rule.ScoreRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EducationScoreRule implements ScoreRule {

    @Autowired
    private MatchConfig config;

    @Override
    public ScoreResult score(UserArchive archive, Position position) {
        String required = position.getEducationRequired();
        if (StrUtil.isBlank(required)) {
            return ScoreResult.of(5, "岗位无学历要求+5分");
        }
        int userLevel = config.getEducationLevels().getOrDefault(archive.getEducation(), 0);
        int requiredLevel = config.getEducationLevels().getOrDefault(required, 0);
        if (userLevel > requiredLevel) {
            int pts = config.getScoreWeights().getOrDefault("educationExceed", 10);
            return ScoreResult.of(pts, "学历超出要求+" + pts + "分");
        } else if (userLevel == requiredLevel) {
            int pts = config.getScoreWeights().getOrDefault("educationMatch", 5);
            return ScoreResult.of(pts, "学历刚好满足+" + pts + "分");
        }
        return ScoreResult.zero();
    }
}
