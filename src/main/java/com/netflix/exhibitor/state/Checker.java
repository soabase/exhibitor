package com.netflix.exhibitor.state;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.activity.RepeatingActivity;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Checker implements Closeable
{
    private final Exhibitor                 exhibitor;
    private final AtomicReference<InstanceStateTypes> state = new AtomicReference<InstanceStateTypes>(InstanceStateTypes.LATENT);
    private final RepeatingActivity         repeatingActivity;

    public Checker(Exhibitor exhibitor, InstanceStateManager manager)
    {
        this.exhibitor = exhibitor;
        Activity activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
                // NOP
            }

            @Override
            public Boolean call() throws Exception
            {
                setState();
                return true;
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.MAIN, activity, TimeUnit.MILLISECONDS.convert(exhibitor.getConfig().getCheckSeconds(), TimeUnit.SECONDS));
    }

    public void     start()
    {
        repeatingActivity.start();
    }

    @Override
    public void close() throws IOException
    {
        repeatingActivity.close();
    }

    public InstanceStateTypes   getState()
    {
        return state.get();
    }

    private void setState()
    {
        InstanceStateTypes      currentState = state.get();
        InstanceStateTypes      newState = InstanceStateTypes.UNKNOWN;

        String      ruok = new FourLetterWord(FourLetterWord.Word.RUOK, exhibitor.getConfig()).getResponse();
        if ( "imok".equals(ruok) )
        {
            // The following code depends on inside knowledge of the "stat" response. If they change it
            // this code might break

            List<String> lines = new FourLetterWord(FourLetterWord.Word.STAT, exhibitor.getConfig()).getResponseLines();
            for ( String line : lines )
            {
                if ( line.contains("not currently serving") )
                {
                    newState = InstanceStateTypes.NOT_SERVING;
                    break;
                }

                if ( line.toLowerCase().startsWith("mode") )
                {
                    newState = InstanceStateTypes.SERVING;
                    break;
                }
            }
        }
        
        if ( newState != currentState )
        {
            state.set(newState);
        }
    }
}
