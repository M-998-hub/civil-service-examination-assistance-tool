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
public class PoliticalStatusScoreRule implements ScoreRule {

    @Autowired
    private MatchConfig config;

    @Override
    public ScoreResult score(UserArchive archive, Position position) {
        String required = position.getPoliticalStatusRequired();
        if (StrUtil.isBlank(required) || "不限".equals(required)) {
            return ScoreResult.zero();
        }
        int userLevel = config.getPoliticalStatusLevels().getOrDefault(archive.getPoliticalStatus(), -1);
        int requiredLevel = config.getPoliticalStatusLevels().getOrDefault(required, -1);
        if (userLevel >= requiredLevel) {
            int pts = config.getScoreWeights().getOrDefault("politicalStatus", 5);
            return ScoreResult.of(pts, "政治面貌满足+" + pts + "分");
        }
        return ScoreResult.zero();
    }
}
