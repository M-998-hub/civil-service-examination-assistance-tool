package com.m998.civilservice.modules.match.engine;

import com.m998.civilservice.modules.match.dto.MatchResultDto;
import com.m998.civilservice.modules.match.rule.MatchRule;
import com.m998.civilservice.modules.match.rule.ScoreResult;
import com.m998.civilservice.modules.match.rule.ScoreRule;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class MatchEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchEngine.class);

    @Autowired
    private List<MatchRule> filters;

    @Autowired
    private List<ScoreRule> scorers;

    public List<MatchResultDto> recommend(UserArchive archive, List<Position> positions) {
        LOGGER.info("MatchEngine: filtering {} positions with {} filters, scoring with {} scorers",
                positions.size(), filters.size(), scorers.size());

        List<MatchResultDto> results = new ArrayList<>();
        int checked = 0, passed = 0;

        for (Position pos : positions) {
            checked++;
            if (!passFilters(archive, pos)) continue;
            passed++;

            int totalScore = 0;
            List<String> details = new ArrayList<>();
            for (ScoreRule rule : scorers) {
                ScoreResult sr = rule.score(archive, pos);
                if (sr.getScore() > 0) {
                    totalScore += sr.getScore();
                    if (sr.getDetail() != null) details.add(sr.getDetail());
                }
            }
            results.add(new MatchResultDto(pos, totalScore, details));
        }

        results.sort(Comparator.comparingInt(MatchResultDto::getMatchScore).reversed());
        LOGGER.info("MatchEngine: checked={}, passed={}, results={}", checked, passed, results.size());
        return results;
    }

    private boolean passFilters(UserArchive archive, Position pos) {
        for (MatchRule rule : filters) {
            if (!rule.filter(archive, pos)) return false;
        }
        return true;
    }
}
