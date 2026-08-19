package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.vo.CollabUserCandidateVo;

/**
 * Collaborative user service layer.
 */
public interface ICollabUserService
{
    /**
     * List users eligible for collab task assignment.
     *
     * @return enabled users with collab_member role
     */
    public List<CollabUserCandidateVo> listCandidates();

    /**
     * Whether the user has the collab_member role and is enabled.
     *
     * @param userId user id
     * @return true if user is an eligible collab member
     */
    public boolean isCollabMember(Long userId);
}
