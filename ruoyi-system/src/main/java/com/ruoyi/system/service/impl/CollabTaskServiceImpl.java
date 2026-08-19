package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.CollabDoc;
import com.ruoyi.system.domain.CollabTask;
import com.ruoyi.system.domain.CollabTaskAssignment;
import com.ruoyi.system.domain.vo.CollabAssignmentVo;
import com.ruoyi.system.domain.vo.CollabTaskCreateVo;
import com.ruoyi.system.domain.vo.CollabTaskDetailVo;
import com.ruoyi.system.mapper.CollabDocMapper;
import com.ruoyi.system.mapper.CollabTaskAssignmentMapper;
import com.ruoyi.system.mapper.CollabTaskMapper;
import com.ruoyi.system.service.ICollabTaskService;
import com.ruoyi.system.service.ICollabUserService;

/**
 * Collaborative task service layer.
 */
@Service
public class CollabTaskServiceImpl implements ICollabTaskService
{
    private static final String MODE_SUBMIT = "submit";

    private static final String SCOPE_SECTION = "section";

    private static final String SUBMIT_EDITING = "editing";

    private static final String TASK_OPEN = "open";

    private static final String PERM_TASK_LIST = "collab:task:list";

    @Autowired
    private CollabTaskMapper collabTaskMapper;

    @Autowired
    private CollabTaskAssignmentMapper collabTaskAssignmentMapper;

    @Autowired
    private CollabDocMapper collabDocMapper;

    @Autowired
    private ICollabUserService collabUserService;

    @Override
    public List<CollabTask> selectCollabTaskList(CollabTask collabTask)
    {
        return collabTaskMapper.selectCollabTaskList(collabTask);
    }

    @Override
    public List<CollabTask> selectMyCollabTaskList(CollabTask collabTask)
    {
        collabTask.getParams().put("userId", SecurityUtils.getUserId());
        return collabTaskMapper.selectCollabTaskList(collabTask);
    }

    @Override
    @Transactional
    public Long createTask(CollabTaskCreateVo createVo)
    {
        validateCreateVo(createVo);

        CollabTask task = new CollabTask();
        task.setDocId(createVo.getDocId());
        task.setTitle(createVo.getTitle());
        task.setMode(StringUtils.isBlank(createVo.getMode()) ? MODE_SUBMIT : createVo.getMode());
        task.setDeadlineAt(createVo.getDeadlineAt());
        task.setTaskStatus(TASK_OPEN);
        task.setRemark(createVo.getRemark());
        task.setCreateBy(SecurityUtils.getUsername());

        collabTaskMapper.insertCollabTask(task);

        List<CollabTaskAssignment> assignments = new ArrayList<>();
        for (CollabAssignmentVo item : createVo.getAssignments())
        {
            if (!collabUserService.isCollabMember(item.getUserId()))
            {
                throw new ServiceException("\u6307\u6d3e\u7528\u6237\u4e0d\u5177\u5907\u534f\u4f5c\u8005\u8d44\u683c", HttpStatus.BAD_REQUEST);
            }

            CollabTaskAssignment assignment = new CollabTaskAssignment();
            assignment.setTaskId(task.getTaskId());
            assignment.setUserId(item.getUserId());
            assignment.setScopeType(SCOPE_SECTION);
            assignment.setScopeJson(item.getScopeJson());
            assignment.setSubmitStatus(SUBMIT_EDITING);
            assignments.add(assignment);
        }
        collabTaskAssignmentMapper.insertCollabAssignmentBatch(assignments);

        return task.getTaskId();
    }

    @Override
    public CollabTaskDetailVo selectCollabTaskDetail(Long taskId)
    {
        CollabTask task = collabTaskMapper.selectCollabTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("Task not found", HttpStatus.NOT_FOUND);
        }

        if (!SecurityUtils.hasPermi(PERM_TASK_LIST))
        {
            CollabTaskAssignment mine = collabTaskAssignmentMapper.selectByTaskAndUser(taskId, SecurityUtils.getUserId());
            if (mine == null)
            {
                throw new ServiceException("No permission to access this task", HttpStatus.FORBIDDEN);
            }
        }

        CollabDoc doc = collabDocMapper.selectCollabDocById(task.getDocId());

        CollabTaskDetailVo detail = new CollabTaskDetailVo();
        detail.setTask(task);
        detail.setDocTitle(doc != null ? doc.getTitle() : null);
        detail.setAssignments(collabTaskAssignmentMapper.selectByTaskId(taskId));
        return detail;
    }

    private void validateCreateVo(CollabTaskCreateVo createVo)
    {
        if (createVo.getDocId() == null)
        {
            throw new ServiceException("Document id is required", HttpStatus.BAD_REQUEST);
        }
        CollabDoc doc = collabDocMapper.selectCollabDocById(createVo.getDocId());
        if (doc == null)
        {
            throw new ServiceException("Document not found", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isBlank(createVo.getTitle()))
        {
            throw new ServiceException("Task title is required", HttpStatus.BAD_REQUEST);
        }
        if (createVo.getDeadlineAt() == null)
        {
            throw new ServiceException("Deadline is required", HttpStatus.BAD_REQUEST);
        }
        if (!createVo.getDeadlineAt().after(new Date()))
        {
            throw new ServiceException("Deadline must be in the future", HttpStatus.BAD_REQUEST);
        }
        if (CollectionUtils.isEmpty(createVo.getAssignments()))
        {
            throw new ServiceException("At least one assignment is required", HttpStatus.BAD_REQUEST);
        }
    }
}
