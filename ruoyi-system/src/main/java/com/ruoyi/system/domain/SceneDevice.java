package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Scene device master data (scene_device).
 * JSON uses B1 names: id / name / type.
 */
public class SceneDevice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String deviceId;

    private String deviceName;

    private String ip;

    private String mac;

    private String deviceType;

    private String buildingId;

    private String parentDeviceId;

    private String delFlag;

    @JsonProperty("id")
    public String getDeviceId()
    {
        return deviceId;
    }

    @JsonProperty("id")
    public void setDeviceId(String deviceId)
    {
        this.deviceId = deviceId;
    }

    /** Spring MVC GET query alias (?id=); ignored by Jackson. */
    @JsonIgnore
    public void setId(String id)
    {
        this.deviceId = id;
    }

    @JsonProperty("name")
    public String getDeviceName()
    {
        return deviceName;
    }

    @JsonProperty("name")
    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    /** Spring MVC GET query alias (?name=). */
    @JsonIgnore
    public void setName(String name)
    {
        this.deviceName = name;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac(String mac)
    {
        this.mac = mac;
    }

    @JsonProperty("type")
    public String getDeviceType()
    {
        return deviceType;
    }

    @JsonProperty("type")
    public void setDeviceType(String deviceType)
    {
        this.deviceType = deviceType;
    }

    /** Spring MVC GET query alias (?type=). */
    @JsonIgnore
    public void setType(String type)
    {
        this.deviceType = type;
    }

    public String getBuildingId()
    {
        return buildingId;
    }

    public void setBuildingId(String buildingId)
    {
        this.buildingId = buildingId;
    }

    public String getParentDeviceId()
    {
        return parentDeviceId;
    }

    public void setParentDeviceId(String parentDeviceId)
    {
        this.parentDeviceId = parentDeviceId;
    }

    @JsonIgnore
    public String getDelFlag()
    {
        return delFlag;
    }

    @JsonIgnore
    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("deviceId", getDeviceId())
            .append("deviceName", getDeviceName())
            .append("ip", getIp())
            .append("mac", getMac())
            .append("deviceType", getDeviceType())
            .append("buildingId", getBuildingId())
            .append("parentDeviceId", getParentDeviceId())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
