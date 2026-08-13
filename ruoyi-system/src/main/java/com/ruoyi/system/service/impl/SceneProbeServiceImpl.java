package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.ruoyi.system.service.probe.IcmpProbeReachability;
import com.ruoyi.system.service.probe.MockProbeReachability;
import com.ruoyi.system.service.probe.ProbeIpUtils;
import com.ruoyi.system.service.probe.SceneProbeReachability;

/**
 * Scene probe service implementation supporting mock and ICMP reachability modes.
 */
@Service
public class SceneProbeServiceImpl implements ISceneProbeService
{
    private static final Logger log = LoggerFactory.getLogger(SceneProbeServiceImpl.class);

    private static final String STATUS_UNKNOWN = "unknown";
    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";

    private static final String MODE_MOCK = "mock";
    private static final String MODE_ICMP = "icmp";

    private static final int IDX_FAIL_COUNT = 0;
    private static final int IDX_SUCCESS_COUNT = 1;

    @Autowired
    private SceneProbeStateMapper sceneProbeStateMapper;

    @Autowired
    private SceneProbeEventMapper sceneProbeEventMapper;

    @Autowired
    private SceneDeviceMapper sceneDeviceMapper;

    @Value("${scene.probe.mode:mock}")
    private String probeMode;

    @Value("${scene.probe.offline-prob:0.15}")
    private double offlineProb;

    @Value("${scene.probe.recover-prob:0.40}")
    private double recoverProb;

    @Value("${scene.probe.icmp.timeout-ms:2000}")
    private int icmpTimeoutMs;

    @Value("${scene.probe.icmp.fail-threshold:3}")
    private int icmpFailThreshold;

    @Value("${scene.probe.icmp.recover-threshold:1}")
    private int icmpRecoverThreshold;

    private final IcmpProbeReachability icmpReachability = new IcmpProbeReachability();

    private MockProbeReachability mockReachability;

    private final ConcurrentHashMap<String, int[]> probeCounters = new ConcurrentHashMap<>();

    private volatile boolean invalidModeWarned;

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
        int rows = applyToAllActive(false);
        probeCounters.clear();
        return rows;
    }

    @Override
    public int startOne(String deviceId)
    {
        return upsertMonitoring(deviceId, true);
    }

    @Override
    public int stopOne(String deviceId)
    {
        int rows = upsertMonitoring(deviceId, false);
        probeCounters.remove(deviceId);
        return rows;
    }

    @Override
    public void tick()
    {
        List<SceneProbeState> active = sceneProbeStateMapper.selectMonitoringActive();
        long nowMs = System.currentTimeMillis();
        boolean icmp = isIcmpMode();
        for (SceneProbeState state : active)
        {
            SceneProbeReachability.Result result = icmp
                ? icmpProbe(state)
                : getMockReachability().probe(state, null);
            applyResult(state, result, nowMs);
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
        Set<String> deviceIdSet = new HashSet<>();
        for (SceneProbeEvent event : events)
        {
            if (event.getDeviceId() != null)
            {
                deviceIdSet.add(event.getDeviceId());
            }
        }
        if (deviceIdSet.isEmpty())
        {
            return;
        }
        List<SceneDevice> devices = sceneDeviceMapper.selectSceneDeviceByIds(new ArrayList<>(deviceIdSet));
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
                probeCounters.remove(deviceId);
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
            probeCounters.remove(deviceId);
        }
        state.setLastChangeAt(nowMs);
        int rows = sceneProbeStateMapper.insertSceneProbeState(state);
        if (start && rows > 0)
        {
            recordEvent(deviceId, STATUS_ONLINE);
        }
        return rows;
    }

    private SceneProbeReachability.Result icmpProbe(SceneProbeState state)
    {
        String deviceId = state.getDeviceId();
        SceneDevice device = sceneDeviceMapper.selectSceneDeviceById(deviceId);
        String ip = device != null ? device.getIp() : null;
        if (!ProbeIpUtils.isValidIpv4(ip))
        {
            return SceneProbeReachability.Result.SKIP;
        }
        boolean reachable = icmpReachability.probe(ip, icmpTimeoutMs);
        int[] counters = probeCounters.computeIfAbsent(deviceId, k -> new int[2]);
        if (reachable)
        {
            counters[IDX_FAIL_COUNT] = 0;
            counters[IDX_SUCCESS_COUNT]++;
            if (counters[IDX_SUCCESS_COUNT] >= icmpRecoverThreshold && !STATUS_ONLINE.equals(state.getStatus()))
            {
                return SceneProbeReachability.Result.UP;
            }
        }
        else
        {
            counters[IDX_SUCCESS_COUNT] = 0;
            counters[IDX_FAIL_COUNT]++;
            if (counters[IDX_FAIL_COUNT] >= icmpFailThreshold && !STATUS_OFFLINE.equals(state.getStatus()))
            {
                return SceneProbeReachability.Result.DOWN;
            }
        }
        return SceneProbeReachability.Result.SKIP;
    }

    private MockProbeReachability getMockReachability()
    {
        if (mockReachability == null)
        {
            mockReachability = new MockProbeReachability(offlineProb, recoverProb);
        }
        return mockReachability;
    }

    private boolean isIcmpMode()
    {
        if (MODE_ICMP.equalsIgnoreCase(probeMode))
        {
            return true;
        }
        if (MODE_MOCK.equalsIgnoreCase(probeMode))
        {
            return false;
        }
        if (!invalidModeWarned)
        {
            invalidModeWarned = true;
            log.warn("Invalid scene.probe.mode '{}', falling back to mock mode", probeMode);
        }
        return false;
    }

    private void applyResult(SceneProbeState state, SceneProbeReachability.Result result, long nowMs)
    {
        if (result == SceneProbeReachability.Result.SKIP)
        {
            return;
        }
        String newStatus = result == SceneProbeReachability.Result.UP ? STATUS_ONLINE : STATUS_OFFLINE;
        if (newStatus.equals(state.getStatus()))
        {
            return;
        }
        state.setStatus(newStatus);
        state.setLastChangeAt(nowMs);
        int updated = sceneProbeStateMapper.updateSceneProbeState(state);
        if (updated > 0)
        {
            recordEvent(state.getDeviceId(), newStatus);
        }
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
