package com.ruoyi.system.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SceneDevice;
import com.ruoyi.system.mapper.SceneDeviceMapper;
import com.ruoyi.system.mapper.SceneProbeStateMapper;
import com.ruoyi.system.service.ISceneDeviceService;

/**
 * Scene device service layer.
 */
@Service
public class SceneDeviceServiceImpl implements ISceneDeviceService
{
    private static final Pattern MAC = Pattern.compile("(?i)^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");
    private static final Pattern IP = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    private static final Set<String> VALID_TYPES = Set.of("router", "switch", "terminal", "other");

    @Autowired
    private SceneDeviceMapper sceneDeviceMapper;

    @Autowired
    private SceneProbeStateMapper sceneProbeStateMapper;

    @Override
    public List<SceneDevice> selectSceneDeviceList(SceneDevice sceneDevice)
    {
        return sceneDeviceMapper.selectSceneDeviceList(sceneDevice);
    }

    @Override
    public SceneDevice selectSceneDeviceById(String deviceId)
    {
        return sceneDeviceMapper.selectSceneDeviceById(deviceId);
    }

    @Override
    public int insertSceneDevice(SceneDevice sceneDevice)
    {
        validateDevice(sceneDevice, true);
        if (StringUtils.isBlank(sceneDevice.getDelFlag()))
        {
            sceneDevice.setDelFlag("0");
        }
        return sceneDeviceMapper.insertSceneDevice(sceneDevice);
    }

    @Override
    public int updateSceneDevice(SceneDevice sceneDevice)
    {
        validateDevice(sceneDevice, false);
        return sceneDeviceMapper.updateSceneDevice(sceneDevice);
    }

    @Override
    public int deleteSceneDeviceByIds(String[] deviceIds)
    {
        int rows = sceneDeviceMapper.deleteSceneDeviceByIds(deviceIds);
        for (String deviceId : deviceIds)
        {
            sceneDeviceMapper.updateParentNullByParentId(deviceId);
            sceneProbeStateMapper.deleteByDeviceId(deviceId);
        }
        return rows;
    }

    private void validateDevice(SceneDevice device, boolean isInsert)
    {
        if (isInsert && StringUtils.isBlank(device.getDeviceId()))
        {
            device.setDeviceId("dev-" + System.currentTimeMillis());
        }
        if (!isInsert && StringUtils.isBlank(device.getDeviceId()))
        {
            throw new ServiceException("Device id is required");
        }
        if (StringUtils.isBlank(device.getDeviceName()))
        {
            throw new ServiceException("Device name is required");
        }
        if (StringUtils.isBlank(device.getIp()) || !IP.matcher(device.getIp().trim()).matches())
        {
            throw new ServiceException("Invalid IP address format");
        }
        device.setIp(device.getIp().trim());
        if (StringUtils.isBlank(device.getDeviceType()) || !VALID_TYPES.contains(device.getDeviceType()))
        {
            throw new ServiceException("Invalid device type");
        }
        if (StringUtils.isBlank(device.getBuildingId()))
        {
            throw new ServiceException("Building id is required");
        }
        checkIpUnique(device);
        device.setMac(normalizeMac(device.getMac()));
        checkMacUnique(device);
        validateParent(device);
    }

    private static String normalizeMac(String mac)
    {
        if (StringUtils.isBlank(mac))
        {
            return null;
        }
        String trimmed = mac.trim();
        if (!MAC.matcher(trimmed).matches())
        {
            throw new ServiceException("Invalid MAC address format");
        }
        return trimmed.replace('-', ':').toUpperCase();
    }

    private void checkIpUnique(SceneDevice device)
    {
        SceneDevice info = sceneDeviceMapper.checkIpUnique(device.getIp());
        String deviceId = StringUtils.isNull(device.getDeviceId()) ? "" : device.getDeviceId();
        if (StringUtils.isNotNull(info) && !info.getDeviceId().equals(deviceId))
        {
            throw new ServiceException("IP address already exists");
        }
    }

    private void checkMacUnique(SceneDevice device)
    {
        if (device.getMac() == null)
        {
            return;
        }
        SceneDevice info = sceneDeviceMapper.checkMacUnique(device.getMac());
        String deviceId = StringUtils.isNull(device.getDeviceId()) ? "" : device.getDeviceId();
        if (StringUtils.isNotNull(info) && !info.getDeviceId().equals(deviceId))
        {
            throw new ServiceException("MAC address already exists");
        }
    }

    private void validateParent(SceneDevice device)
    {
        String parentId = device.getParentDeviceId();
        if (StringUtils.isBlank(parentId))
        {
            device.setParentDeviceId(null);
            return;
        }
        parentId = parentId.trim();
        String deviceId = device.getDeviceId();
        if (parentId.equals(deviceId))
        {
            throw new ServiceException("Cannot set device as its own parent");
        }
        SceneDevice parent = sceneDeviceMapper.selectSceneDeviceById(parentId);
        if (parent == null)
        {
            throw new ServiceException("Parent device does not exist");
        }
        Set<String> visited = new HashSet<>();
        String current = parentId;
        while (current != null)
        {
            if (current.equals(deviceId))
            {
                throw new ServiceException("Parent device would create a cycle");
            }
            if (visited.contains(current))
            {
                throw new ServiceException("Parent device would create a cycle");
            }
            visited.add(current);
            SceneDevice node = sceneDeviceMapper.selectSceneDeviceById(current);
            if (node == null)
            {
                break;
            }
            current = node.getParentDeviceId();
            if (StringUtils.isBlank(current))
            {
                current = null;
            }
        }
        device.setParentDeviceId(parentId);
    }
}
