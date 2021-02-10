package com.sequenceiq.freeipa.service;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UsedImagesProvider {

    @Inject
    private StackService stackService;

    public UsedImagesListV1Response getUsedImages() {
        final UsedImagesListV1Response usedImages = new UsedImagesListV1Response();

        final Set<Stack> stacks = stackService.getAllAliveWithImages();
        stacks.stream()
                .map(stack -> stack.getImage().getImageId())
                .forEach(usedImages::addImage);

        return usedImages;
    }
}
