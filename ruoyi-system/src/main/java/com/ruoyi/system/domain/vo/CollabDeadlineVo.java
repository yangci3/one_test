package com.ruoyi.system.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Deadline extension payload for a collaborative task.
 */
public class CollabDeadlineVo
{
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadlineAt;

    public Date getDeadlineAt()
    {
        return deadlineAt;
    }

    public void setDeadlineAt(Date deadlineAt)
    {
        this.deadlineAt = deadlineAt;
    }
}
