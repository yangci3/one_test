package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Collaborative task (collab_task).
 */
public class CollabTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long docId;

    private String title;

    private String mode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadlineAt;

    private String taskStatus;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getDocId()
    {
        return docId;
    }

    public void setDocId(Long docId)
    {
        this.docId = docId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public Date getDeadlineAt()
    {
        return deadlineAt;
    }

    public void setDeadlineAt(Date deadlineAt)
    {
        this.deadlineAt = deadlineAt;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("docId", getDocId())
            .append("title", getTitle())
            .append("mode", getMode())
            .append("deadlineAt", getDeadlineAt())
            .append("taskStatus", getTaskStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
