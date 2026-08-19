package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.CollabDoc;
import com.ruoyi.system.domain.CollabTask;
import com.ruoyi.system.domain.CollabTaskAssignment;
import com.ruoyi.system.domain.CollabTaskDeadlineLog;
import com.ruoyi.system.domain.vo.CollabAssignmentVo;
import com.ruoyi.system.domain.vo.CollabTaskCreateVo;
import com.ruoyi.system.domain.vo.CollabTaskDetailVo;
import com.ruoyi.system.mapper.CollabDocMapper;
import com.ruoyi.system.mapper.CollabTaskAssignmentMapper;
import com.ruoyi.system.mapper.CollabTaskDeadlineLogMapper;
import com.ruoyi.system.mapper.CollabTaskMapper;
import com.ruoyi.system.service.ICollabTaskService;
import com.ruoyi.system.service.ICollabUserService;
import com.ruoyi.system.service.collab.CollabSectionHelper;

/**
 * Collaborative task service layer.
 */
@Service
public class CollabTaskServiceImpl implements ICollabTaskService
{
    private static final String MODE_SUBMIT = "submit";

    private static final String SCOPE_SECTION = "section";

    private static final String SUBMIT_EDITING = "editing";

    private static final String SUBMIT_SUBMITTED = "submitted";

    private static final String SUBMIT_OVERDUE = "overdue";

    private static final String SUBMIT_RESUBMIT_ALLOWED = "resubmit_allowed";

    private static final String TASK_OPEN = "open";

    private static final String PERM_TASK_LIST = "collab:task:list";

    @Autowired
    private CollabTaskMapper collabTaskMapper;

    @Autowired
    private CollabTaskAssignmentMapper collabTaskAssignmentMapper;

    @Autowired
    private CollabTaskDeadlineLogMapper collabTaskDeadlineLogMapper;

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
        CollabTask task = requireTask(taskId);

        if (!SecurityUtils.hasPermi(PERM_TASK_LIST))
        {
            CollabTaskAssignment mine = collabTaskAssignmentMapper.selectByTaskAndUser(taskId, SecurityUtils.getUserId());
            if (mine == null)
            {
                throw new ServiceException("No permission to access this task", HttpStatus.FORBIDDEN);
            }
        }

        refreshOverdueStatuses(task);

        CollabDoc doc = collabDocMapper.selectCollabDocById(task.getDocId());

