package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SceneDevice;
import com.ruoyi.system.domain.SceneTopologyEdge;
import com.ruoyi.system.domain.SceneTopologyGraphVo;
import com.ruoyi.system.domain.SceneTopologyLinkVo;
import com.ruoyi.system.domain.SceneTopologyNode;
import com.ruoyi.system.mapper.SceneDeviceMapper;
import com.ruoyi.system.service.ISceneTopologyService;
import com.ruoyi.system.service.topology.SceneBuildingNameCatalog;
import com.ruoyi.system.service.topology.SceneTopologyGraph;

/**
 * Topology service: devices from MySQL, edges from parentDeviceId.
 */
@Service
public class SceneTopologyServiceImpl implements ISceneTopologyService
{
    private static final String MSG_DEVICE_MISSING = "\u8bbe\u5907\u4e0d\u5b58\u5728";

    private static final String MSG_LINK_MISSING = "\u94fe\u8def\u4e0d\u5b58\u5728";

    @Autowired
    private SceneDeviceMapper sceneDeviceMapper;

    @Autowired
    private SceneBuildingNameCatalog buildingNames;

    @Override
    public SceneTopologyGraphVo getGraph(String focusDeviceId)
    {
        SceneTopologyGraphVo full = SceneTopologyGraph.buildGraph(loadNodes());
        if (focusDeviceId != null && !focusDeviceId.trim().isEmpty())
        {
            boolean exists = false;
            for (SceneTopologyNode n : full.getNodes())
            {
                if (focusDeviceId.equals(n.getId()))
                {
                    exists = true;
                    break;
                }
            }
            if (!exists)
            {
                throw new ServiceException(MSG_DEVICE_MISSING, 500);
            }
            return SceneTopologyGraph.focusSubgraph(full, focusDeviceId);
        }
        return full;
    }

    @Override
    public SceneTopologyLinkVo getLink(String edgeId)
    {
        List<SceneTopologyNode> nodes = loadNodes();
        SceneTopologyGraphVo full = SceneTopologyGraph.buildGraph(nodes);
        Set<String> ids = new HashSet<>();
        for (SceneTopologyNode n : full.getNodes())
        {
            ids.add(n.getId());
        }
        String[] parsed = SceneTopologyGraph.parseEdgeKey(edgeId, ids);
        if (parsed == null)
        {
            throw new ServiceException(MSG_LINK_MISSING, 500);
        }
        SceneTopologyEdge found = null;
        for (SceneTopologyEdge e : full.getEdges())
        {
            if (e.getId() != null && e.getId().equals(edgeId))
            {
                found = e;
                break;
            }
        }
        if (found == null)
        {
            throw new ServiceException(MSG_LINK_MISSING, 500);
        }
        SceneTopologyNode from = findNode(full.getNodes(), parsed[0]);
        SceneTopologyNode to = findNode(full.getNodes(), parsed[1]);
        if (from == null || to == null)
        {
            throw new ServiceException(MSG_LINK_MISSING, 500);
        }
        SceneTopologyLinkVo vo = new SceneTopologyLinkVo();
        vo.setEdgeId(found.getId());
        vo.setFrom(from);
        vo.setTo(to);
        return vo;
    }

    private List<SceneTopologyNode> loadNodes()
    {
        List<SceneDevice> devices = sceneDeviceMapper.selectSceneDeviceList(new SceneDevice());
        List<SceneTopologyNode> nodes = new ArrayList<>();
        if (devices == null)
        {
            return nodes;
        }
        for (SceneDevice d : devices)
        {
            if (d == null || d.getDeviceId() == null || d.getDeviceId().trim().isEmpty())
            {
                continue;
            }
            SceneTopologyNode n = new SceneTopologyNode();
            n.setId(d.getDeviceId());
            n.setName(d.getDeviceName() != null && !d.getDeviceName().trim().isEmpty()
                ? d.getDeviceName() : d.getDeviceId());
            n.setIp(d.getIp() != null ? d.getIp() : "");
            n.setType(d.getDeviceType() != null && !d.getDeviceType().trim().isEmpty()
                ? d.getDeviceType() : "other");
            n.setBuildingId(d.getBuildingId());
            n.setParentDeviceId(d.getParentDeviceId());
            n.setBuildingName(buildingNames.nameOf(d.getBuildingId()));
            nodes.add(n);
        }
        return nodes;
    }

    private static SceneTopologyNode findNode(List<SceneTopologyNode> nodes, String id)
    {
        for (SceneTopologyNode n : nodes)
        {
            if (id.equals(n.getId()))
            {
                return n;
            }
        }
        return null;
    }
}
