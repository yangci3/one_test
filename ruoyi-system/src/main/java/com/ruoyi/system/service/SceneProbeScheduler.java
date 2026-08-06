package com.ruoyi.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled mock probe status flips.
 */
@Component
public class SceneProbeScheduler
{
    @Autowired
    private ISceneProbeService sceneProbeService;

    @Scheduled(fixedDelayString = "${scene.probe.interval-ms:5000}")
    public void run()
    {
        sceneProbeService.tick();
    }
}
