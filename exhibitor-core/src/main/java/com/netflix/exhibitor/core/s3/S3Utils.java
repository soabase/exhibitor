package com.netflix.exhibitor.core.s3;

import org.apache.commons.codec.binary.Base64;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class S3Utils
{
    public static String toBase64(byte[] md5)
    {
        byte encoded[] = Base64.encodeBase64(md5, false);
        return new String(encoded);
    }

    public static byte[] md5(ByteBuffer buffer)
    {
        try
        {
            MessageDigest mdigest = MessageDigest.getInstance("MD5");
            mdigest.update(buffer);
            return mdigest.digest();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            buffer.rewind();
        }
    }

    public static String toHex(byte[] digest)
    {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for ( byte b : digest )
        {
            String hex = Integer.toHexString(b);
            if ( hex.length() == 1 )
            {
                sb.append("0");
            }
            else if ( hex.length() == 8 )
            {
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase();
    }
}
