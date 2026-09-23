package com.m998.civilservice.modules.match.rule.score;

import cn.hutool.core.util.StrUtil;
import com.m998.civilservice.modules.match.config.MatchConfig;
import com.m998.civilservice.modules.match.rule.ScoreResult;
import com.m998.civilservice.modules.match.rule.ScoreRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MajorScoreRule implements ScoreRule {

    @Autowired
    private MatchConfig config;

    @Override
    public ScoreResult score(UserArchive archive, Position position) {
        String requiredMajor = position.getMajorRequired();
        if (StrUtil.isBlank(requiredMajor)) {
            return ScoreResult.zero();
        }
        String userMajor = archive.getMajor();
        if (StrUtil.isBlank(userMajor)) {
            return ScoreResult.zero();
        }
        // 完全匹配
        if (requiredMajor.equals(userMajor) || requiredMajor.contains(userMajor) || userMajor.contains(requiredMajor)) {
            int pts = config.getScoreWeights().getOrDefault("majorExact", 30);
            return ScoreResult.of(pts, "专业完全匹配+" + pts + "分");
        }
        // 大类匹配
        if (isInSameCategory(userMajor, requiredMajor)) {
            int pts = config.getScoreWeights().getOrDefault("majorCategory", 15);
            return ScoreResult.of(pts, "专业属于相关大类+" + pts + "分");
        }
        return ScoreResult.zero();
    }

    private boolean isInSameCategory(String userMajor, String requiredMajor) {
        Map<String, List<String>> categories = config.getMajorCategories();
        if (categories == null) return false;
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            String catName = entry.getKey();
            List<String> majors = entry.getValue();
            boolean requiredInCat = majorMatches(catName, requiredMajor, majors);
            boolean userInCat = majorMatches(catName, userMajor, majors);
            if (requiredInCat && userInCat) return true;
        }
        return false;
    }

    private boolean majorMatches(String catName, String major, List<String> majors) {
        if (catName.equals(major) || (major != null && major.contains(catName.replace("类", "")))) return true;
        for (String m : majors) {
            if (m.equals(major) || (major != null && major.contains(m))) return true;
        }
        return false;
    }
}
