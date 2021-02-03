package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.microsoft.azure.management.Azure;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;

public final class AzureLoadBalancingRule {
    private final int backendPort;
    private final int frontendPort;

    /**
     * Create a load balancing rule where the backend and frontend port are the same.
     *
     * @param port the port to use for the backend and frontend of this rule.
     */
    public AzureLoadBalancingRule (int port) {
        this(port, port);
    }

    public AzureLoadBalancingRule (int backendPort, int frontendPort) {
        this.backendPort = backendPort;
        this.frontendPort = frontendPort;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public int getFrontendPort() {
        return frontendPort;
    }
}
