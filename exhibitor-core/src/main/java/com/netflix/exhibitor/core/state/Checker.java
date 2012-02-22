package com.netflix.exhibitor.core.state;

import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.StringConfigs;
import com.netflix.exhibitor.core.controlpanel.ControlPanelTypes;
import java.util.List;

/**
 * Determines the state of the ZK server
 */
public class Checker
{
    private final Exhibitor exhibitor;

    public Checker(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
    }

    public InstanceStateTypes   getState() throws Exception
    {
        InstanceConfig          config = exhibitor.getConfigManager().getConfig();

        InstanceStateTypes      potentialState = InstanceStateTypes.UNKNOWN;
        ServerList              serverList = new ServerList(config.getString(StringConfigs.SERVERS_SPEC));
        ServerList.ServerSpec   us = Iterables.find(serverList.getSpecs(), ServerList.isUs(exhibitor.getThisJVMHostname()), null);
        if ( us != null )
        {
            if ( !exhibitor.getControlPanelValues().isSet(ControlPanelTypes.RESTARTS) )
            {
                potentialState = InstanceStateTypes.DOWN_BECAUSE_RESTARTS_TURNED_OFF;
            }
        }
        else
        {
            if ( !exhibitor.getControlPanelValues().isSet(ControlPanelTypes.UNLISTED_RESTARTS) )
            {
                potentialState = InstanceStateTypes.DOWN_BECAUSE_UNLISTED;
            }
        }

        InstanceStateTypes      actualState = potentialState;
        String                  ruok = new FourLetterWord(FourLetterWord.Word.RUOK, config, exhibitor.getConnectionTimeOutMs()).getResponse();
        if ( "imok".equals(ruok) )
        {
            // The following code depends on inside knowledge of the "srvr" response. If they change it
            // this code might break

            List<String> lines = new FourLetterWord(FourLetterWord.Word.SRVR, config, exhibitor.getConnectionTimeOutMs()).getResponseLines();
            for ( String line : lines )
            {
                if ( line.contains("not currently serving") )
                {
                    actualState = InstanceStateTypes.NOT_SERVING;
                    break;
                }

                if ( line.toLowerCase().startsWith("mode") )
                {
                    actualState = InstanceStateTypes.SERVING;
                    break;
                }
            }
        }

        return actualState;
    }
}
