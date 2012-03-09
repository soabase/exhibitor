package com.netflix.exhibitor.core.backup;

import com.google.common.base.Optional;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.EncodedConfigParser;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;
import com.netflix.exhibitor.core.controlpanel.ControlPanelTypes;
import com.netflix.exhibitor.core.index.ZooKeeperLogFiles;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages backups/restores
 */
public class BackupManager implements Closeable
{
    private final Exhibitor exhibitor;
    private final Optional<BackupProvider> backupProvider;
    private final RepeatingActivity repeatingActivity;
    private final AtomicBoolean tempDisabled = new AtomicBoolean(false);
    private final AtomicLong lastRollCheck = new AtomicLong(0);

    /**
     * @param exhibitor main instance
     * @param backupProvider provider
     */
    public BackupManager(Exhibitor exhibitor, BackupProvider backupProvider)
    {
        InstanceConfig config = exhibitor.getConfigManager().getConfig();

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
        repeatingActivity = new RepeatingActivity(exhibitor.getLog(), exhibitor.getActivityQueue(), QueueGroups.IO, activity, config.getInt(IntConfigs.BACKUP_PERIOD_MS));
    }

    /**
     * Manager must be started
     */
    public void start()
    {
        if ( isActive() )
        {
            repeatingActivity.start();
            exhibitor.getConfigManager().addConfigListener
            (
                new ConfigListener()
                {
                    @Override
                    public void configUpdated()
                    {
                        repeatingActivity.setTimePeriodMs(exhibitor.getConfigManager().getConfig().getInt(IntConfigs.BACKUP_PERIOD_MS));
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
        tempDisabled.set(value);    // TODO - this is too hack-y
    }

    /**
     * Return list of available backups
     *
     * @return backups
     * @throws Exception errors
     */
    public List<BackupMetaData> getAvailableBackups() throws Exception
    {
        Map<String, String>       config = getBackupConfig();
        return backupProvider.get().getAvailableBackups(exhibitor, config);
    }

    /**
     * Return the stored backup config
     *
     * @return backup config
     */
    public EncodedConfigParser getBackupConfigParser()
    {
        return new EncodedConfigParser(exhibitor.getConfigManager().getConfig().getString(StringConfigs.BACKUP_EXTRA));
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
     * Restore the given key to the given file
     *
     * @param backup the backup to pull down
     * @param destinationFile the file
     * @throws Exception errors
     */
    public void restore(BackupMetaData backup, File destinationFile) throws Exception
    {
        backupProvider.get().downloadBackup(exhibitor, backup, destinationFile, getBackupConfig());
    }

    private void doBackup() throws Exception
    {
        if ( !exhibitor.getControlPanelValues().isSet(ControlPanelTypes.BACKUPS) )
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

        for ( File f : zooKeeperLogFiles.getPaths() )
        {
            BackupMetaData metaData = new BackupMetaData(f.getName(), f.lastModified());
            BackupProvider.UploadResult result = provider.uploadBackup(exhibitor, metaData, f, config);
            switch ( result )
            {
                case SUCCEEDED:
                {
                    exhibitor.getLog().add(ActivityLog.Type.INFO, "Backing up: " + f);
                    break;
                }

                case DUPLICATE:
                {
                    // ignore
                    break;
                }

                case REPLACED_OLD_VERSION:
                {
                    exhibitor.getLog().add(ActivityLog.Type.INFO, "Updated back up for: " + f);
                    break;
                }
            }
        }

        doRoll(config);
    }

    private Map<String, String> getBackupConfig()
    {
        String              backupExtra = exhibitor.getConfigManager().getConfig().getString(StringConfigs.BACKUP_EXTRA);
        EncodedConfigParser encodedConfigParser = new EncodedConfigParser(backupExtra);
        return encodedConfigParser.getValues();
    }

    private void doRoll(Map<String, String> config) throws Exception
    {
        long        elapsed = System.currentTimeMillis() - lastRollCheck.get();
        if ( elapsed < (exhibitor.getConfigManager().getConfig().getInt(IntConfigs.BACKUP_MAX_STORE_MS) / 3) )
        {
            return;
        }

        List<BackupMetaData>        availableBackups = backupProvider.get().getAvailableBackups(exhibitor, config);
        for ( BackupMetaData backup : availableBackups )
        {
            long        age = System.currentTimeMillis() - backup.getModifiedDate();
            if ( age > exhibitor.getConfigManager().getConfig().getInt(IntConfigs.BACKUP_MAX_STORE_MS) )
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Cleaning backup: " + backup);
                backupProvider.get().deleteBackup(exhibitor, backup, config);
            }
        }

        lastRollCheck.set(System.currentTimeMillis());
    }
}
