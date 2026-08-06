package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SceneProbeState;

/**
 * Scene probe state data access layer.
 */
public interface SceneProbeStateMapper
{
    public List<SceneProbeState> selectProbeStateList(@Param("buildingId") String buildingId);

    public List<String> selectActiveDeviceIds(@Param("buildingId") String buildingId);

    public SceneProbeState selectByDeviceId(String deviceId);

    public int insertSceneProbeState(SceneProbeState state);

    public int updateSceneProbeState(SceneProbeState state);

    public int deleteByDeviceId(String deviceId);

    public List<SceneProbeState> selectMonitoringActive();
}
