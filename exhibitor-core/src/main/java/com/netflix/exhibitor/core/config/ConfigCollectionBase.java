package com.netflix.exhibitor.core.config;

abstract class ConfigCollectionBase implements ConfigCollection, RollingConfigState
{
    @Override
    public final InstanceConfig getConfigForThisInstance(String hostname)
    {
        return (isRolling() && getRollingHostNames().contains(hostname)) ? getRollingConfig() : getRootConfig();
    }

    @Override
    public final boolean isRolling()
    {
        return (getRollingHostNames().size() > 0);
    }

    @Override
    public final String getRollingStatus()
    {
        if ( !isRolling() )
        {
            return "n/a";
        }
        String          currentRollingHostname = getCurrentRollingHostname();
        return "Applying to \"" + currentRollingHostname + "\"";
    }

    @Override
    public final int getRollingPercentDone()
    {
        if ( isRolling() )
        {
            return (100 * getRollingHostNamesIndex()) / getRollingHostNames().size();
        }
        return 0;
    }

    @Override
    public final RollingConfigState getRollingConfigState()
    {
        return this;
    }

    private String getCurrentRollingHostname()
    {
        return getRollingHostNames().get(getRollingHostNamesIndex());
    }
}
