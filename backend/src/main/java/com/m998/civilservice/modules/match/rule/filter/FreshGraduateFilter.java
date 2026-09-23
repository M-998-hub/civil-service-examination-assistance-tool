package com.m998.civilservice.modules.match.rule.filter;

import com.m998.civilservice.modules.match.rule.MatchRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.springframework.stereotype.Component;

@Component
public class FreshGraduateFilter implements MatchRule {

    @Override
    public boolean filter(UserArchive archive, Position position) {
        if (Boolean.TRUE.equals(position.getIsFreshOnly())) {
            return Boolean.TRUE.equals(archive.getIsFreshGraduate());
        }
        return true;
    }
}
