package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Scene probe state (scene_probe_state).
 * JSON: deviceId, monitoring (boolean), status, lastChangeAt.
 */
@JsonIgnoreProperties({"searchValue", "createBy", "createTime", "updateBy", "updateTime", "remark", "params"})
public class SceneProbeState extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String deviceId;

    /** DB storage: {@code 0} or {@code 1}. */
    private String monitoringFlag;

    private String status;

    private Long lastChangeAt;

    public String getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(String deviceId)
    {
        this.deviceId = deviceId;
    }

    @JsonProperty("monitoring")
    public boolean isMonitoring()
    {
        return "1".equals(monitoringFlag);
    }

    @JsonProperty("monitoring")
    public void setMonitoring(boolean monitoring)
    {
        this.monitoringFlag = monitoring ? "1" : "0";
    }

    @JsonIgnore
    public String getMonitoringFlag()
    {
        return monitoringFlag;
    }

    @JsonIgnore
    public void setMonitoringFlag(String monitoringFlag)
    {
        this.monitoringFlag = monitoringFlag;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @JsonProperty("lastChangeAt")
    public Long getLastChangeAt()
    {
        return lastChangeAt;
    }

    @JsonProperty("lastChangeAt")
    public void setLastChangeAt(Long lastChangeAt)
    {
        this.lastChangeAt = lastChangeAt;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("deviceId", getDeviceId())
            .append("monitoring", isMonitoring())
            .append("status", getStatus())
            .append("lastChangeAt", getLastChangeAt())
            .toString();
    }
}
