package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.teardown;

import com.sequenceiq.flow.core.FlowState;

public enum DatalakeRecoveryTeardownState implements FlowState {
    INIT_STATE,
    RECOVERY_TEARDOWN_STATE,
    RECOVERY_TEARDOWN_FAILED_STATE,
    RECOVERY_TEARDOWN_FINISHED_STATE,
    FINAL_STATE;
}
