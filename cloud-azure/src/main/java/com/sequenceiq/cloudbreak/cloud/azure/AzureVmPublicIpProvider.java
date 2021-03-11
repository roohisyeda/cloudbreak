package com.sequenceiq.cloudbreak.cloud.azure;

import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import org.springframework.stereotype.Component;

@Component
class AzureVmPublicIpProvider {

    String getPublicIp(AzureClient azureClient, AzureUtils azureUtils, NetworkInterface networkInterface, String resourceGroup) {
        PublicIPAddress publicIpAddress = networkInterface.primaryIPConfiguration().getPublicIPAddress();
        // get LBs associated with the NIC
        String publicIp = null;

        // throw away the name we constructed above if `publicIpAddress` is available.
        if (publicIpAddress != null && publicIpAddress.ipAddress() != null) {
            publicIp = publicIpAddress.ipAddress();
        }

        return publicIp;
    }
}
