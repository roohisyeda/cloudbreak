package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredFlowEventToClusterShapeConverterTest {

    private StructuredEventToClusterShapeConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToClusterShapeConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredFlowEvent) null));
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredSyncEvent) null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", flowClusterShape.getClusterTemplateName());
        Assert.assertEquals(0, flowClusterShape.getNodes());
        Assert.assertEquals("", flowClusterShape.getDefinitionDetails());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncClusterShape.getClusterTemplateName());
        Assert.assertEquals(0, syncClusterShape.getNodes());
        Assert.assertEquals("", syncClusterShape.getDefinitionDetails());
    }

    @Test
    public void testConversionWithValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        BlueprintDetails flowBlueprintDetails = new BlueprintDetails();
        flowBlueprintDetails.setName("My Blueprint");
        structuredFlowEvent.setBlueprintDetails(flowBlueprintDetails);

        UsageProto.CDPClusterShape flowSlusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("My Blueprint", flowSlusterShape.getClusterTemplateName());
        Assert.assertEquals(10, flowSlusterShape.getNodes());
        Assert.assertEquals("compute=3, gw=4, master=1, worker=2", flowSlusterShape.getHostGroupNodeCount());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        BlueprintDetails syncBlueprintDetails = new BlueprintDetails();
        syncBlueprintDetails.setName("My Blueprint");
        structuredSyncEvent.setBlueprintDetails(syncBlueprintDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("My Blueprint", syncClusterShape.getClusterTemplateName());
        Assert.assertEquals(10, syncClusterShape.getNodes());
        Assert.assertEquals("compute=3, gw=4, master=1, worker=2", syncClusterShape.getHostGroupNodeCount());
    }

    private StackDetails createStackDetails() {
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 1);
        InstanceGroupDetails worker = createInstanceGroupDetails("worker", 2);
        InstanceGroupDetails compute = createInstanceGroupDetails("compute", 3);
        InstanceGroupDetails gw = createInstanceGroupDetails("gw", 4);

        stackDetails.setInstanceGroups(List.of(master, worker, compute, gw));
        return stackDetails;
    }

    private InstanceGroupDetails createInstanceGroupDetails(String groupName, int nodeCount) {
        InstanceGroupDetails ig = new InstanceGroupDetails();
        ig.setGroupName(groupName);
        ig.setNodeCount(nodeCount);
        return ig;
    }
}