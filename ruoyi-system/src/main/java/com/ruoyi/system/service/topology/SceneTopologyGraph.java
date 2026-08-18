package com.ruoyi.system.service.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.system.domain.SceneTopologyEdge;
import com.ruoyi.system.domain.SceneTopologyGraphVo;
import com.ruoyi.system.domain.SceneTopologyNode;

/**
 * Pure topology graph helpers (no Spring / no DB).
 * Ports frontend topologyGraph.js edgeId/buildGraph/focusSubgraph and topology.js parseEdgeKey.
 */
public final class SceneTopologyGraph
{
    private SceneTopologyGraph()
    {
    }

    public static String edgeId(String fromDeviceId, String toDeviceId)
    {
        return "edge-" + fromDeviceId + "-" + toDeviceId;
    }

    public static SceneTopologyGraphVo buildGraph(List<SceneTopologyNode> nodes)
    {
        SceneTopologyGraphVo vo = new SceneTopologyGraphVo();
        List<SceneTopologyNode> list = new ArrayList<>();
        Map<String, SceneTopologyNode> byId = new HashMap<>();
        if (nodes != null)
        {
            for (SceneTopologyNode d : nodes)
            {
                if (d == null || isBlank(d.getId()))
                {
                    continue;
                }
                SceneTopologyNode n = copyNode(d);
                list.add(n);
                byId.put(n.getId(), n);
            }
        }

        List<SceneTopologyEdge> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SceneTopologyNode d : list)
        {
            String parentId = d.getParentDeviceId();
            if (isBlank(parentId) || !byId.containsKey(parentId))
            {
                continue;
            }
            if (parentId.equals(d.getId()))
            {
                continue;
            }
            String id = edgeId(parentId, d.getId());
            if (!seen.add(id))
            {
                continue;
            }
            SceneTopologyEdge e = new SceneTopologyEdge();
            e.setId(id);
            e.setFromDeviceId(parentId);
            e.setToDeviceId(d.getId());
            edges.add(e);
        }

        vo.setNodes(list);
        vo.setEdges(edges);
        return vo;
    }

    public static SceneTopologyGraphVo focusSubgraph(SceneTopologyGraphVo graph, String focusDeviceId)
    {
        SceneTopologyGraphVo empty = new SceneTopologyGraphVo();
        if (graph == null || isBlank(focusDeviceId))
        {
            return empty;
        }
        List<SceneTopologyNode> nodes = graph.getNodes() != null ? graph.getNodes() : new ArrayList<>();
        List<SceneTopologyEdge> edges = graph.getEdges() != null ? graph.getEdges() : new ArrayList<>();
        Map<String, SceneTopologyNode> nodeById = new HashMap<>();
        for (SceneTopologyNode n : nodes)
        {
            if (n != null && !isBlank(n.getId()))
            {
                nodeById.put(n.getId(), n);
            }
        }
        if (!nodeById.containsKey(focusDeviceId))
        {
            return empty;
        }

        Set<String> keep = new HashSet<>();
        keep.add(focusDeviceId);

        Map<String, String> parents = parentMap(nodes);
        String cur = parents.get(focusDeviceId);
        Set<String> guard = new HashSet<>();
        while (!isBlank(cur) && nodeById.containsKey(cur) && guard.add(cur))
        {
            keep.add(cur);
            cur = parents.get(cur);
        }

        Map<String, List<String>> kids = childrenMap(edges);
        List<String> stack = new ArrayList<>();
        stack.add(focusDeviceId);
        while (!stack.isEmpty())
        {
            String id = stack.remove(stack.size() - 1);
            List<String> childIds = kids.get(id);
            if (childIds == null)
            {
                continue;
            }
            for (String cid : childIds)
            {
                if (keep.add(cid))
                {
                    stack.add(cid);
                }
            }
        }

        List<SceneTopologyNode> outNodes = new ArrayList<>();
        for (SceneTopologyNode n : nodes)
        {
            if (n != null && keep.contains(n.getId()))
            {
                outNodes.add(n);
            }
        }
        List<SceneTopologyEdge> outEdges = new ArrayList<>();
        for (SceneTopologyEdge e : edges)
        {
            if (e != null && keep.contains(e.getFromDeviceId()) && keep.contains(e.getToDeviceId()))
            {
                outEdges.add(e);
            }
        }
        SceneTopologyGraphVo out = new SceneTopologyGraphVo();
        out.setNodes(outNodes);
        out.setEdges(outEdges);
        return out;
    }

    /**
     * Resolve edge-{from}-{to} by matching known device id pairs (ids may contain '-').
     *
     * @return {from, to} or null
     */
    public static String[] parseEdgeKey(String edgeId, Collection<String> deviceIds)
    {
        if (isBlank(edgeId) || deviceIds == null)
        {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (String id : deviceIds)
        {
            if (!isBlank(id))
            {
                ids.add(id);
            }
        }
        for (int i = 0; i < ids.size(); i++)
        {
            for (int j = 0; j < ids.size(); j++)
            {
                if (i == j)
                {
                    continue;
                }
                String from = ids.get(i);
                String to = ids.get(j);
                if (edgeId(from, to).equals(edgeId))
                {
                    return new String[] { from, to };
                }
            }
        }
        return null;
    }

    private static SceneTopologyNode copyNode(SceneTopologyNode d)
    {
        SceneTopologyNode n = new SceneTopologyNode();
        n.setId(d.getId());
        n.setName(!isBlank(d.getName()) ? d.getName() : d.getId());
        n.setIp(d.getIp() != null ? d.getIp() : "");
        n.setType(!isBlank(d.getType()) ? d.getType() : "other");
        n.setBuildingId(d.getBuildingId());
        n.setBuildingName(d.getBuildingName());
        n.setParentDeviceId(d.getParentDeviceId());
        return n;
    }

    private static Map<String, List<String>> childrenMap(List<SceneTopologyEdge> edges)
    {
        Map<String, List<String>> map = new HashMap<>();
        for (SceneTopologyEdge e : edges)
        {
            if (e == null || isBlank(e.getFromDeviceId()) || isBlank(e.getToDeviceId()))
            {
                continue;
            }
            map.computeIfAbsent(e.getFromDeviceId(), k -> new ArrayList<>()).add(e.getToDeviceId());
        }
        return map;
    }

    private static Map<String, String> parentMap(List<SceneTopologyNode> nodes)
    {
        Map<String, String> map = new HashMap<>();
        for (SceneTopologyNode n : nodes)
        {
            if (n == null || isBlank(n.getId()))
            {
                continue;
            }
            map.put(n.getId(), n.getParentDeviceId());
        }
        return map;
    }

    private static boolean isBlank(String s)
    {
        return s == null || s.trim().isEmpty();
    }
}
