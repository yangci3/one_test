package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Collaborative task deadline change log (collab_task_deadline_log).
 */
public class CollabTaskDeadlineLog
{
    private Long logId;

    private Long taskId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date oldDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date newDeadline;

    private Long operatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Date getOldDeadline()
    {
        return oldDeadline;
    }

    public void setOldDeadline(Date oldDeadline)
    {
        this.oldDeadline = oldDeadline;
    }

    public Date getNewDeadline()
    {
        return newDeadline;
    }

    public void setNewDeadline(Date newDeadline)
    {
        this.newDeadline = newDeadline;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("taskId", getTaskId())
            .append("oldDeadline", getOldDeadline())
            .append("newDeadline", getNewDeadline())
            .append("operatorId", getOperatorId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
