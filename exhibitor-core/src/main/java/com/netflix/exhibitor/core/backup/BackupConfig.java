package com.netflix.exhibitor.core.backup;

public class BackupConfig
{
    private final String        key;
    private final String        displayName;
    private final String        helpText;
    private final String        defaultValue;
    private final Type          type;

    public enum Type
    {
        STRING,
        INTEGER
    }

    public BackupConfig(String key, String displayName, String helpText, String defaultValue, Type type)
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
