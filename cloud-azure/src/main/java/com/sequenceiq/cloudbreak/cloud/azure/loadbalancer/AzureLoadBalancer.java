package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import org.bouncycastle.util.IPAddress;

import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.*;

public final class AzureLoadBalancer {
    private List<AzureLoadBalancingRule> rules;

    public AzureLoadBalancer(CloudLoadBalancer cloudLoadBalancer) {
        rules = new ArrayList<>();
        rules = cloudLoadBalancer.getPortToTargetGroupMapping().keySet().stream().map(portPair -> new AzureLoadBalancingRule(portPair.getTrafficPort())).collect(toList());
    }

    public List<AzureLoadBalancingRule> getRules() {
        return rules;
    }
}
