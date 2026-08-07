package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SceneProbeEvent;

/**
 * Scene probe event history data access layer.
 */
public interface SceneProbeEventMapper
{
    public int insertEvent(SceneProbeEvent e);

    public List<SceneProbeEvent> selectEvents(@Param("deviceId") String deviceId, @Param("from") Long from, @Param("to") Long to);

    public int deleteByDeviceId(String deviceId);

    public int deleteOlderThan(long eventAtBefore);

    public long countAll();

    public int deleteOldestBeyond(int keepCount);
}
