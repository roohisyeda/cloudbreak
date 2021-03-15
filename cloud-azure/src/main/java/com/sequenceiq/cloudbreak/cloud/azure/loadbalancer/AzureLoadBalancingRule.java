package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;

public final class AzureLoadBalancingRule {
    private final String name;

    private final int backendPort;

    private final int frontendPort;

    private final AzureLoadBalancerProbe probe;

    public AzureLoadBalancingRule(TargetGroupPortPair portPair) {
        this.backendPort = portPair.getTrafficPort();
        this.frontendPort = portPair.getTrafficPort();
        this.name = defaultNameFromPort(portPair.getTrafficPort());
        this.probe = new AzureLoadBalancerProbe(portPair.getHealthCheckPort());
    }

    private static String defaultNameFromPort(int port) {
        return "port-" + Integer.toString(port) + "-rule";
    }

    public String getName() {
        return name;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public int getFrontendPort() {
        return frontendPort;
    }

    public AzureLoadBalancerProbe getProbe() {
        return probe;
    }
}
