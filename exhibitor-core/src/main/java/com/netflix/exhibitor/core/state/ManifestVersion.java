package com.netflix.exhibitor.core.state;

import com.google.common.io.Closeables;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.jar.Manifest;

public class ManifestVersion
{
    private final String    version;

    private static final String IMPLEMENTATION_VERSION = "Implementation-Version";
    private static final String IMPLEMENTATION_TITLE = "Implementation-Title";
    private static final String SNAPSHOT = "-SNAPSHOT";
    private static final String MAIN_CLASS = "Main-Class";

    public ManifestVersion()
    {
        String          localVersion = null;
        InputStream     stream = null;
        try
        {
            for ( URL manifestUrl : Collections.list(getClass().getClassLoader().getResources("META-INF/MANIFEST.MF")) )
            {
                if ( stream != null )
                {
                    Closeables.closeQuietly(stream);
                }
                stream = manifestUrl.openStream();
                Manifest        manifest = new Manifest(stream);
                String          title = manifest.getMainAttributes().getValue(IMPLEMENTATION_TITLE);
                String          mainClass = manifest.getMainAttributes().getValue(MAIN_CLASS);
                if ( "com.netflix.exhibitor.application.ExhibitorMain".equals(mainClass) || "exhibitor-core".equals(title) )
                {
                    localVersion = manifest.getMainAttributes().getValue(IMPLEMENTATION_VERSION);
                    if ( localVersion != null )
                    {
                        localVersion = "v" + localVersion;
                        if ( localVersion.endsWith(SNAPSHOT) )
                        {
                            localVersion = localVersion.substring(0, localVersion.length() - SNAPSHOT.length());
                        }
                    }
                    break;
                }
            }
        }
        catch ( Exception ignore )
        {
            // ignore
        }
        finally
        {
            Closeables.closeQuietly(stream);
        }

        version = (localVersion != null) ? localVersion : "dev-beta1";
    }

    public String getVersion()
    {
        return version;
    }
}
