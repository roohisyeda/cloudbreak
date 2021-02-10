package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

public class AzureLoadBalancerProbe {
    private final int port;
    private final String name;

    public AzureLoadBalancerProbe(int port) {
        this(port, "port-" + Integer.toString(port ) + "-probe");
    }

    public AzureLoadBalancerProbe (int port, String name) {
        this.port = port;
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }
}
