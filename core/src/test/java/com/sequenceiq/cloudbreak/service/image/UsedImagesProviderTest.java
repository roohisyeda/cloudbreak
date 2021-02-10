package com.sequenceiq.cloudbreak.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UsedImagesProviderTest {

    @Mock
    private StackService stackService;

    @InjectMocks
    private UsedImagesProvider underTest;

    @Test
    void testEmpty() {
        when(stackService.getAllAliveWithInstanceGroups()).thenReturn(Set.of());

        final UsedImagesListV4Response result = underTest.getUsedImages();

        assertThat(result.getUsedImages()).isEmpty();
    }

    @Test
    void testSingleImage() {
        when(stackService.getAllAliveWithInstanceGroups()).thenReturn(Set.of(
                createStack(createImage("aws-image"))));

        final UsedImagesListV4Response result = underTest.getUsedImages();

        assertThat(result.getUsedImages())
                .hasSize(1)
                .first()
                .matches(usedImage -> usedImage.getNumberOfStacks() == 1);
    }

    @Test
    void testMultipleImages() {
        when(stackService.getAllAliveWithInstanceGroups()).thenReturn(Set.of(
                createStack(createImage("aws-image")),
                createStack(createImage("aws-image")),
                createStack(createImage("azure-image"))));

        final UsedImagesListV4Response result = underTest.getUsedImages();

        assertThat(result.getUsedImages())
                .hasSize(2)
                .anyMatch(usedImage -> usedImage.getImage().getImageId().equals("aws-image") && usedImage.getNumberOfStacks() == 2)
                .anyMatch(usedImage -> usedImage.getImage().getImageId().equals("azure-image") && usedImage.getNumberOfStacks() == 1);
    }

    private Stack createStack(Image image) {
        final Stack stack = new Stack();

        final InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setImage(new Json(image));

        final InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));

        return stack;
    }

    private Image createImage(String imageId) {
        return new Image("imageName", Map.of(), "os", "osType", "imageCatalogUrl", "imageCatalogName", imageId, null);
    }

}
