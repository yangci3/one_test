package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SceneDevice;
import com.ruoyi.system.domain.SceneProbeEvent;
import com.ruoyi.system.domain.SceneProbeState;
import com.ruoyi.system.mapper.SceneDeviceMapper;
import com.ruoyi.system.mapper.SceneProbeEventMapper;
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

    @Autowired
    private SceneProbeEventMapper sceneProbeEventMapper;

    @Autowired
    private SceneDeviceMapper sceneDeviceMapper;

    @Value("${scene.probe.offline-prob:0.15}")
    private double offlineProb;

    @Value("${scene.probe.recover-prob:0.40}")
    private double recoverProb;

    private static final long RETENTION_MS = 30L * 24 * 3600 * 1000;

    private static final long MAX_EVENT_COUNT = 50000L;

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
                int updated = sceneProbeStateMapper.updateSceneProbeState(state);
                if (updated > 0)
                {
                    recordEvent(state.getDeviceId(), newStatus);
                }
            }
        }
    }

    @Override
    public List<SceneProbeEvent> listEvents(String deviceId, Long from, Long to)
    {
        List<SceneProbeEvent> events = sceneProbeEventMapper.selectEvents(deviceId, from, to);
        enrichEvents(events);
        return events;
    }

    @Override
    public Map<String, Object> getDeviceHistory(String deviceId, Long from, Long to)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        SceneDevice device = sceneDeviceMapper.selectSceneDeviceById(deviceId);
        result.put("deviceName", deviceNameOrId(device, deviceId));
        result.put("ip", device != null && device.getIp() != null ? device.getIp() : "");
        List<SceneProbeEvent> events = sceneProbeEventMapper.selectEvents(deviceId, from, to);
        enrichEvents(events);
        result.put("events", events);
        return result;
    }

    private void enrichEvents(List<SceneProbeEvent> events)
    {
        if (events == null || events.isEmpty())
        {
            return;
        }
        List<SceneDevice> devices = sceneDeviceMapper.selectSceneDeviceList(new SceneDevice());
        Map<String, SceneDevice> byDeviceId = new HashMap<>();
        for (SceneDevice device : devices)
        {
            byDeviceId.put(device.getDeviceId(), device);
        }
        for (SceneProbeEvent event : events)
        {
            SceneDevice device = byDeviceId.get(event.getDeviceId());
            event.setDeviceName(deviceNameOrId(device, event.getDeviceId()));
            event.setIp(device != null && device.getIp() != null ? device.getIp() : "");
        }
    }

    private static String deviceNameOrId(SceneDevice device, String deviceId)
    {
        if (device != null && device.getDeviceName() != null && !device.getDeviceName().isEmpty())
        {
            return device.getDeviceName();
        }
        return deviceId;
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
            int rows = sceneProbeStateMapper.updateSceneProbeState(existing);
            if (start && rows > 0)
            {
                recordEvent(deviceId, STATUS_ONLINE);
            }
            return rows;
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
        int rows = sceneProbeStateMapper.insertSceneProbeState(state);
        if (start && rows > 0)
        {
            recordEvent(deviceId, STATUS_ONLINE);
        }
        return rows;
    }

    private void recordEvent(String deviceId, String eventType)
    {
        long nowMs = System.currentTimeMillis();
        SceneProbeEvent event = new SceneProbeEvent();
        event.setEventId(generateEventId(nowMs));
        event.setDeviceId(deviceId);
        event.setEventType(eventType);
        event.setEventAt(nowMs);
        sceneProbeEventMapper.insertEvent(event);
        trimEvents(nowMs);
    }

    private String generateEventId(long nowMs)
    {
        int random = ThreadLocalRandom.current().nextInt(10000);
        return "ph-" + nowMs + "-" + String.format("%04d", random);
    }

    private void trimEvents(long nowMs)
    {
        sceneProbeEventMapper.deleteOlderThan(nowMs - RETENTION_MS);
        if (sceneProbeEventMapper.countAll() > MAX_EVENT_COUNT)
        {
            sceneProbeEventMapper.deleteOldestBeyond(50000);
        }
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
