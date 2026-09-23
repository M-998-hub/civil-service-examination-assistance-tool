package com.m998.civilservice.modules.match.rule;

import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;

public interface ScoreRule {
    ScoreResult score(UserArchive archive, Position position);
}
