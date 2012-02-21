package com.netflix.exhibitor.core.backup;

/**
 * Details on a single backup config value
 */
public class BackupConfigSpec
{
    private final String        key;
    private final String        displayName;
    private final String        helpText;
    private final String        defaultValue;
    private final Type          type;

    /**
     * The config type - used by the UI to determine which editing widget to use
     */
    public enum Type
    {
        STRING,
        INTEGER
    }

    /**
     * @param key unique key to use when storing the value
     * @param displayName the end-user name of the config
     * @param helpText long description of the config
     * @param defaultValue default value for the config
     * @param type type
     */
    public BackupConfigSpec(String key, String displayName, String helpText, String defaultValue, Type type)
    {
        this.key = key;
        this.displayName = displayName;
        this.helpText = helpText;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    public String getKey()
    {
        return key;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getHelpText()
    {
        return helpText;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public Type getType()
    {
        return type;
    }
}
