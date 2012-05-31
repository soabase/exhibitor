package com.netflix.exhibitor.core.rest;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class SmartSort
{
    private static final String formatPattern = "0000000000";

    private static final Comparator<String> sequentialComparator = new Comparator<String>()
    {
        @Override
        public int compare(String node1, String node2)
        {
            String      sequence1 = node1.substring(node1.length() - formatPattern.length());
            String      sequence2 = node2.substring(node2.length() - formatPattern.length());
            return sequence1.compareTo(sequence2);
        }
    };

    static void     sortChildren(List<String> children)
    {
        NumberFormat    format = new DecimalFormat(formatPattern);

        boolean         doDefault = true;
        try
        {
            outer: do
            {
                for ( String node : children )
                {
                    if ( node.length() >= formatPattern.length() )
                    {
                        format.parse(node.substring(node.length() - formatPattern.length()));
                    }
                    else
                    {
                        break outer;
                    }
                }

                Collections.sort(children, sequentialComparator);
                doDefault = false;
            } while ( false );
        }
        catch ( ParseException e )
        {
            // not sequential
        }

        if ( doDefault )
        {
            Collections.sort(children);
        }
    }

    private SmartSort()
    {
    }
}
