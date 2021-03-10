package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class LaunchStackHandler implements CloudPlatformEventHandler<LaunchStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private EventBus eventBus;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public Class<LaunchStackRequest> type() {
        return LaunchStackRequest.class;
    }

    @Override
    public void accept(Event<LaunchStackRequest> launchStackRequestEvent) {
        LOGGER.debug("Received event: {}", launchStackRequestEvent);
        LaunchStackRequest request = launchStackRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        CloudStack cloudStack = request.getCloudStack();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = connector.resources().launch(ac, cloudStack, persistenceNotifier,
                    request.getAdjustmentType(), request.getThreshold());
            ResourcesStatePollerResult statePollerResult = waitForResources(ac, resourceStatus, cloudContext);
            if (isLoadBalancerEnabled(cloudContext.getPlatform())) {
                List<CloudResourceStatus> loadBalancerResourceStatus = connector.resources().launchLoadBalancer(ac, cloudStack,
                        persistenceNotifier);
                if (!loadBalancerResourceStatus.isEmpty()) {
                    ResourcesStatePollerResult loadBalancerStatePollerResult = waitForResources(ac, loadBalancerResourceStatus, cloudContext);
                    statePollerResult.addResults(loadBalancerStatePollerResult.getResults());
                }
            }
            LaunchStackResult result = ResourcesStatePollerResults.transformToLaunchStackResult(request, statePollerResult);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(launchStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Launching the stack successfully finished for {}", cloudContext);
        } catch (Exception e) {
            if (ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
                LOGGER.info("Interrupted exception is ignored as it has been thrown because of graceful shutdown of the java process.");
            }
            LaunchStackResult failure = new LaunchStackResult(e, request.getResourceId());
            LOGGER.warn("Error during launching the stack:", e);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(launchStackRequestEvent.getHeaders(), failure));
        }
    }

    /**
     * Creates a poll task which waits for the resources to be fully up and running, or more specifically,
     * for the resources to be in a permanent successful state.
     *
     * Returns the result of the poll task.
     */
    private ResourcesStatePollerResult waitForResources(AuthenticatedContext ac, List<CloudResourceStatus> resourceStatuses, CloudContext cloudContext)
            throws Exception {
        List<CloudResource> resources = ResourceLists.transform(resourceStatuses);
        PollTask<ResourcesStatePollerResult> task = statusCheckFactory.newPollResourcesStateTask(ac, resources, true);
        ResourcesStatePollerResult statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatuses);
        if (!task.completed(statePollerResult)) {
            statePollerResult = syncPollingScheduler.schedule(task);
        }
        return statePollerResult;
    }

    /**
     * Checks to see if load balancers are enabled for this account, also verifying that the entitlement
     * can be obtained successfully so as to keep errors out of the main accept call.
     */
    private boolean isLoadBalancerEnabled(Platform cloudPlatform) {
        if (ThreadBasedUserCrnProvider.getUserCrn() != null) {
            return CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform.value()) ||
                entitlementService.datalakeLoadBalancerEnabled(ThreadBasedUserCrnProvider.getAccountId());
        }

        return false;
    }
}
