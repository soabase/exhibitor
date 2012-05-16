package com.netflix.exhibitor.core.analyze;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * Abstraction to represent and node in the Resource Allocation Graph
 */
class Node
{
    private final String        value;
    private final NodeTypes     type;
    private final List<Node>    edges;

    // guarded by sync
    // use an int instead of a boolean so that it doesn't need to be cleared for each iteration
    private int flagValue;

    Node(NodeTypes type, String value)
    {
        this.type = type;
        this.value = value;
        edges = Lists.newArrayList();
        flagValue = 0;
    }

    NodeTypes getType()
    {
        return type;
    }

    String getValue()
    {
        return value;
    }

    List<Node> getEdges()
    {
        return edges;
    }

    synchronized int getFlagValue()
    {
        return flagValue;
    }

    synchronized void setFlagValue(int newValue)
    {
        flagValue = newValue;
    }

    @Override
    public String toString()
    {
        return value;
    }
}
