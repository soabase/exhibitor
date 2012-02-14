package com.netflix.exhibitor.core.state;

import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.StringConfigs;
import java.util.List;

public class Checker
{
    private final Exhibitor exhibitor;

    public Checker(Exhibitor exhibitor, InstanceStateManager manager)
    {
        this.exhibitor = exhibitor;
    }

    public InstanceStateTypes   getState()
    {
        InstanceStateTypes      state = InstanceStateTypes.UNKNOWN;
        ServerList              serverList = new ServerList(exhibitor.getConfig().getString(StringConfigs.SERVERS_SPEC));
        ServerList.ServerSpec   us = Iterables.find(serverList.getSpecs(), ServerList.isUs(exhibitor.getConfig().getString(StringConfigs.HOSTNAME)), null);
        if ( us != null )
        {
            if ( !exhibitor.isControlPanelSettingEnabled(ControlPanelTypes.RESTARTS) )
            {
                state = InstanceStateTypes.DOWN_BECAUSE_RESTARTS_TURNED_OFF;
            }
        }
        else
        {
            if ( !exhibitor.isControlPanelSettingEnabled(ControlPanelTypes.UNLISTED_RESTARTS) )
            {
                state = InstanceStateTypes.DOWN_BECAUSE_UNLISTED;
            }
        }

        String      ruok = new FourLetterWord(FourLetterWord.Word.RUOK, exhibitor.getConfig()).getResponse();
        if ( "imok".equals(ruok) )
        {
            // The following code depends on inside knowledge of the "srvr" response. If they change it
            // this code might break

            List<String> lines = new FourLetterWord(FourLetterWord.Word.SRVR, exhibitor.getConfig()).getResponseLines();
            for ( String line : lines )
            {
                if ( line.contains("not currently serving") )
                {
                    state = InstanceStateTypes.NOT_SERVING;
                    break;
                }

                if ( line.toLowerCase().startsWith("mode") )
                {
                    state = InstanceStateTypes.SERVING;
                    break;
                }
            }
        }

        return state;
    }
}
