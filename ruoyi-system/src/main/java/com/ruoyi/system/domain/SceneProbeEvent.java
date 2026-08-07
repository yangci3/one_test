package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Scene probe online/offline event (scene_probe_event).
 * JSON: id, deviceId, type, at.
 */
@JsonIgnoreProperties({"searchValue", "createBy", "createTime", "updateBy", "updateTime", "remark", "params"})
public class SceneProbeEvent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String eventId;

    private String deviceId;

    private String eventType;

    private Long eventAt;

    /** Enriched from scene_device (not persisted). */
    private String deviceName;

    /** Enriched from scene_device (not persisted). */
    private String ip;

    @JsonProperty("id")
    public String getEventId()
    {
        return eventId;
    }

    @JsonProperty("id")
    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    public String getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(String deviceId)
    {
        this.deviceId = deviceId;
    }

    @JsonProperty("type")
    public String getEventType()
    {
        return eventType;
    }

    @JsonProperty("type")
    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    @JsonProperty("at")
    public Long getEventAt()
    {
        return eventAt;
    }

    @JsonProperty("at")
    public void setEventAt(Long eventAt)
    {
        this.eventAt = eventAt;
    }

    @JsonProperty("deviceName")
    public String getDeviceName()
    {
        return deviceName;
    }

    @JsonProperty("deviceName")
    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    @JsonProperty("ip")
    public String getIp()
    {
        return ip;
    }

    @JsonProperty("ip")
    public void setIp(String ip)
    {
        this.ip = ip;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("eventId", getEventId())
            .append("deviceId", getDeviceId())
            .append("eventType", getEventType())
            .append("eventAt", getEventAt())
            .append("deviceName", getDeviceName())
            .append("ip", getIp())
            .toString();
    }
}
