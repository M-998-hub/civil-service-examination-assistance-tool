package com.m998.civilservice.modules.match.rule.filter;

import cn.hutool.core.util.StrUtil;
import com.m998.civilservice.modules.match.config.MatchConfig;
import com.m998.civilservice.modules.match.rule.MatchRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PoliticalStatusFilter implements MatchRule {

    @Autowired
    private MatchConfig config;

    @Override
    public boolean filter(UserArchive archive, Position position) {
        String required = position.getPoliticalStatusRequired();
        if (StrUtil.isBlank(required) || "不限".equals(required)) {
            return true;
        }
        int userLevel = config.getPoliticalStatusLevels().getOrDefault(archive.getPoliticalStatus(), -1);
        int requiredLevel = config.getPoliticalStatusLevels().getOrDefault(required, -1);
        return userLevel >= requiredLevel;
    }
}
