package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SceneDevice;

/**
 * Scene device service layer.
 */
public interface ISceneDeviceService
{
    /**
     * Query scene device list.
     *
     * @param sceneDevice query criteria
     * @return device list
     */
    public List<SceneDevice> selectSceneDeviceList(SceneDevice sceneDevice);

    /**
     * Query scene device by id.
     *
     * @param deviceId device id
     * @return device or null
     */
    public SceneDevice selectSceneDeviceById(String deviceId);

    /**
     * Insert scene device.
     *
     * @param sceneDevice device to insert
     * @return rows affected
     */
    public int insertSceneDevice(SceneDevice sceneDevice);

    /**
     * Update scene device.
     *
     * @param sceneDevice device to update
     * @return rows affected
     */
    public int updateSceneDevice(SceneDevice sceneDevice);

    /**
     * Soft-delete scene devices by ids and clear child parent links.
     *
     * @param deviceIds device ids
     * @return rows affected
     */
    public int deleteSceneDeviceByIds(String[] deviceIds);
}
