package com.sequenceiq.flow.core.chain.finalize.flowevents;

import reactor.rx.Promise;

public class FlolwChainFinalizeFailedPayload extends FlowChainFinalizePayload {

    private Exception exception;

    public FlolwChainFinalizeFailedPayload(String flowChainName, Long resourceId, Exception exception) {
        super(flowChainName, resourceId, new Promise<>());
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
