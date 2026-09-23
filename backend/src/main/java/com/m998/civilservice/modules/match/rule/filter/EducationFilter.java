package com.m998.civilservice.modules.match.rule.filter;

import cn.hutool.core.util.StrUtil;
import com.m998.civilservice.modules.match.config.MatchConfig;
import com.m998.civilservice.modules.match.rule.MatchRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EducationFilter implements MatchRule {

    @Autowired
    private MatchConfig config;

    @Override
    public boolean filter(UserArchive archive, Position position) {
        String required = position.getEducationRequired();
        if (StrUtil.isBlank(required)) {
            return true;
        }
        int userLevel = config.getEducationLevels().getOrDefault(archive.getEducation(), -1);
        int requiredLevel = config.getEducationLevels().getOrDefault(required, -1);
        return userLevel >= requiredLevel;
    }
}
