package com.ruoyi.web.controller.scene;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SceneProbeState;
import com.ruoyi.system.service.ISceneProbeService;

/**
 * Scene probe REST endpoints.
 */
@RestController
@RequestMapping("/scene/probe")
public class SceneProbeController extends BaseController
{
    @Autowired
    private ISceneProbeService probeService;

    /**
     * List probe states for active devices.
     */
    @PreAuthorize("@ss.hasPermi('scene:probe:query')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String buildingId)
    {
        List<SceneProbeState> list = probeService.list(buildingId);
        return success(list);
    }

    /**
     * Start monitoring all active devices.
     */
    @PreAuthorize("@ss.hasPermi('scene:probe:edit')")
    @Log(title = "Scene probe", businessType = BusinessType.UPDATE)
    @PostMapping("/startAll")
    public AjaxResult startAll()
    {
        return toAjax(probeService.startAll());
    }

    /**
     * Stop monitoring all active devices.
     */
    @PreAuthorize("@ss.hasPermi('scene:probe:edit')")
    @Log(title = "Scene probe", businessType = BusinessType.UPDATE)
    @PostMapping("/stopAll")
    public AjaxResult stopAll()
    {
        return toAjax(probeService.stopAll());
    }

    /**
     * Start monitoring one device.
     */
    @PreAuthorize("@ss.hasPermi('scene:probe:edit')")
    @Log(title = "Scene probe", businessType = BusinessType.UPDATE)
    @PostMapping("/start/{deviceId}")
    public AjaxResult startOne(@PathVariable String deviceId)
    {
        return toAjax(probeService.startOne(deviceId));
    }

    /**
     * Stop monitoring one device.
     */
    @PreAuthorize("@ss.hasPermi('scene:probe:edit')")
    @Log(title = "Scene probe", businessType = BusinessType.UPDATE)
    @PostMapping("/stop/{deviceId}")
    public AjaxResult stopOne(@PathVariable String deviceId)
    {
        return toAjax(probeService.stopOne(deviceId));
    }
}
