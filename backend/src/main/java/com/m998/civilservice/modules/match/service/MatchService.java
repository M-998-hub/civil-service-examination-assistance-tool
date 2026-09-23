package com.m998.civilservice.modules.match.service;

import com.m998.civilservice.modules.match.dto.MatchResultDto;
import com.m998.civilservice.modules.profile.model.UserArchive;

import java.util.List;

/**
 * 岗位匹配服务
 */
public interface MatchService {

    /**
     * 根据用户档案推荐匹配岗位
     * @param userArchive 用户档案
     * @return 匹配结果列表，按匹配度从高到低排序
     */
    List<MatchResultDto> recommend(UserArchive userArchive);
}
