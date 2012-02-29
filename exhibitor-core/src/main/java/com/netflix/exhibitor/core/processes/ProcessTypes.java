package com.netflix.exhibitor.core.processes;

public enum ProcessTypes
{
    CLEANUP()
    {
        @Override
        public String getDescription()
        {
            return "Cleanup Task";
        }
    },

    ZOOKEEPER()
    {
        @Override
        public String getDescription()
        {
            return "ZooKeeper Server";
        }
    }
    ;

    public abstract String      getDescription();
}
