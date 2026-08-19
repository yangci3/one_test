package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CollabTaskAssignment;

/**
 * Collaborative task assignment data access layer.
 */
public interface CollabTaskAssignmentMapper
{
    public int insertCollabAssignment(CollabTaskAssignment assignment);

    public int insertCollabAssignmentBatch(@Param("list") List<CollabTaskAssignment> assignments);

    public List<CollabTaskAssignment> selectByTaskId(Long taskId);

    public CollabTaskAssignment selectByTaskAndUser(@Param("taskId") Long taskId, @Param("userId") Long userId);

    public int updateAssignment(CollabTaskAssignment assignment);
}
