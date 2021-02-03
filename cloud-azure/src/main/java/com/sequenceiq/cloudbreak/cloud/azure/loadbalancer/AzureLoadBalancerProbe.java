package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

public class AzureLoadBalancerProbe {
    private final int port;

    public AzureLoadBalancerProbe(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
