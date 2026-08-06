package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SceneProbeState;

/**
 * Scene probe mock service.
 */
public interface ISceneProbeService
{
    /**
     * List probe state for each active device (synthesizes unknown when no row).
     *
     * @param buildingId optional building filter
     * @return probe states
     */
    public List<SceneProbeState> list(String buildingId);

    /**
     * Start monitoring all active devices.
     *
     * @return rows affected
     */
    public int startAll();

    /**
     * Stop monitoring all active devices.
     *
     * @return rows affected
     */
    public int stopAll();

    /**
     * Start monitoring one device.
     *
     * @param deviceId device id
     * @return rows affected
     */
    public int startOne(String deviceId);

    /**
     * Stop monitoring one device.
     *
     * @param deviceId device id
     * @return rows affected
     */
    public int stopOne(String deviceId);

    /**
     * Probabilistic status flip for monitoring devices.
     */
    public void tick();
}
