package com.netflix.exhibitor.core.state;

import com.netflix.exhibitor.core.cluster.ServerList;
import com.netflix.exhibitor.core.cluster.ServerSpec;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;

public class TestServerList
{
    @Test
    public void     testSimple()
    {
        ServerList      serverList = new ServerList("1:a");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerSpec("a", 1)));

        serverList = new ServerList("1:a,2:b");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerSpec("a", 1), new ServerSpec("b", 2)));

        serverList = new ServerList("2:b,1:a");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerSpec("b", 2), new ServerSpec("a", 1)));

        serverList = new ServerList("2:b,1:a");
        Assert.assertNotEquals(serverList.getSpecs(), Arrays.asList(new ServerSpec("a", 1), new ServerSpec("b", 2)));
    }

    @Test
    public void     testEdges()
    {
        ServerList      serverList = new ServerList("1:a,,,a,asfklasf,L:1x30,:*&*,$:$");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerSpec("a", 1)));

        serverList = new ServerList("1 :a    , 2 :    b \t\t,     , : ,");
        Assert.assertEquals(serverList.getSpecs(), Arrays.asList(new ServerSpec("a", 1), new ServerSpec("b", 2)));
    }
}
