package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class UsedImagesProvider {

    @Inject
    private StackService stackService;

    public UsedImagesListV4Response getUsedImages() {
        final UsedImagesListV4Response usedImages = new UsedImagesListV4Response();

        final Set<Stack> stacks = stackService.getAllAliveWithInstanceGroups();
        stacks.stream()
                .map(this::getImageForStack)
                .filter(Objects::nonNull)
                .forEach(usedImages::addImage);

        return usedImages;
    }

    private Image getImageForStack(Stack stack) {
        return stack.getInstanceGroupsAsList().stream()
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream())
                .map(instanceMetaData -> {
                    try {
                        return instanceMetaData.getImage().get(Image.class);
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
