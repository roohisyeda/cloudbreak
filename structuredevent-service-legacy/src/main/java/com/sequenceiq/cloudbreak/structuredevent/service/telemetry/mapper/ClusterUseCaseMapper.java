package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

@Component
public class ClusterUseCaseMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUseCaseMapper.class);

    @Inject
    private ClusterRequestProcessingStepMapper clusterRequestProcessingStepMapper;

    @Inject
    private List<FlowUseCaseMapper> useCaseMappers;

    @Inject
    private DefaultFlowUseCaseMapper defaultFlowUseCaseMapper;

    private Map<String, FlowUseCaseMapper> useCaseMapperMap;

    @PostConstruct
    private void initialize() {
        useCaseMapperMap = useCaseMappers.stream()
                .filter(flowUseCaseMapper -> StringUtils.isNotEmpty(flowUseCaseMapper.getRootFlowChainType()))
                .collect(Collectors.toMap(FlowUseCaseMapper::getRootFlowChainType, flowUseCaseMapper -> flowUseCaseMapper));
    }

    // At the moment we need to introduce a complex logic to figure out the use case
    public UsageProto.CDPClusterStatus.Value useCase(FlowDetails flow) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        String rootFlowChainType = getRootFlowChainType(flow.getFlowChainType());
        FlowUseCaseMapper flowUseCaseMapper = useCaseMapperMap.getOrDefault(rootFlowChainType, defaultFlowUseCaseMapper);
        if (clusterRequestProcessingStepMapper.isFirstStep(flow)) {
            useCase = flowUseCaseMapper.mapFirstStepToUseCase(flow.getFlowType())
                    .orElse(firstStepToUseCaseMapping(rootFlowChainType, flow.getFlowType()));
        } else if (clusterRequestProcessingStepMapper.isLastStep(flow)) {
            useCase = flowUseCaseMapper.mapLastStepToUseCase(flow.getFlowType(), flow.getFlowState())
                    .orElse(lastStepToUseCaseMapping(rootFlowChainType, flow.getFlowType(), flow.getFlowState()));
        }
        LOGGER.debug("FlowDetails: {}, Usecase: {}", flow, useCase);
        return useCase;
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
    private UsageProto.CDPClusterStatus.Value firstStepToUseCaseMapping(String rootFlowChainType, String flowType) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowType) {
            case "CloudConfigValidationFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
                break;
            case "ClusterTerminationFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_STARTED;
                break;
            case "StackUpscaleConfig":
                useCase = UsageProto.CDPClusterStatus.Value.UPSCALE_STARTED;
                break;
            case "ClusterDownscaleFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.DOWNSCALE_STARTED;
                break;
            case "StackStartFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.RESUME_STARTED;
                break;
            case "ClusterStopFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.SUSPEND_STARTED;
                break;
            case "ClusterCertificateRenewFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.RENEW_PUBLIC_CERT_STARTED;
                break;
            case "CertRotationFlowConfig":
                useCase = UsageProto.CDPClusterStatus.Value.RENEW_CLUSTER_INTERNAL_CERT_STARTED;
                break;
            default:
                LOGGER.debug("Flow type: {}", flowType);
        }
        LOGGER.debug("Mapping flow type to use-case: [flowchain: {}, flow: {}]: usecase: {}", rootFlowChainType, flowType, useCase);
        return useCase;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF: CyclomaticComplexity
    private UsageProto.CDPClusterStatus.Value lastStepToUseCaseMapping(String rootFlowChainType, String flowType, String flowState) {
        UsageProto.CDPClusterStatus.Value useCase = UsageProto.CDPClusterStatus.Value.UNSET;
        switch (flowState) {
            case "CLUSTER_CREATION_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
                break;
            case "VALIDATE_CLOUD_CONFIG_FAILED_STATE":
            case "VALIDATE_KERBEROS_CONFIG_FAILED_STATE":
            case "EXTERNAL_DATABASE_CREATION_FAILED_STATE":
            case "CLUSTER_CREATION_FAILED_STATE":
            case "STACK_CREATION_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
                break;
            case "TERMINATION_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_FINISHED;
                break;
            case "CLUSTER_TERMINATION_FAILED_STATE":
            case "EXTERNAL_DATABASE_TERMINATION_FAILED_STATE":
            case "TERMINATION_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DELETE_FAILED;
                break;
            case "FINALIZE_UPSCALE_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.UPSCALE_FINISHED;
                break;
            case "CLUSTER_UPSCALE_FAILED_STATE":
            case "UPSCALE_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.UPSCALE_FAILED;
                break;
            case "DOWNSCALE_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DOWNSCALE_FINISHED;
                break;
            case "CLUSTER_DOWNSCALE_FAILED_STATE":
            case "DOWNSCALE_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.DOWNSCALE_FAILED;
                break;
            case "START_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RESUME_FINISHED;
                break;
            case "CLUSTER_START_FAILED_STATE":
            case "EXTERNAL_DATABASE_START_FAILED_STATE":
            case "START_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RESUME_FAILED;
                break;
            case "STOP_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.SUSPEND_FINISHED;
                break;
            case "CLUSTER_STOP_FAILED_STATE":
            case "EXTERNAL_DATABASE_STOP_FAILED_STATE":
            case "STOP_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.SUSPEND_FAILED;
                break;
            case "CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RENEW_PUBLIC_CERT_FINISHED;
                break;
            case "CLUSTER_CERTIFICATE_RENEW_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RENEW_PUBLIC_CERT_FAILED;
                break;
            case "CERT_ROTATION_FINISHED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RENEW_CLUSTER_INTERNAL_CERT_FINISHED;
                break;
            case "CERT_ROTATION_FAILED_STATE":
                useCase = UsageProto.CDPClusterStatus.Value.RENEW_CLUSTER_INTERNAL_CERT_FAILED;
                break;
            default:
                LOGGER.debug("Flow state: {}", flowState);
        }
        LOGGER.debug("Mapping last flow state to use-case: {}, {}", flowState, useCase);
        return useCase;
    }
    //CHECKSTYLE:ON

    private String getRootFlowChainType(String flowChainTypes) {
        if (StringUtils.isNotEmpty(flowChainTypes)) {
            return flowChainTypes.split("/")[0];
        }
        return "";
    }
}
