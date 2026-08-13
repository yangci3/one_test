package com.ruoyi.system.service.probe;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Windows ICMP reachability probe using the ping command.
 */
public class IcmpProbeReachability
{
    private static final Logger log = LoggerFactory.getLogger(IcmpProbeReachability.class);

    /**
     * Probe the given IPv4 address with one ICMP ping.
     *
     * @param ip the IPv4 address to ping
     * @param timeoutMs the timeout in milliseconds passed to Windows ping -w
     * @return true if the ping command exits with code 0, false otherwise
     */
    public boolean probe(String ip, int timeoutMs)
    {
        if (!ProbeIpUtils.isValidIpv4(ip))
        {
            return false;
        }
        if (timeoutMs <= 0)
        {
            return false;
        }
        try
        {
            ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", "-w", String.valueOf(timeoutMs), ip);
            pb.redirectOutput(new File("NUL"));
            pb.redirectError(new File("NUL"));
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutMs + 1000L, TimeUnit.MILLISECONDS);
            if (!finished)
            {
                process.destroy();
                return false;
            }
            return process.exitValue() == 0;
        }
        catch (IOException | InterruptedException e)
        {
            log.warn("ICMP probe failed for {}: {}", ip, e.getMessage());
            return false;
        }
        catch (Throwable t)
        {
            log.warn("ICMP probe unexpected failure for {}: {}", ip, t.getMessage());
            return false;
        }
    }
}
