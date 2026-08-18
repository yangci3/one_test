package com.ruoyi.system.domain;

/**
 * Topology graph node (device projection for scene topology).
 */
public class SceneTopologyNode
{
    private String id;

    private String name;

    private String ip;

    private String type;

    private String buildingId;

    private String buildingName;

    private String parentDeviceId;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getBuildingId()
    {
        return buildingId;
    }

    public void setBuildingId(String buildingId)
    {
        this.buildingId = buildingId;
    }

    public String getBuildingName()
    {
        return buildingName;
    }

    public void setBuildingName(String buildingName)
    {
        this.buildingName = buildingName;
    }

    public String getParentDeviceId()
    {
        return parentDeviceId;
    }

    public void setParentDeviceId(String parentDeviceId)
    {
        this.parentDeviceId = parentDeviceId;
    }
}
