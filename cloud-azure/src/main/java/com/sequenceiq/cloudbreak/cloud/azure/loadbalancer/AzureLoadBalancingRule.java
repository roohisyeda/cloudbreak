package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

public final class AzureLoadBalancingRule {
    private final String name;
    private final int backendPort;
    private final int frontendPort;

    /**
     * Create a load balancing rule where the backend and frontend port are the same.
     *
     * @param port the port to use for the backend and frontend of this rule.
     */
    public AzureLoadBalancingRule (int port) {
        this(defaultNameFromPort(port), port, port);
    }

    public AzureLoadBalancingRule (String name, int backendPort, int frontendPort) {
        this.backendPort = backendPort;
        this.frontendPort = frontendPort;
        this.name = name;
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
}
