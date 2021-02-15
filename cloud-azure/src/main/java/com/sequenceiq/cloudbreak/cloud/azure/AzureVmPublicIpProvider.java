package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;

import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerInboundNatRule;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@Component
class AzureVmPublicIpProvider {

    /**
     * Return a public IP address for the supplied network interface.
     * The public IP address is based off of one of:
     * <ul>
     *     <li>A public IP address assigned directly to the NIC.</li>
     *     <li>A public IP address assigned to a load balancer that routes to the NIC.</li>
     * </ul>
     *
     * If both a directly assigned public IP address and load balancer are associated with the NIC, the directly assigned
     * address is used.
     *
     * @param azureClient dependency for retrieving load balancer IPs
     * @param networkInterface the network interface to get public IPs for
     * @param resourceGroupName used to guess the load balancer name associated with this NIC
     * @return
     */
    String getPublicIp(AzureClient azureClient, NetworkInterface networkInterface, String resourceGroupName) {
        PublicIPAddress publicIpAddress = networkInterface.primaryIPConfiguration().getPublicIPAddress();

        // get LBs associated with the NIC
        List<LoadBalancerBackend> backends = networkInterface.primaryIPConfiguration().listAssociatedLoadBalancerBackends();
        List<LoadBalancerInboundNatRule> inboundNatRules = networkInterface.primaryIPConfiguration().listAssociatedLoadBalancerInboundNatRules();
        String publicIp = null;

        // if Azure knows about LB backends, and there are associated inbound NAT rules, we retrieve the IP addresses of the LBs.
        // The id of the loadbalancer is assumed to be "resourceGroupName" + "lb".
        if (!backends.isEmpty() || !inboundNatRules.isEmpty()) {
            publicIp = azureClient.getLoadBalancerIps(resourceGroupName, AzureUtils.getLoadBalancerId(resourceGroupName)).get(0);
        }

        // throw away the name we constructed above if `publicIpAddress` is available.
        if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
            publicIp = publicIpAddress.ipAddress();
        }

        return publicIp;
    }
}
