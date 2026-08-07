package com.ruoyi.system.domain;

import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Per-user scene monitor settings (scene_monitor_settings).
 * JSON excludes DB-only flag fields and the JSON-string color storage.
 */
@JsonIgnoreProperties({"searchValue", "createBy", "createTime", "updateBy", "updateTime", "remark", "params"})
public class SceneMonitorSettings extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long userId;

    /** DB storage: {@code 0} or {@code 1}. */
    private String alertMutedFlag;

    private Double alertVolume;

    private String beepPreset;

    private Integer probeIntervalMs;

    private Integer hoverSummaryDelayMs;

    /** DB storage: {@code 0} or {@code 1}. */
    private String alertPopupEnabledFlag;

    private String alertSoundMode;

    /** JSON string stored in {@code building_status_colors}. */
    private String buildingStatusColorsJson;

    /** In-memory color map exposed to JSON as {@code buildingStatusColors}. */
    private Map<String, Object> buildingStatusColors;

    @JsonIgnore
    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    @JsonIgnore
    public String getAlertMutedFlag()
    {
        return alertMutedFlag;
    }

    public void setAlertMutedFlag(String alertMutedFlag)
    {
        this.alertMutedFlag = alertMutedFlag;
    }

    @JsonProperty("alertMuted")
    public boolean isAlertMuted()
    {
        return "1".equals(alertMutedFlag);
    }

    @JsonProperty("alertMuted")
    public void setAlertMuted(boolean alertMuted)
    {
        this.alertMutedFlag = alertMuted ? "1" : "0";
    }

    public Double getAlertVolume()
    {
        return alertVolume;
    }

    public void setAlertVolume(Double alertVolume)
    {
        this.alertVolume = alertVolume;
    }

    public String getBeepPreset()
    {
        return beepPreset;
    }

    public void setBeepPreset(String beepPreset)
    {
        this.beepPreset = beepPreset;
    }

    public Integer getProbeIntervalMs()
    {
        return probeIntervalMs;
    }

    public void setProbeIntervalMs(Integer probeIntervalMs)
    {
        this.probeIntervalMs = probeIntervalMs;
    }

    public Integer getHoverSummaryDelayMs()
    {
        return hoverSummaryDelayMs;
    }

    public void setHoverSummaryDelayMs(Integer hoverSummaryDelayMs)
    {
        this.hoverSummaryDelayMs = hoverSummaryDelayMs;
    }

    @JsonIgnore
    public String getAlertPopupEnabledFlag()
    {
        return alertPopupEnabledFlag;
    }

    public void setAlertPopupEnabledFlag(String alertPopupEnabledFlag)
    {
        this.alertPopupEnabledFlag = alertPopupEnabledFlag;
    }

    @JsonProperty("alertPopupEnabled")
    public boolean isAlertPopupEnabled()
    {
        return "1".equals(alertPopupEnabledFlag);
    }

    @JsonProperty("alertPopupEnabled")
    public void setAlertPopupEnabled(boolean alertPopupEnabled)
    {
        this.alertPopupEnabledFlag = alertPopupEnabled ? "1" : "0";
    }

    public String getAlertSoundMode()
    {
        return alertSoundMode;
    }

    public void setAlertSoundMode(String alertSoundMode)
    {
        this.alertSoundMode = alertSoundMode;
    }

    @JsonIgnore
    public String getBuildingStatusColorsJson()
    {
        return buildingStatusColorsJson;
    }

    public void setBuildingStatusColorsJson(String buildingStatusColorsJson)
    {
        this.buildingStatusColorsJson = buildingStatusColorsJson;
    }

    @JsonProperty("buildingStatusColors")
    public Map<String, Object> getBuildingStatusColors()
    {
        return buildingStatusColors;
    }

    @JsonProperty("buildingStatusColors")
    public void setBuildingStatusColors(Map<String, Object> buildingStatusColors)
    {
        this.buildingStatusColors = buildingStatusColors;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("alertMuted", isAlertMuted())
            .append("alertVolume", getAlertVolume())
            .append("beepPreset", getBeepPreset())
            .append("probeIntervalMs", getProbeIntervalMs())
            .append("hoverSummaryDelayMs", getHoverSummaryDelayMs())
            .append("alertPopupEnabled", isAlertPopupEnabled())
            .append("alertSoundMode", getAlertSoundMode())
            .append("buildingStatusColors", getBuildingStatusColors())
            .toString();
    }
}
