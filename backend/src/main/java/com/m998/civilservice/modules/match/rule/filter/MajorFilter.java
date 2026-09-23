package com.m998.civilservice.modules.match.rule.filter;

import cn.hutool.core.util.StrUtil;
import com.m998.civilservice.modules.match.config.MatchConfig;
import com.m998.civilservice.modules.match.rule.MatchRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MajorFilter implements MatchRule {

    @Autowired
    private MatchConfig config;

    @Override
    public boolean filter(UserArchive archive, Position position) {
        String requiredMajor = position.getMajorRequired();
        if (StrUtil.isBlank(requiredMajor)) {
            return true;
        }
        String userMajor = archive.getMajor();
        if (StrUtil.isBlank(userMajor)) {
            return false;
        }
        // 完全匹配或包含
        if (requiredMajor.equals(userMajor) || requiredMajor.contains(userMajor) || userMajor.contains(requiredMajor)) {
            return true;
        }
        // 大类匹配
        return isInSameCategory(userMajor, requiredMajor);
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
