package com.ruoyi.system.service.impl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SceneMonitorSettings;
import com.ruoyi.system.mapper.SceneMonitorSettingsMapper;
import com.ruoyi.system.service.ISceneMonitorSettingsService;

/**
 * Per-user scene monitor settings service implementation.
 */
@Service
public class SceneMonitorSettingsServiceImpl implements ISceneMonitorSettingsService
{
    private static final List<String> COLOR_KEYS = Arrays.asList("default", "green", "yellow", "red");
    private static final List<String> BEEP_PRESETS = Arrays.asList("soft", "default", "sharp");
    private static final List<String> SOUND_MODES = Arrays.asList("preset", "custom");

    @Autowired
    private SceneMonitorSettingsMapper settingsMapper;

    /** Local instance: Spring Boot 4 / this project does not expose an ObjectMapper bean. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public SceneMonitorSettings getSettings(Long userId)
    {
        SceneMonitorSettings settings = settingsMapper.selectByUserId(userId);
        if (settings == null)
        {
            return newDefaultSettings();
        }
        parseColorsJson(settings);
        return settings;
    }

    @Override
    public SceneMonitorSettings saveSettings(Long userId, SceneMonitorSettings settings)
    {
        if (settings == null)
        {
            throw new ServiceException("settings cannot be null");
        }
        settings.setUserId(userId);
        normalize(settings);
        validate(settings);
        serializeColorsJson(settings);
        if (settingsMapper.selectByUserId(userId) != null)
        {
            settingsMapper.update(settings);
        }
        else
        {
            settingsMapper.insert(settings);
        }
        return settings;
    }

    @Override
    public SceneMonitorSettings resetSettings(Long userId)
    {
        settingsMapper.deleteByUserId(userId);
        return newDefaultSettings();
    }

    private SceneMonitorSettings newDefaultSettings()
    {
        SceneMonitorSettings s = new SceneMonitorSettings();
        s.setAlertMuted(false);
        s.setAlertPopupEnabled(true);
        s.setAlertVolume(0.7);
        s.setBeepPreset("default");
        s.setAlertSoundMode("preset");
        s.setProbeIntervalMs(5000);
        s.setHoverSummaryDelayMs(500);
        s.setBuildingStatusColors(newDefaultColors());
        return s;
    }

    private Map<String, Object> newDefaultColors()
    {
        Map<String, Object> colors = new LinkedHashMap<>();
        colors.put("default", rgba(0.0, 1.0, 1.0, 0.45));
        colors.put("green", rgba(0.2, 0.85, 0.3, 0.55));
        colors.put("yellow", rgba(0.95, 0.85, 0.2, 0.55));
        colors.put("red", rgba(0.95, 0.25, 0.2, 0.55));
        return colors;
    }

    private static Map<String, Double> rgba(double r, double g, double b, double a)
    {
        Map<String, Double> color = new LinkedHashMap<>();
        color.put("r", r);
        color.put("g", g);
        color.put("b", b);
        color.put("a", a);
        return color;
    }

    private void normalize(SceneMonitorSettings s)
    {
        if (s.getAlertMutedFlag() == null)
        {
            s.setAlertMuted(false);
        }
        if (s.getAlertPopupEnabledFlag() == null)
        {
            s.setAlertPopupEnabled(true);
        }
        if (s.getAlertVolume() == null)
        {
            s.setAlertVolume(0.7);
        }
        if (s.getBeepPreset() == null)
        {
            s.setBeepPreset("default");
        }
        if (s.getAlertSoundMode() == null)
        {
            s.setAlertSoundMode("preset");
        }
        if (s.getProbeIntervalMs() == null)
        {
            s.setProbeIntervalMs(5000);
        }
        if (s.getHoverSummaryDelayMs() == null)
        {
            s.setHoverSummaryDelayMs(500);
        }
        if (s.getBuildingStatusColors() == null)
        {
            s.setBuildingStatusColors(newDefaultColors());
        }
    }

    private void validate(SceneMonitorSettings s)
    {
        Double volume = s.getAlertVolume();
        if (volume == null || volume < 0.0 || volume > 1.0)
        {
            throw new ServiceException("alertVolume must be between 0 and 1");
        }
        Integer probeIntervalMs = s.getProbeIntervalMs();
        if (probeIntervalMs == null || probeIntervalMs < 1000)
        {
            throw new ServiceException("probeIntervalMs must be an integer >= 1000");
        }
        Integer hoverSummaryDelayMs = s.getHoverSummaryDelayMs();
        if (hoverSummaryDelayMs == null || hoverSummaryDelayMs < 100)
        {
            throw new ServiceException("hoverSummaryDelayMs must be an integer >= 100");
        }
        if (s.getBeepPreset() == null || !BEEP_PRESETS.contains(s.getBeepPreset()))
        {
            throw new ServiceException("beepPreset must be soft, default, or sharp");
        }
        if (s.getAlertSoundMode() == null || !SOUND_MODES.contains(s.getAlertSoundMode()))
        {
            throw new ServiceException("alertSoundMode must be preset or custom");
        }
        Map<String, Object> colors = s.getBuildingStatusColors();
        if (colors == null)
        {
            throw new ServiceException("buildingStatusColors required");
        }
        for (String key : COLOR_KEYS)
        {
            Object color = colors.get(key);
            if (!(color instanceof Map))
            {
                throw new ServiceException("buildingStatusColors." + key + " must be an object with r,g,b,a in [0,1]");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> rgba = (Map<String, Object>) color;
            for (String channel : Arrays.asList("r", "g", "b", "a"))
            {
                Object value = rgba.get(channel);
                if (!(value instanceof Number))
                {
                    throw new ServiceException("buildingStatusColors." + key + "." + channel + " must be a number in [0,1]");
                }
                double d = ((Number) value).doubleValue();
                if (d < 0.0 || d > 1.0)
                {
                    throw new ServiceException("buildingStatusColors." + key + "." + channel + " must be in [0,1]");
                }
            }
        }
    }

    private void serializeColorsJson(SceneMonitorSettings s)
    {
        try
        {
            s.setBuildingStatusColorsJson(objectMapper.writeValueAsString(s.getBuildingStatusColors()));
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("buildingStatusColors is invalid JSON");
        }
    }

    private void parseColorsJson(SceneMonitorSettings s)
    {
        String json = s.getBuildingStatusColorsJson();
        if (json == null || json.isEmpty())
        {
            s.setBuildingStatusColors(null);
            return;
        }
        try
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> colors = objectMapper.readValue(json, Map.class);
            s.setBuildingStatusColors(colors);
        }
        catch (JsonProcessingException e)
        {
            s.setBuildingStatusColors(null);
        }
    }
}
