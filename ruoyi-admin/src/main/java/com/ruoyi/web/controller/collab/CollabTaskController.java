package com.ruoyi.web.controller.collab;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.CollabTask;
import com.ruoyi.system.domain.vo.CollabTaskCreateVo;
import com.ruoyi.system.service.ICollabTaskService;

/**
 * Collaborative task REST endpoints.
 */
@RestController
@RequestMapping("/collab/task")
public class CollabTaskController extends BaseController
{
    @Autowired
    private ICollabTaskService collabTaskService;

    /**
     * Query collaborative task list (admin).
     */
    @PreAuthorize("@ss.hasPermi('collab:task:list')")
    @GetMapping("/list")
    public AjaxResult list(CollabTask query)
    {
        List<CollabTask> list = collabTaskService.selectCollabTaskList(query);
        return success(list);
    }

    /**
     * Query tasks assigned to the current user.
     */
    @PreAuthorize("@ss.hasPermi('collab:task:mine')")
    @GetMapping("/mine")
    public AjaxResult mine(CollabTask query)
    {
        List<CollabTask> list = collabTaskService.selectMyCollabTaskList(query);
        return success(list);
    }

    /**
     * Create collaborative task with assignments.
     */
    @PreAuthorize("@ss.hasPermi('collab:task:add')")
    @Log(title = "Collab task", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CollabTaskCreateVo createVo)
    {
        return success(collabTaskService.createTask(createVo));
    }

    /**
     * Get collaborative task detail.
     */
    @PreAuthorize("@ss.hasAnyPermi('collab:task:list,collab:task:mine')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        return success(collabTaskService.selectCollabTaskDetail(taskId));
    }
}
