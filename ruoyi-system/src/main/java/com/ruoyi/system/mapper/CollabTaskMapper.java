package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CollabTask;

/**
 * Collaborative task data access layer.
 */
public interface CollabTaskMapper
{
    public CollabTask selectCollabTaskById(Long taskId);

    public List<CollabTask> selectCollabTaskList(CollabTask collabTask);

    public int insertCollabTask(CollabTask collabTask);

    public int updateCollabTask(CollabTask collabTask);

    public int updateCollabTaskDeadline(CollabTask collabTask);
}
