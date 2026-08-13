package com.ruoyi.system.service.probe;

import com.ruoyi.system.domain.SceneDevice;
import com.ruoyi.system.domain.SceneProbeState;

/**
 * Reachability strategy for scene probe tick().
 */
public interface SceneProbeReachability
{
    /**
     * Reachability decision returned by a probe strategy.
     */
    enum Result
    {
        /**
         * Skip this device for the current tick; do not change status.
         */
        SKIP,
        /**
         * Device is considered reachable / online.
         */
        UP,
        /**
         * Device is considered unreachable / offline.
         */
        DOWN
    }

    /**
     * Probe the reachability of the given device based on its current state.
     *
     * @param state the current probe state (may be null in tests)
     * @param device the device with IP and metadata
     * @return SKIP, UP or DOWN
     */
    Result probe(SceneProbeState state, SceneDevice device);
}
