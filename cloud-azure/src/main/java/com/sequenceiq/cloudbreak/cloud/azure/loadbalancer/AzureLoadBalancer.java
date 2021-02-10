package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public final class AzureLoadBalancer {
    // todo: handling of rules and probes is naive here, need to create an association from a rule to the probes that it relies on.
    // I think we should _create_ a probe, then associate it with a rule and add it to the Load Balancer's list of probes.
    private final List<AzureLoadBalancingRule> rules;
    private final Set<AzureLoadBalancerProbe> probes;
    private final String name = "MyLoadBalancer"; //todo: pass this in

    public AzureLoadBalancer(CloudLoadBalancer cloudLoadBalancer) {
        rules = cloudLoadBalancer.getPortToTargetGroupMapping()
                .keySet()
                .stream()
                .map(AzureLoadBalancingRule::new)
                .collect(toList());

        // we want to derive the set of probes from the rules used in the load balancer
        probes = rules.stream()
                .map(AzureLoadBalancingRule::getProbe)
                .collect(toSet());
    }

    public Collection<AzureLoadBalancingRule> getRules() {
        return rules;
    }

    public Collection<AzureLoadBalancerProbe> getProbes() {
        return probes;
    }

    public String getName() {
        return name;
    }
}
