package com.ruoyi.system.service.probe;

import java.util.concurrent.ThreadLocalRandom;
import com.ruoyi.system.domain.SceneDevice;
import com.ruoyi.system.domain.SceneProbeState;

/**
 * Mock reachability strategy that flips status using configured probabilities.
 */
public class MockProbeReachability implements SceneProbeReachability
{
    private static final String STATUS_ONLINE = "online";
    private static final String STATUS_OFFLINE = "offline";

    private final double offlineProb;
    private final double recoverProb;

    public MockProbeReachability(double offlineProb, double recoverProb)
    {
        this.offlineProb = offlineProb;
        this.recoverProb = recoverProb;
    }

    @Override
    public Result probe(SceneProbeState state, SceneDevice device)
    {
        if (state == null)
        {
            return Result.SKIP;
        }
        String status = state.getStatus();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (STATUS_ONLINE.equals(status) && random.nextDouble() < offlineProb)
        {
            return Result.DOWN;
        }
        if (STATUS_OFFLINE.equals(status) && random.nextDouble() < recoverProb)
        {
            return Result.UP;
        }
        return Result.SKIP;
    }
}
