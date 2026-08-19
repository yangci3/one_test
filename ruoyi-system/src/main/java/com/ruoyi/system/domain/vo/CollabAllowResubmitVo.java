package com.ruoyi.system.domain.vo;

/**
 * Allow-resubmit payload for a collaborative task assignment.
 */
public class CollabAllowResubmitVo
{
    private Long assignmentId;

    public Long getAssignmentId()
    {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId)
    {
        this.assignmentId = assignmentId;
    }
}
