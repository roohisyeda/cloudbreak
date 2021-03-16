package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

@ExtendWith(MockitoExtension.class)
public class UpgradeFlowUseCaseMapperTest {
    @InjectMocks
    UpgradeFlowUseCaseMapper underTest;

    @Test
    public void testMapFirstStepToUseCase() {
        // GIVEN
        Map<String, UsageProto.CDPClusterStatus.Value> useCaseMap = underTest.getFirstFlowConfigToUseCaseMap();
        // WHEN
        Map<String, UsageProto.CDPClusterStatus.Value> actualUseCaseMap = new HashMap();
        for (Map.Entry<String, UsageProto.CDPClusterStatus.Value> useCaseEntry : useCaseMap.entrySet()) {
            actualUseCaseMap.put(useCaseEntry.getKey(), underTest.mapFirstStepToUseCase(useCaseEntry.getKey()).get());
        }
        // THEN
        Assertions.assertEquals(useCaseMap.size(), actualUseCaseMap.size());
        for (Map.Entry<String, UsageProto.CDPClusterStatus.Value> actualUseCaseEntry : actualUseCaseMap.entrySet()) {
            Assertions.assertEquals(useCaseMap.get(actualUseCaseEntry.getKey()), actualUseCaseEntry.getValue());
        }
    }

    @Test
    public void testMapFirstStepToUseCaseIfNoMappingExists() {
        // GIVEN
        String notMappedFirstStep = "notMappedFirstStep";
        // WHEN
        Optional<UsageProto.CDPClusterStatus.Value> actualUseCase = underTest.mapFirstStepToUseCase(notMappedFirstStep);
        // THEN
        Assertions.assertTrue(actualUseCase.isPresent());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, actualUseCase.get());
    }

    @Test
    public void testMapFirstStepToUseCaseIfNull() {
        // GIVEN null
        // WHEN
        Optional<UsageProto.CDPClusterStatus.Value> actualUseCase = underTest.mapFirstStepToUseCase(null);
        // THEN
        Assertions.assertFalse(actualUseCase.isPresent());
    }

    @Test
    public void testMapLastStepToUseCase() {
        // GIVEN
        Map<String, UsageProto.CDPClusterStatus.Value> useCaseMap = underTest.getLastFlowStateToUseCaseMap();
        // WHEN
        Map<String, UsageProto.CDPClusterStatus.Value> actualUseCaseMap = new HashMap();
        for (Map.Entry<String, UsageProto.CDPClusterStatus.Value> useCaseEntry : useCaseMap.entrySet()) {
            actualUseCaseMap.put(useCaseEntry.getKey(), underTest.mapLastStepToUseCase("flowtype", useCaseEntry.getKey()).get());
        }
        // THEN
        Assertions.assertEquals(useCaseMap.size(), actualUseCaseMap.size());
        for (Map.Entry<String, UsageProto.CDPClusterStatus.Value> actualUseCaseEntry : actualUseCaseMap.entrySet()) {
            Assertions.assertEquals(useCaseMap.get(actualUseCaseEntry.getKey()), actualUseCaseEntry.getValue());
        }
    }

    @Test
    public void testMapLastStepToUseCaseIfNoMappingExists() {
        // GIVEN
        String notMappedLastStep = "notMappedLastStep";
        // WHEN
        Optional<UsageProto.CDPClusterStatus.Value> actualUseCase = underTest.mapLastStepToUseCase("flowType", notMappedLastStep);
        // THEN
        Assertions.assertTrue(actualUseCase.isPresent());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, actualUseCase.get());
    }

    @Test
    public void testMapLastStepToUseCaseIfNull() {
        // GIVEN null
        // WHEN
        Optional<UsageProto.CDPClusterStatus.Value> actualUseCase = underTest.mapLastStepToUseCase(null, null);
        // THEN
        Assertions.assertFalse(actualUseCase.isPresent());
    }
}
