package org.reveno.atp.commons;

import java.util.Arrays;

/**
 * WARNING! should be used very carefully as for internal performance
 * issues much of checks are ommited here.
 */
public final class ByteArrayWrapper
{
    public final byte[] data;

    public ByteArrayWrapper(byte[] data)
    {
        if (data == null)
        {
            throw new NullPointerException();
        }
        this.data = data;
    }

    @Override
    public boolean equals(Object other)
    {
        // for performance we omit instanceof checks here - with right internal usage
        // this case is impossible to get
        return Arrays.equals(data, ((ByteArrayWrapper)other).data);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(data);
    }
}