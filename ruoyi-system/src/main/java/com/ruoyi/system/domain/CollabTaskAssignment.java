package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Collaborative task assignment (collab_task_assignment).
 */
public class CollabTaskAssignment
{
    private Long assignmentId;

    private Long taskId;

    private Long userId;

    private String scopeType;

    private String scopeJson;

    private String submitStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submittedAt;

    private String draftContent;

    private String contentSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getAssignmentId()
    {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId)
    {
        this.assignmentId = assignmentId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getScopeType()
    {
        return scopeType;
    }

    public void setScopeType(String scopeType)
    {
        this.scopeType = scopeType;
    }

    public String getScopeJson()
    {
        return scopeJson;
    }

    public void setScopeJson(String scopeJson)
    {
        this.scopeJson = scopeJson;
    }

    public String getSubmitStatus()
    {
        return submitStatus;
    }

    public void setSubmitStatus(String submitStatus)
    {
        this.submitStatus = submitStatus;
    }

    public Date getSubmittedAt()
    {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt)
    {
        this.submittedAt = submittedAt;
    }

    public String getDraftContent()
    {
        return draftContent;
    }

    public void setDraftContent(String draftContent)
    {
        this.draftContent = draftContent;
    }

    public String getContentSnapshot()
    {
        return contentSnapshot;
    }

    public void setContentSnapshot(String contentSnapshot)
    {
        this.contentSnapshot = contentSnapshot;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("assignmentId", getAssignmentId())
            .append("taskId", getTaskId())
            .append("userId", getUserId())
            .append("scopeType", getScopeType())
            .append("scopeJson", getScopeJson())
            .append("submitStatus", getSubmitStatus())
            .append("submittedAt", getSubmittedAt())
            .append("draftContent", getDraftContent())
            .append("contentSnapshot", getContentSnapshot())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
