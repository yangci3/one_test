package com.ruoyi.system.service;

import com.ruoyi.system.domain.SceneMonitorSettings;

/**
 * Per-user scene monitor settings service.
 */
public interface ISceneMonitorSettingsService
{
    /**
     * Get settings for the user, returning defaults if no row exists.
     *
     * @param userId user id
     * @return current or default settings
     */
    SceneMonitorSettings getSettings(Long userId);

    /**
     * Validate and save settings for the user (insert or update).
     *
     * @param userId user id
     * @param settings settings to save
     * @return saved settings
     */
    SceneMonitorSettings saveSettings(Long userId, SceneMonitorSettings settings);

    /**
     * Reset settings to defaults and remove the persisted row.
     *
     * @param userId user id
     * @return default settings
     */
    SceneMonitorSettings resetSettings(Long userId);
}
