package com.ruoyi.system.service.probe;

/**
 * IPv4 validation utilities for scene probe.
 */
public final class ProbeIpUtils
{
    private ProbeIpUtils()
    {
    }

    /**
     * Validate a basic dotted-quad IPv4 address.
     *
     * @param ip the IP string to validate
     * @return true if the input is a non-null, non-blank, valid IPv4 dotted-quad
     */
    public static boolean isValidIpv4(String ip)
    {
        if (ip == null)
        {
            return false;
        }
        String trimmed = ip.trim();
        if (trimmed.isEmpty())
        {
            return false;
        }
        String[] parts = trimmed.split("\\.", -1);
        if (parts.length != 4)
        {
            return false;
        }
        for (String part : parts)
        {
            if (part.isEmpty())
            {
                return false;
            }
            int value;
            try
            {
                value = Integer.parseInt(part);
            }
            catch (NumberFormatException e)
            {
                return false;
            }
            if (value < 0 || value > 255)
            {
                return false;
            }
        }
        return true;
    }
}
