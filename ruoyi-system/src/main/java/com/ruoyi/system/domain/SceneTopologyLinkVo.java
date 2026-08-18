package com.ruoyi.system.domain;

/**
 * Link detail payload: edge id and endpoint nodes.
 */
public class SceneTopologyLinkVo
{
    private String edgeId;

    private SceneTopologyNode from;

    private SceneTopologyNode to;

    public String getEdgeId()
    {
        return edgeId;
    }

    public void setEdgeId(String edgeId)
    {
        this.edgeId = edgeId;
    }

    public SceneTopologyNode getFrom()
    {
        return from;
    }

    public void setFrom(SceneTopologyNode from)
    {
        this.from = from;
    }

    public SceneTopologyNode getTo()
    {
        return to;
    }

    public void setTo(SceneTopologyNode to)
    {
        this.to = to;
    }
}
