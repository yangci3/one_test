package com.ruoyi.system.service;

import com.ruoyi.system.domain.SceneTopologyGraphVo;
import com.ruoyi.system.domain.SceneTopologyLinkVo;

/**
 * Scene topology graph and link lookup.
 */
public interface ISceneTopologyService
{
    /**
     * Full graph, or focus subgraph when focusDeviceId is set.
     *
     * @param focusDeviceId optional device id
     * @return graph payload
     */
    public SceneTopologyGraphVo getGraph(String focusDeviceId);

    /**
     * Link endpoints for a derived parent-child edge.
     *
     * @param edgeId edge-{from}-{to}
     * @return link payload
     */
    public SceneTopologyLinkVo getLink(String edgeId);
}
