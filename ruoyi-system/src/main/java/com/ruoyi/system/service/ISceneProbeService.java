package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.SceneProbeEvent;
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

    /**
     * List probe events in the given time window, enriched with device name and ip.
     *
     * @param deviceId optional device filter
     * @param from optional start timestamp (ms)
     * @param to optional end timestamp (ms)
     * @return enriched events
     */
    public List<SceneProbeEvent> listEvents(String deviceId, Long from, Long to);

    /**
     * Get probe history for a single device, enriched with device name and ip.
     *
     * @param deviceId device id
     * @param from optional start timestamp (ms)
     * @param to optional end timestamp (ms)
     * @return map with deviceId, deviceName, ip, events
     */
    public Map<String, Object> getDeviceHistory(String deviceId, Long from, Long to);
}
