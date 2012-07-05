package com.netflix.exhibitor.core;

import com.google.common.base.Preconditions;
import com.netflix.exhibitor.core.config.JQueryStyle;

/**
 * Various static arguments for the {@link Exhibitor} instance
 */
public class ExhibitorArguments
{
    final int           connectionTimeOutMs;
    final int           logWindowSizeLines;
    final int           configCheckMs;
    final String        extraHeadingText;
    final String        thisJVMHostname;
    final boolean       allowNodeMutations;
    final JQueryStyle   jQueryStyle;
    final int           restPort;
    final String        restPath;
    final String        restScheme;

    public static class Builder
    {
        private ExhibitorArguments      arguments = new ExhibitorArguments();

        public Builder connectionTimeOutMs(int connectionTimeOutMs)
        {
            arguments = new ExhibitorArguments(connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder logWindowSizeLines(int logWindowSizeLines)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder configCheckMs(int configCheckMs)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder extraHeadingText(String extraHeadingText)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder thisJVMHostname(String thisJVMHostname)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder allowNodeMutations(boolean allowNodeMutations)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder jQueryStyle(JQueryStyle jQueryStyle)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, jQueryStyle, arguments.restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder restPort(int restPort)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, restPort, arguments.restPath, arguments.restScheme);
            return this;
        }

        public Builder restPath(String restPath)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, restPath, arguments.restScheme);
            return this;
        }

        public Builder restScheme(String restScheme)
        {
            arguments = new ExhibitorArguments(arguments.connectionTimeOutMs, arguments.logWindowSizeLines, arguments.configCheckMs, arguments.extraHeadingText, arguments.thisJVMHostname, arguments.allowNodeMutations, arguments.jQueryStyle, arguments.restPort, arguments.restPath, restScheme);
            return this;
        }

        public ExhibitorArguments build()
        {
            Preconditions.checkArgument(arguments.connectionTimeOutMs > 0, "connectionTimeOutMs must be a positive number");
            Preconditions.checkArgument(arguments.logWindowSizeLines > 0, "logWindowSizeLines must be a positive number");
            Preconditions.checkArgument(arguments.configCheckMs > 0, "configCheckMs must be a positive number");
            Preconditions.checkArgument(arguments.restPort > 0, "restPort must be a positive number");
            Preconditions.checkArgument(arguments.restPath != null, "restPath cannot be null");

            return arguments;
        }

        private Builder()
        {
        }
    }

    public static Builder      builder()
    {
        return new Builder();
    }

    private ExhibitorArguments()
    {
        this(0, 0, 0, null, null, false, JQueryStyle.RED, 0, "/", "http");
    }

    public ExhibitorArguments(int connectionTimeOutMs, int logWindowSizeLines, int configCheckMs, String extraHeadingText, String thisJVMHostname, boolean allowNodeMutations, JQueryStyle jQueryStyle, int restPort, String restPath, String restScheme)
    {
        this.connectionTimeOutMs = connectionTimeOutMs;
        this.logWindowSizeLines = logWindowSizeLines;
        this.configCheckMs = configCheckMs;
        this.extraHeadingText = extraHeadingText;
        this.thisJVMHostname = thisJVMHostname;
        this.allowNodeMutations = allowNodeMutations;
        this.jQueryStyle = jQueryStyle;
        this.restPort = restPort;
        this.restPath = restPath;
        this.restScheme = restScheme;
    }
}
