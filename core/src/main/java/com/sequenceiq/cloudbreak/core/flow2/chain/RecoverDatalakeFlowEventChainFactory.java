package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_INIT_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterRecoveryTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class RecoverDatalakeFlowEventChainFactory implements FlowEventChainFactory<ClusterRecoveryTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_RECOVERY_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterRecoveryTriggerEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();
        chain.add(new StackEvent(ClusterTerminationEvent.TEARDOWN.event(), event.getResourceId(), event.accepted()));
        chain.add(new StackEvent(ClusterTerminationEvent.BRINGUP.event(), event.getResourceId(), event.accepted()));
        // TEARDOWN
        // BRINGUP

        chain.add(new ClusterRecoveryTriggerEvent(CLUSTER_UPGRADE_INIT_EVENT.event(), event.getResourceId(), event.accepted()));
        return chain;
    }
}
