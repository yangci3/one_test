package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SceneProbeState;
import com.ruoyi.system.mapper.SceneProbeStateMapper;
import com.ruoyi.system.service.ISceneProbeService;

/**
 * Scene probe mock service implementation.
 */
@Service
public class SceneProbeServiceImpl implements ISceneProbeService
{
    private static final String STATUS_UNKNOWN = "unknown";
    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";

    @Autowired
    private SceneProbeStateMapper sceneProbeStateMapper;

    @Value("${scene.probe.offline-prob:0.15}")
    private double offlineProb;

    @Value("${scene.probe.recover-prob:0.40}")
    private double recoverProb;

    @Override
    public List<SceneProbeState> list(String buildingId)
    {
        List<String> deviceIds = sceneProbeStateMapper.selectActiveDeviceIds(buildingId);
        List<SceneProbeState> existing = sceneProbeStateMapper.selectProbeStateList(buildingId);
        Map<String, SceneProbeState> byDeviceId = new HashMap<>();
        for (SceneProbeState state : existing)
        {
            byDeviceId.put(state.getDeviceId(), state);
        }
        List<SceneProbeState> result = new ArrayList<>(deviceIds.size());
        for (String deviceId : deviceIds)
        {
            SceneProbeState state = byDeviceId.get(deviceId);
            if (state != null)
            {
                result.add(state);
            }
            else
            {
                result.add(synthesizeUnknown(deviceId));
            }
        }
        return result;
    }

    @Override
    public int startAll()
    {
        return applyToAllActive(true);
    }

    @Override
    public int stopAll()
    {
        return applyToAllActive(false);
    }

    @Override
    public int startOne(String deviceId)
    {
        return upsertMonitoring(deviceId, true);
    }

    @Override
    public int stopOne(String deviceId)
    {
        return upsertMonitoring(deviceId, false);
    }

    @Override
    public void tick()
    {
        List<SceneProbeState> active = sceneProbeStateMapper.selectMonitoringActive();
        long nowMs = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (SceneProbeState state : active)
        {
            String status = state.getStatus();
            String newStatus = null;
            if (STATUS_ONLINE.equals(status) && random.nextDouble() < offlineProb)
            {
                newStatus = STATUS_OFFLINE;
            }
            else if (STATUS_OFFLINE.equals(status) && random.nextDouble() < recoverProb)
            {
                newStatus = STATUS_ONLINE;
            }
            if (newStatus != null)
            {
                state.setStatus(newStatus);
                state.setLastChangeAt(nowMs);
                sceneProbeStateMapper.updateSceneProbeState(state);
            }
        }
    }

    private int applyToAllActive(boolean start)
    {
        List<String> deviceIds = sceneProbeStateMapper.selectActiveDeviceIds(null);
        int rows = 0;
        for (String deviceId : deviceIds)
        {
            rows += upsertMonitoring(deviceId, start);
        }
        return rows;
    }

    private int upsertMonitoring(String deviceId, boolean start)
    {
        long nowMs = System.currentTimeMillis();
        SceneProbeState existing = sceneProbeStateMapper.selectByDeviceId(deviceId);
        if (existing != null)
        {
            if (start)
            {
                existing.setMonitoring(true);
                existing.setStatus(STATUS_ONLINE);
            }
            else
            {
                existing.setMonitoring(false);
                existing.setStatus(STATUS_UNKNOWN);
            }
            existing.setLastChangeAt(nowMs);
            return sceneProbeStateMapper.updateSceneProbeState(existing);
        }
        SceneProbeState state = new SceneProbeState();
        state.setDeviceId(deviceId);
        if (start)
        {
            state.setMonitoring(true);
            state.setStatus(STATUS_ONLINE);
        }
        else
        {
            state.setMonitoring(false);
            state.setStatus(STATUS_UNKNOWN);
        }
        state.setLastChangeAt(nowMs);
        return sceneProbeStateMapper.insertSceneProbeState(state);
    }

    private static SceneProbeState synthesizeUnknown(String deviceId)
    {
        SceneProbeState state = new SceneProbeState();
        state.setDeviceId(deviceId);
        state.setMonitoring(false);
        state.setStatus(STATUS_UNKNOWN);
        state.setLastChangeAt(null);
        return state;
    }
}
