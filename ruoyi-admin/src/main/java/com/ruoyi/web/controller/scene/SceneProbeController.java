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
import com.ruoyi.system.domain.SceneProbeEvent;
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

    private static final long HISTORY_WINDOW_MS = 30L * 24 * 60 * 60 * 1000;

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

    /**
     * List probe history events for the optional device and time window.
     * Default window is the last 30 days.
     */
    @PreAuthorize("@ss.hasPermi('scene:history:list')")
    @GetMapping("/history")
    public AjaxResult listHistory(@RequestParam(required = false) String deviceId,
                                  @RequestParam(required = false) Long from,
                                  @RequestParam(required = false) Long to)
    {
        long nowMs = System.currentTimeMillis();
        long windowStart = nowMs - HISTORY_WINDOW_MS;
        long fromMs = from != null ? from : windowStart;
        long toMs = to != null ? to : nowMs;
        List<SceneProbeEvent> list = probeService.listEvents(deviceId, fromMs, toMs);
        return success(list);
    }

    /**
     * Get probe history for a single device.
     * Default window is the last 30 days.
     */
    @PreAuthorize("@ss.hasPermi('scene:history:list')")
    @GetMapping("/history/{deviceId}")
    public AjaxResult getDeviceHistory(@PathVariable String deviceId,
                                       @RequestParam(required = false) Long from,
                                       @RequestParam(required = false) Long to)
    {
        long nowMs = System.currentTimeMillis();
        long windowStart = nowMs - HISTORY_WINDOW_MS;
        long fromMs = from != null ? from : windowStart;
        long toMs = to != null ? to : nowMs;
        return success(probeService.getDeviceHistory(deviceId, fromMs, toMs));
    }
}
