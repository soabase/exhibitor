package com.netflix.exhibitor.core.state;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;

public class TestServerList
{
    @Test
    public void     testSimple()
    {
        ServerList      serverList = new ServerList("a:1");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerList.ServerSpec("a", 1)));

        serverList = new ServerList("a:1,b:2");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerList.ServerSpec("a", 1), new ServerList.ServerSpec("b", 2)));

        serverList = new ServerList("b:2,a:1");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerList.ServerSpec("b", 2), new ServerList.ServerSpec("a", 1)));

        serverList = new ServerList("b:2,a:1");
        Assert.assertNotEquals(serverList.getSpecs(), Arrays.asList(new ServerList.ServerSpec("a", 1), new ServerList.ServerSpec("b", 2)));
    }

    @Test
    public void     testEdges()
    {
        ServerList      serverList = new ServerList("a:1,,,a,asfklasf,L:1x30,:*&*,$:$");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerList.ServerSpec("a", 1)));

        serverList = new ServerList("a :1    , b :    2 \t\t,     , : ,");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerList.ServerSpec("a", 1), new ServerList.ServerSpec("b", 2)));
    }
}
