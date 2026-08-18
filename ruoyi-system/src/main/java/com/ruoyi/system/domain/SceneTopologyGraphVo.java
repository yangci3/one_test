package com.ruoyi.system.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Topology graph payload: nodes and edges (never null lists).
 */
public class SceneTopologyGraphVo
{
    private List<SceneTopologyNode> nodes = new ArrayList<>();

    private List<SceneTopologyEdge> edges = new ArrayList<>();

    public List<SceneTopologyNode> getNodes()
    {
        return nodes;
    }

    public void setNodes(List<SceneTopologyNode> nodes)
    {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
    }

    public List<SceneTopologyEdge> getEdges()
    {
        return edges;
    }

    public void setEdges(List<SceneTopologyEdge> edges)
    {
        this.edges = edges != null ? edges : new ArrayList<>();
    }
}
