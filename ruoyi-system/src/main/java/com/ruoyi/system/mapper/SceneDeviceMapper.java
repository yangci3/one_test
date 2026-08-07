package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SceneDevice;

/**
 * Scene device data access layer.
 */
public interface SceneDeviceMapper
{
    public List<SceneDevice> selectSceneDeviceList(SceneDevice sceneDevice);

    public SceneDevice selectSceneDeviceById(String deviceId);

    public List<SceneDevice> selectSceneDeviceByIds(@Param("deviceIds") List<String> deviceIds);

    public int insertSceneDevice(SceneDevice sceneDevice);

    public int updateSceneDevice(SceneDevice sceneDevice);

    public SceneDevice checkIpUnique(String ip);

    public SceneDevice checkMacUnique(String mac);

    public int updateParentNullByParentId(String parentDeviceId);

    public int deleteSceneDeviceByIds(String[] deviceIds);
}
