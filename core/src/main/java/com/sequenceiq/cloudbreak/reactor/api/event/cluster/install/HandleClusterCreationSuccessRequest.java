package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class HandleClusterCreationSuccessRequest extends StackEvent {
    public HandleClusterCreationSuccessRequest(Long stackId) {
        super(stackId);
    }
}
