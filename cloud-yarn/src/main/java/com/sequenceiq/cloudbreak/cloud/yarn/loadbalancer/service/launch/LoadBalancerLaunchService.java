package com.sequenceiq.cloudbreak.cloud.yarn.loadbalancer.service.launch;

import static com.sequenceiq.cloudbreak.cloud.yarn.YarnResourceConnector.ARTIFACT_TYPE_DOCKER;
import static com.sequenceiq.cloudbreak.cloud.yarn.YarnResourceConnector.checkApplicationAlreadyCreated;
import static com.sequenceiq.cloudbreak.cloud.yarn.YarnResourceConnector.createApplication;
import static com.sequenceiq.cloudbreak.cloud.yarn.YarnResourceConnector.initializeRequest;
import static com.sequenceiq.common.api.type.ResourceType.YARN_LOAD_BALANCER;

import java.net.MalformedURLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.yarn.ApplicationNameUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Artifact;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFile;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.ConfigFileType;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Configuration;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Container;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Resource;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class LoadBalancerLaunchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerLaunchService.class);

    private final ApplicationNameUtil applicationNameUtil;

    /**
     * Must match an existing Docker image in the image catalog being used.
     */
    private final String loadBalancerImageName = "docker.io/sgarciaacosta/haproxy:latest";

    private final int loadBalancerNumCPUs = 1;

    /**
     * In megabytes.
     */
    private final int loadBalancerMemorySize = 1024;

    @Value("${cb.yarn.defaultQueue}")
    private String defaultQueue;

    @Value("${cb.yarn.defaultLifeTime:}")
    private int defaultLifeTime;

    LoadBalancerLaunchService(ApplicationNameUtil applicationNameUtil) {
        Objects.requireNonNull(applicationNameUtil);
        this.applicationNameUtil = applicationNameUtil;
    }

    public CloudResource launch(AuthenticatedContext authenticatedContext, CloudStack cloudStack, PersistenceNotifier persistenceNotifier, YarnClient yarnClient)
            throws Exception {
        String applicationName = applicationNameUtil.createLoadBalancerName(authenticatedContext);

        if (!checkApplicationAlreadyCreated(yarnClient, applicationName)) {
            LOGGER.debug("Creating the loadbalancer application for the Yarn datalake.");
            CreateApplicationRequest createApplicationRequest = createLoadBalancerRequest(cloudStack, applicationName, authenticatedContext, yarnClient);
            createApplication(yarnClient, createApplicationRequest);
        }

        // Create an object for the new loadbalancer application and persist it in the resources table.
        CloudResource loadBalancerApplication = new Builder().type(YARN_LOAD_BALANCER).name(applicationName).build();
        persistenceNotifier.notifyAllocation(loadBalancerApplication, authenticatedContext.getCloudContext());
        return loadBalancerApplication;
    }

    /**
     * Creates a load balancer application request, specifically by:
     *      - Getting the IP addresses of the gateway nodes that are already running
     *      - Creating YarnComponent objects for each (if more than one) loadbalancer and pointing them to the gateway IPs.
     */
    private CreateApplicationRequest createLoadBalancerRequest(CloudStack cloudStack, String applicationName, AuthenticatedContext authenticatedContext,
            YarnClient yarnClient) {
        CreateApplicationRequest createApplicationRequest = initializeRequest(cloudStack, applicationName, defaultQueue, defaultLifeTime);
        List<String> gatewayIPs = getGatewayIPs(authenticatedContext, yarnClient, cloudStack);
        List<YarnComponent> loadBalancerComponents = createLoadBalancerComponents(cloudStack, gatewayIPs, applicationName);
        createApplicationRequest.setComponents(loadBalancerComponents);
        return createApplicationRequest;
    }

    /**
     * Uses the provided YarnClient to obtain information about the existing Yarn application. Takes advantage of the fact
     * that the applicationNameUtil has the exact same logic for creating the name of the original application.
     *
     * Returns the extracted IPs of only the gateway nodes in the Yarn application.
     */
    private List<String> getGatewayIPs(AuthenticatedContext authenticatedContext, YarnClient yarnClient, CloudStack cloudStack) {
        LOGGER.debug("Getting the IPs of the existing gateway Yarn containers for the loadbalancer.");
        String applicationName = applicationNameUtil.createApplicationName(authenticatedContext);
        Iterable<Container> foundContainers = getContainers(applicationName, yarnClient);
        Set<String> gatewayGroupNames = getGatewayGroupNames(cloudStack);

        List<String> gatewayIPs = Lists.newArrayList();
        foundContainers.forEach(container -> {
            if (gatewayGroupNames.contains(container.getComponentName())) {
                gatewayIPs.add(container.getIp() + ":443");
            }
        });
        return gatewayIPs;
    }

    /**
     * Gets all of the containers currently running in the YCloud ecosystem within the application
     * given by the application name provided.
     */
    public static Iterable<Container> getContainers(String applicationName, YarnClient yarnClient) {
        LOGGER.debug("Getting the Yarn containers for application " + applicationName + ".");
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(applicationName);
        ResponseContext responseContext;

        try {
            responseContext = yarnClient.getApplicationDetail(applicationDetailRequest);
        } catch (MalformedURLException ex) {
            LOGGER.error("Failed to get information for the Yarn loadbalancer! Application name: " + applicationName + " Error: " + ex.getMessage());
            throw new CloudConnectorException("Failed to get information for the Yarn loadbalancer.", ex);
        }

        if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
            ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();
            return applicationDetailResponse.getContainers();
        } else {
            LOGGER.error("Failed to get yarn container! Application name: " + applicationName);
            ApplicationErrorResponse errorResponse = responseContext.getResponseError();
            throw new CloudConnectorException(String.format("Failed to get yarn container details: HTTP Return: %d Error: %s",
                    responseContext.getStatusCode(), errorResponse == null ? "unknown" : errorResponse.getDiagnostics()));
        }
    }

    /**
     * Gets the names of the instance groups that are Gateway types, the names can then be compared against
     * Yarn container names to check whether a container is a gateway container or not.
     */
    private Set<String> getGatewayGroupNames(CloudStack cloudStack) {
        return cloudStack.getGroups().stream().filter(group -> InstanceGroupType.isGateway(group.getType()))
                .map(Group::getName).collect(Collectors.toSet());
    }

    /**
     * Creates a Yarn component for each of the loadbalancers that are to be created, using a set of pre-defined
     * constants for the various component parameters.
     *
     * Each container is pointed at a specific docker image which contains the loadbalancing service and logic.
     */
    private List<YarnComponent> createLoadBalancerComponents(CloudStack cloudStack, List<String> gatewayIPs, String applicationName) {
        LOGGER.debug("Creating the loadbalancer components for application " + applicationName + " with gatewayIPs: " + gatewayIPs.toString() + ".");
        Artifact artifact = createLoadBalancerArtifact();
        Resource resource = createLoadBalancerResource();
        String launchCommand = createLoadBalancerLaunchCommand(cloudStack);
        Configuration configuration = createLoadBalancerConfiguration(gatewayIPs);

        List<YarnComponent> loadBalancerComponents = Lists.newArrayList();
        for (CloudLoadBalancer loadBalancer : cloudStack.getLoadBalancers()) {
            String componentName = applicationName + loadBalancer.getType().name();
            loadBalancerComponents.add(createLoadBalancerComponent(componentName, artifact, resource, launchCommand, configuration));
        }

        return loadBalancerComponents;
    }

    /**
     * Creates a YarnComponent, using specific parameters to set up the load balancer service on the designated
     * docker image pointed to.
     */
    private YarnComponent createLoadBalancerComponent(String name, Artifact artifact, Resource resource, String launchCommand, Configuration configuration) {
        YarnComponent component = new YarnComponent();
        component.setName(name);
        component.setArtifact(artifact);
        component.setResource(resource);
        component.setNumberOfContainers(1);
        component.setDependencies(Collections.emptyList());
        component.setRunPrivilegedContainer(true);
        component.setLaunchCommand(launchCommand);
        component.setConfiguration(configuration);
        return component;
    }

    /**
     * Creates the artifact object for the loadbalancer Yarn component, which points at the specific Docker image
     * that will set up the actual service which will do the loadbalancing.
     */
    private Artifact createLoadBalancerArtifact() {
        Artifact artifact = new Artifact();
        artifact.setId(loadBalancerImageName);
        artifact.setType(ARTIFACT_TYPE_DOCKER);
        return artifact;
    }

    /**
     * Creates the resource object for the loadbalancer Yarn component, which specifies the amount of
     * CPUs to use and the amount of memory to use for the container.
     */
    private Resource createLoadBalancerResource() {
        Resource resource = new Resource();
        resource.setCpus(loadBalancerNumCPUs);
        resource.setMemory(loadBalancerMemorySize);
        return resource;
    }

    /**
     * Creates the launch command for the loadbalancer Yarn component, which uses a custom start script.
     */
    private String createLoadBalancerLaunchCommand(CloudStack cloudStack) {
        return String.format("/bootstrap/start-systemd '%s' '%s' '%s'",
            Base64.getEncoder().encodeToString(cloudStack.getImage().getUserDataByType(InstanceGroupType.CORE).getBytes()),
            cloudStack.getLoginUserName(), cloudStack.getPublicKey());
    }

    /**
     * Creates the configuration object for the loadbalancer Yarn component, which specifies the backend servers
     * the loadbalancer container will balance against, as well as any other custom properties for the
     * Docker image to use.
     *
     * The name of the destination file must match the name of the properties file used for the Docker image.
     */
    private Configuration createLoadBalancerConfiguration(List<String> gatewayIPs) {
        Map<String, String> propsMap = Maps.newHashMap();
        propsMap.put("conf.cb-conf.per.component", "true");
        propsMap.put("site.cb-conf.groupname", "'loadbalancer'");
        propsMap.put("site.cb-conf.servers", '\'' + String.join(" ", gatewayIPs) + '\'');

        ConfigFile configFileProps = new ConfigFile();
        configFileProps.setType(ConfigFileType.PROPERTIES.name());
        configFileProps.setSrcFile("cb-conf");
        configFileProps.setDestFile("/etc/cloudbreak-loadbalancer.props");

        Configuration configuration = new Configuration();
        configuration.setProperties(propsMap);
        configuration.setFiles(Collections.singletonList(configFileProps));
        return configuration;
    }
}
