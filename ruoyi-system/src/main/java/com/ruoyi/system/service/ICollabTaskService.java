package com.ruoyi.system.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.CollabTask;
import com.ruoyi.system.domain.CollabTaskAssignment;
import com.ruoyi.system.domain.vo.CollabTaskCreateVo;
import com.ruoyi.system.domain.vo.CollabTaskDetailVo;

/**
 * Collaborative task service layer.
 */
public interface ICollabTaskService
{
    /**
     * Query collaborative task list (admin).
     *
     * @param collabTask query criteria
     * @return task list
     */
    public List<CollabTask> selectCollabTaskList(CollabTask collabTask);

    /**
     * Query tasks assigned to the current user.
     *
     * @param collabTask query criteria
     * @return task list for current user
     */
    public List<CollabTask> selectMyCollabTaskList(CollabTask collabTask);

    /**
     * Create a collaborative task with assignments.
     *
     * @param createVo task create payload
     * @return new task id
     */
    public Long createTask(CollabTaskCreateVo createVo);

    /**
     * Get task detail with assignments and document title.
     *
     * @param taskId task id
     * @return task detail
     */
    public CollabTaskDetailVo selectCollabTaskDetail(Long taskId);

    /**
     * Save draft content for an assignment (assignee only).
     */
    public void saveDraft(Long taskId, Long assignmentId, String draftContent);

    /**
     * Submit assignment content (assignee only).
     */
    public void submitAssignment(Long taskId, Long assignmentId, String content);

    /**
     * Extend task deadline and record audit log.
     */
    public void extendDeadline(Long taskId, Date deadlineAt);

    /**
     * Allow a specific assignment to resubmit after deadline.
     */
    public void allowResubmit(Long taskId, Long assignmentId);

    /**
     * List assignments not submitted after the deadline has passed.
     */
    public List<CollabTaskAssignment> listUnsubmitted(Long taskId);

    /**
     * Merge assignment snapshots into the task document (admin summary).
     */
    public void mergeTaskSummary(Long taskId);
}
