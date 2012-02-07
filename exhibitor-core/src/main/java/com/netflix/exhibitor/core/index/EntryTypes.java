package com.netflix.exhibitor.core.index;

public enum EntryTypes
{
    CREATE_PERSISTENT(0),
    CREATE_EPHEMERAL(1),
    DELETE(2),
    SET_DATA(3)
    ;

    private final int id;

    public int getId()
    {
        return id;
    }

    public static EntryTypes    getFromId(int id)
    {
        for ( EntryTypes type : values() )
        {
            if ( type.getId() == id )
            {
                return type;
            }
        }
        return null;
    }

    public static EntryTypes    getFromId(String id)
    {
        int     intId = 0;
        try
        {
            intId = Integer.parseInt(id);
        }
        catch ( NumberFormatException ignore )
        {
            // ignore
        }
        return getFromId(intId);
    }

    private EntryTypes(int id)
    {
        this.id = id;
    }
}
