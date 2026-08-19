package com.ruoyi.system.domain.vo;

import java.util.List;
import com.ruoyi.system.domain.CollabTask;
import com.ruoyi.system.domain.CollabTaskAssignment;

/**
 * Collaborative task detail with assignments and document title.
 */
public class CollabTaskDetailVo
{
    private CollabTask task;

    private String docTitle;

    private List<CollabTaskAssignment> assignments;

    public CollabTask getTask()
    {
        return task;
    }

    public void setTask(CollabTask task)
    {
        this.task = task;
    }

    public String getDocTitle()
    {
        return docTitle;
    }

    public void setDocTitle(String docTitle)
    {
        this.docTitle = docTitle;
    }

    public List<CollabTaskAssignment> getAssignments()
    {
        return assignments;
    }

    public void setAssignments(List<CollabTaskAssignment> assignments)
    {
        this.assignments = assignments;
    }
}
