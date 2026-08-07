package com.ruoyi.web.controller.scene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SceneMonitorSettings;
import com.ruoyi.system.service.ISceneMonitorSettingsService;

/**
 * Per-user scene monitor settings REST endpoints.
 */
@RestController
@RequestMapping("/scene/monitor/settings")
public class SceneMonitorSettingsController extends BaseController
{
    @Autowired
    private ISceneMonitorSettingsService settingsService;

    /**
     * Get current user's monitor settings. Returns defaults if no row exists.
     */
    @PreAuthorize("@ss.hasPermi('scene:settings:query')")
    @GetMapping
    public AjaxResult get()
    {
        Long userId = SecurityUtils.getUserId();
        return success(settingsService.getSettings(userId));
    }

    /**
     * Save current user's monitor settings.
     */
    @PreAuthorize("@ss.hasPermi('scene:settings:edit')")
    @Log(title = "Scene monitor settings", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SceneMonitorSettings settings)
    {
        Long userId = SecurityUtils.getUserId();
        return success(settingsService.saveSettings(userId, settings));
    }

    /**
     * Reset current user's monitor settings to defaults.
     */
    @PreAuthorize("@ss.hasPermi('scene:settings:edit')")
    @Log(title = "Scene monitor settings", businessType = BusinessType.UPDATE)
    @PostMapping("/reset")
    public AjaxResult reset()
    {
        Long userId = SecurityUtils.getUserId();
        return success(settingsService.resetSettings(userId));
    }
}
