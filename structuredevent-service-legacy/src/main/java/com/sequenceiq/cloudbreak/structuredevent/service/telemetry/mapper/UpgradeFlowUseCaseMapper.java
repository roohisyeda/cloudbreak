package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

@Component
public class UpgradeFlowUseCaseMapper implements FlowUseCaseMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeFlowUseCaseMapper.class);

    private final Map<String, UsageProto.CDPClusterStatus.Value> firstFlowConfigToUseCaseMap = Map.of(
            "SaltUpdateFlowConfig", UsageProto.CDPClusterStatus.Value.UPGRADE_STARTED
    );

    private final Map<String, UsageProto.CDPClusterStatus.Value> lastFlowStateToUseCaseMap = Map.of(
            "CLUSTER_UPGRADE_FINISHED_STATE", UsageProto.CDPClusterStatus.Value.UPGRADE_FINISHED,
            "SALT_UPDATE_FAILED_STATE", UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED,
            "CLUSTER_UPGRADE_FAILED_STATE", UsageProto.CDPClusterStatus.Value.UPGRADE_FAILED
    );

    @Override
    public String getRootFlowChainType() {
        return "UpgradeDatalakeFlowEventChainFactory";
    }

    @Override
    public Optional<UsageProto.CDPClusterStatus.Value> mapFirstStepToUseCase(String flowType) {
        if (StringUtils.isNotEmpty(flowType)) {
            UsageProto.CDPClusterStatus.Value useCase = firstFlowConfigToUseCaseMap.getOrDefault(flowType, UsageProto.CDPClusterStatus.Value.UNSET);
            LOGGER.debug("Mapping flow type to use-case: [flowchain: {}, flow: {}]: usecase: {}", getRootFlowChainType(), flowType, useCase);
            return Optional.of(useCase);
        }
        LOGGER.debug("flowType parameter is missing, no mapping is possible!");
        return Optional.empty();
    }

    @Override
    public Optional<UsageProto.CDPClusterStatus.Value> mapLastStepToUseCase(String flowType, String flowState) {
        if (StringUtils.isNotEmpty(flowState)) {
            UsageProto.CDPClusterStatus.Value useCase = lastFlowStateToUseCaseMap.getOrDefault(flowState, UsageProto.CDPClusterStatus.Value.UNSET);
            LOGGER.debug("Mapping last flow state to use-case: [flowchain: {}, flow:{}, flowstate: {}]: {}",
                    getRootFlowChainType(), flowType, flowState, useCase);
            return Optional.of(useCase);
        }
        LOGGER.debug("flowState parameter is missing, no mapping is possible!");
        return Optional.empty();
    }

    Map<String, UsageProto.CDPClusterStatus.Value> getFirstFlowConfigToUseCaseMap() {
        return firstFlowConfigToUseCaseMap;
    }

    Map<String, UsageProto.CDPClusterStatus.Value> getLastFlowStateToUseCaseMap() {
        return lastFlowStateToUseCaseMap;
    }
}
