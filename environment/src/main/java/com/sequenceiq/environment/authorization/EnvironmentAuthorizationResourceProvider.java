package com.sequenceiq.environment.authorization;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.AuthorizationResourceProvider;
import com.sequenceiq.authorization.service.list.ResourceWithParentResource;

@Component
public class EnvironmentAuthorizationResourceProvider implements AuthorizationResourceProvider {

    @Override
    public List<ResourceWithParentResource> findResources(String accountId, List<String> resourceCrns) {
        return resourceCrns.stream()
                .map(c -> new ResourceWithParentResource(c, Optional.empty()))
                .collect(Collectors.toList());
    }
}
