package com.netflix.exhibitor.core.analyze;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Analysis
{
    private final String                    error;
    private final Collection<PathComplete>  completeData;
    private final List<Set<String>>         possibleCycles;

    Analysis(String error, Collection<PathComplete> completeData, List<Set<String>> possibleCycles)
    {
        this.error = error;
        this.completeData = ImmutableList.copyOf(completeData);

        ImmutableList.Builder<Set<String>> builder = ImmutableList.builder();
        for ( Set<String> pc : possibleCycles )
        {
            builder.add(ImmutableSet.copyOf(pc));
        }
        this.possibleCycles = builder.build();
    }

    public String getError()
    {
        return error;
    }

    public List<Set<String>> getPossibleCycles()
    {
        return possibleCycles;
    }

    public Collection<PathComplete> getCompleteData()
    {
        return completeData;
    }
}
