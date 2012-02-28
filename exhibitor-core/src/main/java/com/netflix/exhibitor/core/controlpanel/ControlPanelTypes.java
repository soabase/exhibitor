package com.netflix.exhibitor.core.controlpanel;

/**
 * The on/off switches in the Explorer UI Control Panel tab
 */
public enum ControlPanelTypes
{
    RESTARTS,
    UNLISTED_RESTARTS,
    CLEANUP,
    BACKUPS
    ;
    
    public static ControlPanelTypes fuzzyFind(String s)
    {
        for ( ControlPanelTypes type : ControlPanelTypes.values() )
        {
            String  fuzzy = type.name().replace("_", "");
            if ( fuzzy.equalsIgnoreCase(s) )
            {
                return type;
            }
        }
        return null;
    }
}
