package com.sequenceiq.it.cloudbreak.actor;

public interface Actor {

    CloudbreakUser defaultUser();

    CloudbreakUser secondUser();

    CloudbreakUser create(String tenantName, String username);

    CloudbreakUser createInternal(String tenantName);

    CloudbreakUser useRealUmsUser(String key);
}