package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import org.bouncycastle.util.IPAddress;

import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.*;

public final class AzureLoadBalancer {
    // todo: handling of rules and probes is naive here, need to create an association from a rule to the probes that it relies on.
    // I think we should _create_ a probe, then associate it with a rule and add it to the Load Balancer's list of probes.
    private List<AzureLoadBalancingRule> rules;
    private List<AzureLoadBalancerProbe> probes;

    public AzureLoadBalancer(CloudLoadBalancer cloudLoadBalancer) {
        rules = new ArrayList<>();
        rules = cloudLoadBalancer.getPortToTargetGroupMapping()
                .keySet()
                .stream()
                .map(portPair -> new AzureLoadBalancingRule(portPair.getTrafficPort()))
                .collect(toList());

        probes = cloudLoadBalancer.getPortToTargetGroupMapping()
                .keySet()
                .stream()
                .map(portPair -> new AzureLoadBalancerProbe(portPair.getHealthCheckPort()))
                .collect(toList());
    }

    public List<AzureLoadBalancingRule> getRules() {
        return rules;
    }

    public List<AzureLoadBalancerProbe> getProbes() {
        return probes;
    }
}
