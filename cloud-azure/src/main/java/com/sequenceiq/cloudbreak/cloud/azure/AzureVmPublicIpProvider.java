package com.sequenceiq.cloudbreak.cloud.azure;

import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerInboundNatRule;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
     * @return the public IP address associated with the NIC, or {@code null}
     */
    String getPublicIp(AzureClient azureClient, NetworkInterface networkInterface, String resourceGroupName) {
        NicIPConfiguration primaryIpConfiguration = networkInterface.primaryIPConfiguration();
        PublicIPAddress publicIpAddress = primaryIpConfiguration.getPublicIPAddress();

        if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
            return publicIpAddress.ipAddress();
        }

        Optional<PublicIPAddress> ipAdddressFromBackendPools = primaryIpConfiguration.listAssociatedLoadBalancerBackends().stream()
                .map(LoadBalancerBackend::parent)
                .flatMap(lb -> lb.publicIPAddressIds().stream())
                .map(azureClient::getPublicIpAddressById)
                .findFirst();

        Optional<PublicIPAddress> ipAddressFromInboundNatRules = primaryIpConfiguration.listAssociatedLoadBalancerInboundNatRules().stream()
                .map(LoadBalancerInboundNatRule::parent)
                .flatMap(lb -> lb.publicIPAddressIds().stream())
                .map(azureClient::getPublicIpAddressById)
                .findFirst();

        if (ipAdddressFromBackendPools.isPresent()) {
            return ipAdddressFromBackendPools.get().ipAddress();
        } else if (ipAddressFromInboundNatRules.isPresent()) {
            return ipAddressFromInboundNatRules.get().ipAddress();
        } else {
            return null;
        }
    }
}
