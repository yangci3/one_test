package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.SceneMonitorSettings;

/**
 * Mapper for per-user scene monitor settings.
 */
public interface SceneMonitorSettingsMapper
{
    /**
     * Select settings by user id.
     *
     * @param userId user id
     * @return settings or null
     */
    SceneMonitorSettings selectByUserId(Long userId);

    /**
     * Insert new settings.
     *
     * @param settings settings
     * @return rows affected
     */
    int insert(SceneMonitorSettings settings);

    /**
     * Update existing settings.
     *
     * @param settings settings
     * @return rows affected
     */
    int update(SceneMonitorSettings settings);

    /**
     * Delete settings by user id.
     *
     * @param userId user id
     * @return rows affected
     */
    int deleteByUserId(Long userId);
}
