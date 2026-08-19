package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.vo.CollabUserCandidateVo;

/**
 * Collaborative user candidate queries.
 */
public interface CollabUserMapper
{
    public List<CollabUserCandidateVo> selectCollabMemberCandidates();
}
