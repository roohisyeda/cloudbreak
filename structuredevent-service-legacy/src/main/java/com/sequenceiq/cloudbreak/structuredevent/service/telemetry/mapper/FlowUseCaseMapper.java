package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import java.util.Optional;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

public interface FlowUseCaseMapper {
    String getRootFlowChainType();

    Optional<UsageProto.CDPClusterStatus.Value> mapFirstStepToUseCase(String flowType);

    Optional<UsageProto.CDPClusterStatus.Value> mapLastStepToUseCase(String flowType, String flowState);
}
