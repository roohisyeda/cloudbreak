package com.sequenceiq.cloudbreak.cloud.azure;

import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancerInboundNatRule;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NicIPConfiguration;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureVmPublicIpProviderTest {

    private AzureClient mockAzureClient;
    private AzureVmPublicIpProvider azureVmPublicIpProvider;

    @BeforeEach
    void setUp() {
        azureVmPublicIpProvider = new AzureVmPublicIpProvider();
        mockAzureClient = mock(AzureClient.class);
    }

    @Test
    void preferIpAddressDirectlyAssignedToNic() {
        NetworkInterface mockNetworkInterface = mock(NetworkInterface.class);
        NicIPConfiguration mockNicIpConfiguration = mock(NicIPConfiguration.class);
        when(mockNetworkInterface.primaryIPConfiguration()).thenReturn(mockNicIpConfiguration);

        // set up "publicIpAddress" branch
        attachPublicIpAddressAsPrimary(mockNicIpConfiguration);

        // set up backend LB branch
        associateLoadBalancerViaBackendAddressPool(mockNicIpConfiguration);

        // set up inboundNatRule branch
        associateLoadBalancerViaInboundNatRule(mockNicIpConfiguration);

        String result = azureVmPublicIpProvider.getPublicIp(mockAzureClient, mockNetworkInterface, "bderriso-rg");

        assertEquals("10.44.44.1", result);
    }

    @Test
    void testGetPublicIpAddressFromLoadBalancerBackendPools() {
        NetworkInterface mockNetworkInterface = mock(NetworkInterface.class);
        NicIPConfiguration mockNicIpConfiguration = mock(NicIPConfiguration.class);
        when(mockNetworkInterface.primaryIPConfiguration()).thenReturn(mockNicIpConfiguration);

        // set up backend LB branch
        associateLoadBalancerViaBackendAddressPool(mockNicIpConfiguration);

        // set up InboundNatRule branch
        associateLoadBalancerViaInboundNatRule(mockNicIpConfiguration);

        // set up azure client to look up desired IP
        mockIdToIpAddressLookup(mockAzureClient, "10.44.44.2");

        String result = azureVmPublicIpProvider.getPublicIp(mockAzureClient, mockNetworkInterface, "bderriso-rg");

        assertEquals("10.44.44.2", result);
    }

    @Test
    void testGetPublicIpAddressFromNatRules() {
        NetworkInterface mockNetworkInterface = mock(NetworkInterface.class);
        NicIPConfiguration mockNicIpConfiguration = mock(NicIPConfiguration.class);
        when(mockNetworkInterface.primaryIPConfiguration()).thenReturn(mockNicIpConfiguration);

        // set up backend LB branch with empty list
        when(mockNicIpConfiguration.listAssociatedLoadBalancerBackends()).thenReturn(emptyList());

        // set up inboundNatRule branch
        LoadBalancerInboundNatRule mockLoadBalancerInboundNatRule = mock(LoadBalancerInboundNatRule.class);
        when(mockNicIpConfiguration.listAssociatedLoadBalancerInboundNatRules()).thenReturn(List.of(mockLoadBalancerInboundNatRule));

        LoadBalancer mockLoadBalancer = mock(LoadBalancer.class);
        when(mockLoadBalancer.publicIPAddressIds()).thenReturn(List.of("an_azure_id"));

        when(mockLoadBalancerInboundNatRule.parent()).thenReturn(mockLoadBalancer);
        // recycle previous mockLoadBalancer configuration

        mockIdToIpAddressLookup(mockAzureClient, "10.44.44.3");

        String result = azureVmPublicIpProvider.getPublicIp(mockAzureClient, mockNetworkInterface, "bderriso-rg");

        assertEquals("10.44.44.3", result);
    }

    @Test
    void testGetPublicIpReturnsNull() {
        NetworkInterface mockNetworkInterface = mock(NetworkInterface.class);
        NicIPConfiguration mockNicIpConfiguration = mock(NicIPConfiguration.class);
        when(mockNetworkInterface.primaryIPConfiguration()).thenReturn(mockNicIpConfiguration);
        // skip setting up a `publicIpAddress` object

        // set up backend pools and inbound NATs with empty lists
        when(mockNicIpConfiguration.listAssociatedLoadBalancerBackends()).thenReturn(emptyList());
        when(mockNicIpConfiguration.listAssociatedLoadBalancerInboundNatRules()).thenReturn(emptyList());

        String result = azureVmPublicIpProvider.getPublicIp(mockAzureClient, mockNetworkInterface, "bderriso-rg");

        assertNull(result);
    }

    private void attachPublicIpAddressAsPrimary(NicIPConfiguration mockNicIpConfiguration) {
        PublicIPAddress mockPublicIpAddress = mock(PublicIPAddress.class);
        when(mockNicIpConfiguration.getPublicIPAddress()).thenReturn(mockPublicIpAddress);
        when(mockPublicIpAddress.ipAddress()).thenReturn("10.44.44.1");
    }

    // The publicIP associated with a NIC can be retrieved by looking up the backend address pool the NIC belongs to,
    // then looking up the Public IP of the LB that routes to that pool.
    private void associateLoadBalancerViaBackendAddressPool(NicIPConfiguration mockNicIpConfiguration) {
        LoadBalancerBackend mockLoadBalancerBackend = mock(LoadBalancerBackend.class);
        when(mockNicIpConfiguration.listAssociatedLoadBalancerBackends()).thenReturn(List.of(mockLoadBalancerBackend));

        LoadBalancer mockLoadBalancer = mock(LoadBalancer.class);
        when(mockLoadBalancerBackend.parent()).thenReturn(mockLoadBalancer);
        when(mockLoadBalancer.publicIPAddressIds()).thenReturn(List.of("an_azure_id"));
    }

    private void associateLoadBalancerViaInboundNatRule(NicIPConfiguration mockNicIpConfiguration) {
        LoadBalancerInboundNatRule mockLoadBalancerInboundNatRule = mock(LoadBalancerInboundNatRule.class);
        when(mockNicIpConfiguration.listAssociatedLoadBalancerInboundNatRules()).thenReturn(List.of(mockLoadBalancerInboundNatRule));

        LoadBalancer mockLoadBalancer = mock(LoadBalancer.class);
        when(mockLoadBalancer.publicIPAddressIds()).thenReturn(List.of("an_azure_id"));

        // recycle previous mockLoadBalancer configuration
        when(mockLoadBalancerInboundNatRule.parent()).thenReturn(mockLoadBalancer);
    }

    private void mockIdToIpAddressLookup(AzureClient c, String ip) {
        PublicIPAddress secondMockPublicIpAddress = mock(PublicIPAddress.class);
        when(c.getPublicIpAddressById("an_azure_id")).thenReturn(secondMockPublicIpAddress);
        when(secondMockPublicIpAddress.ipAddress()).thenReturn(ip);
    }
}
