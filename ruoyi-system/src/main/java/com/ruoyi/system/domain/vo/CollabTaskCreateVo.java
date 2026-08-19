package com.ruoyi.system.domain.vo;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Payload for creating a collaborative task with assignments.
 */
public class CollabTaskCreateVo
{
    private Long docId;

    private String title;

    private String mode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadlineAt;

    private String remark;

    private List<CollabAssignmentVo> assignments;

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

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public List<CollabAssignmentVo> getAssignments()
    {
        return assignments;
    }

    public void setAssignments(List<CollabAssignmentVo> assignments)
    {
        this.assignments = assignments;
    }
}
