package com.netflix.exhibitor.state;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

class Checker implements Closeable
{
    private final InstanceStateManager      manager;
    private final ExecutorService           service = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
    private final AtomicReference<InstanceStateTypes> state = new AtomicReference<InstanceStateTypes>(InstanceStateTypes.LATENT);

    Checker(InstanceStateManager manager)
    {
        this.manager = manager;
    }

    public void     start()
    {
        service.submit
        (
            new Runnable()
            {
                @Override
                public void run()
                {
                    doChecking();
                }
            }
        );
    }

    @Override
    public void close() throws IOException
    {
        service.shutdownNow();
    }
    
    public InstanceStateTypes   getState()
    {
        return state.get();
    }

    private void doChecking()
    {
        try
        {
            while ( !Thread.currentThread().isInterrupted() )
            {
                Thread.sleep(manager.getConfig().getCheckSeconds() * 1000);

                setState();
            }
        }
        catch ( InterruptedException dummy )
        {
            Thread.currentThread().interrupt();
        }
    }

    private void setState()
    {
        InstanceStateTypes      currentState = state.get();
        InstanceStateTypes      newState = InstanceStateTypes.UNKNOWN;

        String      ruok = new FourLetterWord(FourLetterWord.Word.RUOK, manager.getConfig()).getResponse();
        if ( "imok".equals(ruok) )
        {
            // The following code depends on inside knowledge of the "stat" response. If they change it
            // this code might break

            List<String> lines = new FourLetterWord(FourLetterWord.Word.STAT, manager.getConfig()).getResponseLines();
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
            manager.incrementVersion();
        }
    }
}
