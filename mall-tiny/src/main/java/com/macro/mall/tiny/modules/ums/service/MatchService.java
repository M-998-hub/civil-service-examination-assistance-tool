package com.macro.mall.tiny.modules.ums.service;

import com.macro.mall.tiny.modules.ums.dto.MatchResultDto;
import com.macro.mall.tiny.modules.ums.model.UserArchive;

import java.util.List;

/**
 * 岗位匹配服务
 * Created by macro on 2026-04-03.
 */
public interface MatchService {

    /**
     * 根据用户档案推荐匹配岗位
     * @param userArchive 用户档案
     * @return 匹配结果列表，按匹配度从高到低排序
     */
    List<MatchResultDto> recommend(UserArchive userArchive);
}