        CollabTaskDetailVo detail = new CollabTaskDetailVo();
        detail.setTask(task);
        detail.setDocTitle(doc != null ? doc.getTitle() : null);
        detail.setAssignments(collabTaskAssignmentMapper.selectByTaskId(taskId));
        return detail;
    }

    @Override
    @Transactional
    public void saveDraft(Long taskId, Long assignmentId, String draftContent)
    {
        CollabTask task = requireTask(taskId);
        refreshOverdueStatuses(task);
        CollabTaskAssignment assignment = requireAssigneeAssignment(taskId, assignmentId);
        assertCanEdit(assignment, task);
        validateContentScope(draftContent, assignment.getScopeJson());

        assignment.setDraftContent(draftContent);
        collabTaskAssignmentMapper.updateAssignment(assignment);
    }

    @Override
    @Transactional
    public void submitAssignment(Long taskId, Long assignmentId, String content)
    {
        CollabTask task = requireTask(taskId);
        refreshOverdueStatuses(task);
        CollabTaskAssignment assignment = requireAssigneeAssignment(taskId, assignmentId);
        assertCanEdit(assignment, task);
        validateContentScope(content, assignment.getScopeJson());

        Date now = new Date();
        assignment.setSubmitStatus(SUBMIT_SUBMITTED);
        assignment.setSubmittedAt(now);
        assignment.setContentSnapshot(content);
        collabTaskAssignmentMapper.updateAssignment(assignment);
    }

    @Override
    @Transactional
    public void extendDeadline(Long taskId, Date deadlineAt)
    {
        CollabTask task = requireTask(taskId);
        if (deadlineAt == null)
        {
            throw new ServiceException("Deadline is required", HttpStatus.BAD_REQUEST);
        }
        if (!deadlineAt.after(new Date()))
        {
            throw new ServiceException("Deadline must be in the future", HttpStatus.BAD_REQUEST);
        }

        Date oldDeadline = task.getDeadlineAt();
        CollabTask update = new CollabTask();
        update.setTaskId(taskId);
        update.setDeadlineAt(deadlineAt);
        update.setUpdateBy(SecurityUtils.getUsername());
        collabTaskMapper.updateCollabTaskDeadline(update);

        CollabTaskDeadlineLog log = new CollabTaskDeadlineLog();
        log.setTaskId(taskId);
        log.setOldDeadline(oldDeadline);
        log.setNewDeadline(deadlineAt);
        log.setOperatorId(SecurityUtils.getUserId());
        collabTaskDeadlineLogMapper.insertDeadlineLog(log);
    }

    @Override
    @Transactional
    public void allowResubmit(Long taskId, Long assignmentId)
    {
        requireTask(taskId);
        CollabTaskAssignment assignment = requireAssignmentForTask(taskId, assignmentId);
        assignment.setSubmitStatus(SUBMIT_RESUBMIT_ALLOWED);
        collabTaskAssignmentMapper.updateAssignment(assignment);
    }

    @Override
    public List<CollabTaskAssignment> listUnsubmitted(Long taskId)
    {
        CollabTask task = requireTask(taskId);
        refreshOverdueStatuses(task);

        Date now = new Date();
        if (!now.after(task.getDeadlineAt()))
        {
            return new ArrayList<>();
        }

        List<CollabTaskAssignment> unsubmitted = new ArrayList<>();
        for (CollabTaskAssignment assignment : collabTaskAssignmentMapper.selectByTaskId(taskId))
        {
            String status = assignment.getSubmitStatus();
            if (SUBMIT_EDITING.equals(status) || SUBMIT_OVERDUE.equals(status))
            {
                unsubmitted.add(assignment);
            }
        }
        return unsubmitted;
    }

    @Override
    @Transactional
    public void mergeTaskSummary(Long taskId)
    {
        CollabTask task = requireTask(taskId);
        CollabDoc doc = collabDocMapper.selectCollabDocById(task.getDocId());
        if (doc == null)
        {
            throw new ServiceException("Document not found", HttpStatus.NOT_FOUND);
        }

        List<CollabTaskAssignment> assignments = collabTaskAssignmentMapper.selectByTaskId(taskId);
        Set<String> scopedSectionIds = new HashSet<>();
        Map<String, String> sectionUpdates = new HashMap<>();

        for (CollabTaskAssignment assignment : assignments)
        {
            scopedSectionIds.addAll(parseScopeSectionIds(assignment.getScopeJson()));
            if (StringUtils.isNotBlank(assignment.getContentSnapshot()))
            {
                putSnapshotSections(sectionUpdates, assignment.getContentSnapshot());
            }
        }

        for (String sectionId : scopedSectionIds)
        {
            if (!sectionUpdates.containsKey(sectionId))
            {
                sectionUpdates.put(sectionId, "<p>[unsubmitted:" + sectionId + "]</p>");
            }
        }

        String mergedHtml = CollabSectionHelper.mergeSnapshots(doc.getContentHtml(), sectionUpdates);

        CollabDoc update = new CollabDoc();
        update.setDocId(doc.getDocId());
        update.setContentHtml(mergedHtml);
        update.setUpdateBy(SecurityUtils.getUsername());
        collabDocMapper.updateCollabDoc(update);
    }

    private void putSnapshotSections(Map<String, String> sectionUpdates, String snapshotHtml)
    {
        for (String sectionId : CollabSectionHelper.listSectionIds(snapshotHtml))
        {
            String fullBlock = CollabSectionHelper.extractSections(snapshotHtml,
                    Collections.singleton(sectionId));
            String innerHtml = stripSectionWrapper(fullBlock);
            if (StringUtils.isNotBlank(innerHtml))
            {
                sectionUpdates.put(sectionId, innerHtml);
            }
        }
    }

    private String stripSectionWrapper(String fullBlock)
    {
        if (StringUtils.isBlank(fullBlock))
        {
            return fullBlock;
        }
        int openEnd = fullBlock.indexOf('>');
        if (openEnd < 0)
        {
            return fullBlock;
        }
        int closeStart = fullBlock.lastIndexOf("</div");
        if (closeStart <= openEnd)
        {
            return fullBlock.substring(openEnd + 1);
        }
        return fullBlock.substring(openEnd + 1, closeStart);
    }

    private CollabTask requireTask(Long taskId)
    {
        CollabTask task = collabTaskMapper.selectCollabTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("Task not found", HttpStatus.NOT_FOUND);
        }
        return task;
    }

    private CollabTaskAssignment requireAssigneeAssignment(Long taskId, Long assignmentId)
    {
        CollabTaskAssignment assignment = requireAssignmentForTask(taskId, assignmentId);
        if (!SecurityUtils.getUserId().equals(assignment.getUserId()))
        {
            throw new ServiceException("Only the assignee may edit this assignment", HttpStatus.FORBIDDEN);
        }
        return assignment;
    }

    private CollabTaskAssignment requireAssignmentForTask(Long taskId, Long assignmentId)
    {
        for (CollabTaskAssignment assignment : collabTaskAssignmentMapper.selectByTaskId(taskId))
        {
            if (assignmentId.equals(assignment.getAssignmentId()))
            {
                return assignment;
            }
        }
        throw new ServiceException("Assignment not found", HttpStatus.NOT_FOUND);
    }

    private void refreshOverdueStatuses(CollabTask task)
    {
        Date now = new Date();
        if (!now.after(task.getDeadlineAt()))
        {
            return;
        }
        for (CollabTaskAssignment assignment : collabTaskAssignmentMapper.selectByTaskId(task.getTaskId()))
        {
            if (SUBMIT_EDITING.equals(assignment.getSubmitStatus()))
            {
                assignment.setSubmitStatus(SUBMIT_OVERDUE);
                collabTaskAssignmentMapper.updateAssignment(assignment);
            }
        }
    }

    private boolean canEdit(CollabTaskAssignment assignment, CollabTask task)
    {
        String status = assignment.getSubmitStatus();
        if (SUBMIT_SUBMITTED.equals(status))
        {
            return false;
        }
        if (SUBMIT_RESUBMIT_ALLOWED.equals(status))
        {
            return true;
        }
        return SUBMIT_EDITING.equals(status) && !new Date().after(task.getDeadlineAt());
    }

    private void assertCanEdit(CollabTaskAssignment assignment, CollabTask task)
    {
        if (!canEdit(assignment, task))
        {
            throw new ServiceException("Assignment is not editable", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateContentScope(String html, String scopeJson)
    {
        Set<String> allowed = parseScopeSectionIds(scopeJson);
        if (allowed.isEmpty())
        {
            throw new ServiceException("Assignment scope is invalid", HttpStatus.BAD_REQUEST);
        }
        List<String> contentIds = CollabSectionHelper.listSectionIds(html);
        for (String sectionId : contentIds)
        {
            if (!allowed.contains(sectionId))
            {
                throw new ServiceException("Content contains section outside assigned scope", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private Set<String> parseScopeSectionIds(String scopeJson)
    {
        Set<String> ids = new HashSet<>();
        if (StringUtils.isBlank(scopeJson))
        {
            return ids;
        }
        JSONObject root = JSON.parseObject(scopeJson);
        JSONArray sectionIds = root.getJSONArray("sectionIds");
        if (sectionIds == null)
        {
            return ids;
        }
        for (int i = 0; i < sectionIds.size(); i++)
        {
            String id = sectionIds.getString(i);
            if (StringUtils.isNotBlank(id))
            {
                ids.add(id);
            }
        }
        return ids;
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
