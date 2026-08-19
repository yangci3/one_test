package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.vo.CollabUserCandidateVo;
import com.ruoyi.system.mapper.CollabUserMapper;
import com.ruoyi.system.service.ICollabUserService;

/**
 * Collaborative user service layer.
 */
@Service
public class CollabUserServiceImpl implements ICollabUserService
{
    @Autowired
    private CollabUserMapper collabUserMapper;

    @Override
    public List<CollabUserCandidateVo> listCandidates()
    {
        return collabUserMapper.selectCollabMemberCandidates();
    }

    @Override
    public boolean isCollabMember(Long userId)
    {
        return userId != null && collabUserMapper.isCollabMember(userId) > 0;
    }
}
