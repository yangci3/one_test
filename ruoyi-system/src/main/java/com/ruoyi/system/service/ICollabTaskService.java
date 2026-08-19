package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CollabTask;
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
}
