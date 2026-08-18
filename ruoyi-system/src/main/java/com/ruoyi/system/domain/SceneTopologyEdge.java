package com.ruoyi.system.domain;

/**
 * Topology graph edge (parent -> child).
 */
public class SceneTopologyEdge
{
    private String id;

    private String fromDeviceId;

    private String toDeviceId;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getFromDeviceId()
    {
        return fromDeviceId;
    }

    public void setFromDeviceId(String fromDeviceId)
    {
        this.fromDeviceId = fromDeviceId;
    }

    public String getToDeviceId()
    {
        return toDeviceId;
    }

    public void setToDeviceId(String toDeviceId)
    {
        this.toDeviceId = toDeviceId;
    }
}
