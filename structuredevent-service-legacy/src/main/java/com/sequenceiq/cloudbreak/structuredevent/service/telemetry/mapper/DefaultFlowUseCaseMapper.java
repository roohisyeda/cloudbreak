package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

@Component
public class DefaultFlowUseCaseMapper implements FlowUseCaseMapper {
    @Override
    public String getRootFlowChainType() {
        return "";
    }

    @Override
    public Optional<UsageProto.CDPClusterStatus.Value> mapFirstStepToUseCase(String flowType) {
        return Optional.empty();
    }

    @Override
    public Optional<UsageProto.CDPClusterStatus.Value> mapLastStepToUseCase(String flowType, String flowState) {
        return Optional.empty();
    }
}
