package com.ruoyi.web.controller.scene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.ISceneTopologyService;

/**
 * Scene topology REST endpoints.
 */
@RestController
@RequestMapping("/scene/topology")
public class SceneTopologyController extends BaseController
{
    @Autowired
    private ISceneTopologyService topologyService;

    /**
     * Full graph, or neighborhood around focusDeviceId.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:list')")
    @GetMapping("/graph")
    public AjaxResult graph(@RequestParam(required = false) String focusDeviceId)
    {
        return success(topologyService.getGraph(focusDeviceId));
    }

    /**
     * Link detail for a derived parent-child edge.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:list')")
    @GetMapping("/edge/{edgeId}")
    public AjaxResult edge(@PathVariable String edgeId)
    {
        return success(topologyService.getLink(edgeId));
    }
}
