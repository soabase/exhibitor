package com.netflix.exhibitor.mocks;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.spi.ProcessOperations;

public class MockExhibitor extends Exhibitor
{
    public MockExhibitor(InstanceConfig instanceConfig, ProcessOperations processOperations)
    {
        super(instanceConfig, processOperations);
    }
}
