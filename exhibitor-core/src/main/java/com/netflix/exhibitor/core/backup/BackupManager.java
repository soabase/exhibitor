package com.netflix.exhibitor.core.backup;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;
import com.netflix.exhibitor.core.index.ZooKeeperLogFiles;
import com.netflix.exhibitor.core.state.ControlPanelTypes;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages backups/restores
 */
public class BackupManager implements Closeable
{
    private final Exhibitor exhibitor;
    private final Optional<BackupProvider> backupProvider;
    private final RepeatingActivity repeatingActivity;
    private final AtomicBoolean tempDisabled = new AtomicBoolean(false);

    private static final String         KEY_PREFIX = "exhibitor";
    private static final String         SEPARATOR = "-";
    private static final String         SEPARATOR_REPLACEMENT = "_";

    private static final String         FORMAT_SPEC = "MMM d yyyy HH:mm:ss";

    /**
     * @param exhibitor main instance
     * @param backupProvider provider
     */
    public BackupManager(Exhibitor exhibitor, BackupProvider backupProvider)
    {
        this.exhibitor = exhibitor;
        this.backupProvider = Optional.fromNullable(backupProvider);

        Activity activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
            }

            @Override
            public Boolean call() throws Exception
            {
                if ( !tempDisabled.get() )
                {
                    doBackup();
                }
                return true;
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfig().getInt(IntConfigs.BACKUP_PERIOD_MS));
    }

    /**
     * Manager must be started
     */
    public void start()
    {
        if ( isActive() )
        {
            repeatingActivity.start();
            exhibitor.addConfigListener
                (
                    new ConfigListener()
                    {
                        @Override
                        public void configUpdated()
                        {
                            repeatingActivity.setTimePeriodMs(exhibitor.getConfig().getInt(IntConfigs.BACKUP_PERIOD_MS));
                        }
                    }
                );
        }
    }

    @Override
    public void close() throws IOException
    {
        if ( isActive() )
        {
            repeatingActivity.close();
        }
    }

    /**
     * Returns true if backup has been configured. Running Exhibitor without backups is supported
     *
     * @return true/false
     */
    public boolean isActive()
    {
        return backupProvider.isPresent();
    }

    /**
     * Used to temporarily disable backups
     *
     * @param value on/off
     */
    public void     setTempDisabled(boolean value)
    {
        tempDisabled.set(value);
    }

    /**
     * Return displayable set of backups
     *
     * @return display names
     * @throws Exception errors
     */
    public Collection<String> getAvailableSessionNames() throws Exception
    {
        Collection<SessionAndName> sessionAndNames = getAvailableSession();
        return Collections2.transform
        (
            sessionAndNames,
            new Function<SessionAndName, String>()
            {
                @Override
                public String apply(SessionAndName sessionAndName)
                {
                    return sessionAndName.name;
                }
            }
        );
    }

    /**
     * Get detailed info on available restores
     *
     * @return details
     * @throws Exception errors
     */
    public Collection<SessionAndName> getAvailableSession() throws Exception
    {
        final SimpleDateFormat    formatter = new SimpleDateFormat(FORMAT_SPEC);
        Map<String, String>       config = getBackupConfig();
        List<String>              backupKeys = backupProvider.get().getAvailableBackupKeys(exhibitor, config);
        TreeSet<Long>             sessions = getSessions(backupKeys);
        return Collections2.transform
        (
            sessions,
            new Function<Long, SessionAndName>()
            {
                @Override
                public SessionAndName apply(Long nanos)
                {
                    String name = formatter.format(new Date(TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS)));
                    return new SessionAndName(name, nanos);
                }
            }
        );
    }

    /**
     * Return the stored backup config
     *
     * @return backup config
     */
    public BackupConfigParser  getBackupConfigParser()
    {
        return new BackupConfigParser(exhibitor.getConfig().getString(StringConfigs.BACKUP_EXTRA), backupProvider.get());
    }

    /**
     * Return the names of backup config, display info, etc.
     *
     * @return specs
     */
    public List<BackupConfigSpec> getConfigSpecs()
    {
        return backupProvider.get().getConfigs();
    }

    /**
     * Given a session ID, return all the backup keys
     *
     * @param session the session
     * @return keys
     * @throws Exception errors
     */
    public Collection<String> findKeyForSession(long session) throws Exception
    {
        List<String> backupKeys = backupProvider.get().getAvailableBackupKeys(exhibitor, getBackupConfig());
        return Collections2.filter(backupKeys, asKey(session));
    }

    /**
     * Restore the given key to the given file
     *
     * @param key the key
     * @param destinationFile the file
     * @throws Exception errors
     */
    public void restoreKey(String key, File destinationFile) throws Exception
    {
        backupProvider.get().downloadBackup(exhibitor, key, destinationFile, getBackupConfig());
    }

    private void doBackup() throws Exception
    {
        if ( !exhibitor.isControlPanelSettingEnabled(ControlPanelTypes.BACKUPS) )
        {
            return;
        }

        ZooKeeperLogFiles zooKeeperLogFiles = new ZooKeeperLogFiles(exhibitor);
        if ( !zooKeeperLogFiles.isValid() )
        {
            return;
        }

        Map<String, String> config = getBackupConfig();

        BackupProvider      provider = backupProvider.get();
        if ( !provider.isValidConfig(exhibitor, config) )
        {
            return;
        }

        long                backupSessionId = System.nanoTime();
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Backup starting");
        try
        {
            int         index = 0;
            for ( File f : zooKeeperLogFiles.getPaths() )
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Backing up: " + f);
                provider.uploadBackup(exhibitor, makeKey(backupSessionId, index++), f, config);
            }
        }
        finally
        {
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Backup complete");
        }

        doRoll(config);
    }

    private Map<String, String> getBackupConfig()
    {
        String              backupExtra = exhibitor.getConfig().getString(StringConfigs.BACKUP_EXTRA);
        BackupConfigParser  backupConfigParser = new BackupConfigParser(backupExtra, backupProvider.get());
        return backupConfigParser.getValues();
    }

    private void doRoll(Map<String, String> config) throws Exception
    {
        List<String>    backupKeys = backupProvider.get().getAvailableBackupKeys(exhibitor, config);
        TreeSet<Long>   sessions = getSessions(backupKeys);

        while ( sessions.size() > exhibitor.getConfig().getInt(IntConfigs.BACKUP_MAX_FILES) )
        {
            final long          session = sessions.pollFirst();
            Collection<String>  keys = Collections2.filter(backupKeys, asKey(session));
            for ( String key : keys )
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Deleting old backup: " + key);
                backupProvider.get().deleteBackup(exhibitor, key, config);
            }
        }
    }

    private Predicate<String> asKey(final long session)
    {
        return new Predicate<String>()
        {
            @Override
            public boolean apply(String key)
            {
                return (sessionFromKey(key) == session);
            }
        };
    }

    private TreeSet<Long> getSessions(List<String> backupKeys)
    {
        TreeSet<Long>   sessions = Sets.newTreeSet();
        for ( String key : backupKeys )
        {
            long session = sessionFromKey(key);
            if ( session != 0 )
            {
                sessions.add(session);
            }
        }
        return sessions;
    }

    private static long sessionFromKey(String key)
    {
        long            session = 0;
        String[]        parts = key.split(SEPARATOR);
        if ( parts.length > 2 )
        {
            try
            {
                session = Long.parseLong(parts[2]);
            }
            catch ( NumberFormatException e )
            {
                // ignore
            }
        }
        return session;
    }

    private String makeKey(long backupSessionId, int index)
    {
        String hostname = exhibitor.getConfig().getString(StringConfigs.HOSTNAME);
        hostname = hostname.replace(SEPARATOR, SEPARATOR_REPLACEMENT);
        return KEY_PREFIX + SEPARATOR + hostname + SEPARATOR + backupSessionId + SEPARATOR + index;
    }
}
