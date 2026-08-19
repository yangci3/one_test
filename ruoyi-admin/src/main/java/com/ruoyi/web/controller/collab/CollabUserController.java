package com.ruoyi.web.controller.collab;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.vo.CollabUserCandidateVo;
import com.ruoyi.system.service.ICollabUserService;

/**
 * Collaborative user REST endpoints.
 */
@RestController
@RequestMapping("/collab/user")
public class CollabUserController extends BaseController
{
    @Autowired
    private ICollabUserService collabUserService;

    /**
     * List eligible collab member candidates for task assignment.
     */
    @PreAuthorize("@ss.hasPermi('collab:task:add')")
    @GetMapping("/candidates")
    public AjaxResult candidates()
    {
        List<CollabUserCandidateVo> list = collabUserService.listCandidates();
        return success(list);
    }
}
