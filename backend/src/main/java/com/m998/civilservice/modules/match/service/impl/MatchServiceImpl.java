package com.m998.civilservice.modules.match.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.m998.civilservice.modules.match.dto.MatchResultDto;
import com.m998.civilservice.modules.match.engine.MatchEngine;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.profile.model.UserArchive;
import com.m998.civilservice.modules.match.service.MatchService;
import com.m998.civilservice.modules.position.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 这是一个协调者，负责日志记录，数据获取，任务委派（委派给MatchEngine），结果返回（把匹配结果返回给调用者）
 */
@Service
public class MatchServiceImpl implements MatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchServiceImpl.class);

    @Autowired
    private PositionService positionService;

    @Autowired
    private MatchEngine matchEngine;

    @Override
    public List<MatchResultDto> recommend(UserArchive userArchive) {
        LOGGER.info("start matching - archive: major={}, education={}, politicalStatus={}, fresh={}",
                userArchive.getMajor(), userArchive.getEducation(),
                userArchive.getPoliticalStatus(), userArchive.getIsFreshGraduate());

        List<Position> allPositions = positionService.list(new LambdaQueryWrapper<Position>()
                .eq(Position::getStatus, 0)
                .eq(Position::getRecruitmentStatus, "ACTIVE"));
        LOGGER.info("total positions: {}", allPositions.size());

        return matchEngine.recommend(userArchive, allPositions);
    }
}
