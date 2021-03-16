package com.sequenceiq.flow.core.chain.finalize;

import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_FAILHANDLED_EVENT;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_FINALIZE_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlolwChainFinalizeFailedPayload;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;

@Configuration
public class FlowChainFinalizeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainFinalizeActions.class);

    @Bean(name = "FLOWCHAIN_FINALIZE_STATE")
    public Action<?, ?> finalizeAction() {
        return new AbstractFlowChainInitAction<>(FlowChainFinalizePayload.class) {
            @Override
            protected void doExecute(CommonContext context, FlowChainFinalizePayload payload, Map<Object, Object> variables) {
                LOGGER.info("Flow chain {} finalized", payload.getFlowChainName());
                sendEvent(context, FLOWCHAIN_FINALIZE_FINALIZE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "FLOWCHAIN_FINALIZE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractFlowChainInitAction<>(FlolwChainFinalizeFailedPayload.class) {
            @Override
            protected void doExecute(CommonContext context, FlolwChainFinalizeFailedPayload payload, Map<Object, Object> variables) {
                sendEvent(context, FLOWCHAIN_FINALIZE_FAILHANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractFlowChainInitAction<P extends FlowChainFinalizePayload>
            extends AbstractAction<FlowChainFinalizeState, FlowChainFinalizeEvent, CommonContext, P> {

        protected AbstractFlowChainInitAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<FlowChainFinalizeState, FlowChainFinalizeEvent> stateContext, P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return new FlolwChainFinalizeFailedPayload(payload.getFlowChainName(), payload.getResourceId(), ex);
        }
    }
}
