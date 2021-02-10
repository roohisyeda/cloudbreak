package com.sequenceiq.freeipa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UsedImagesProviderTest {

    @Mock
    private StackService stackService;

    @InjectMocks
    private UsedImagesProvider underTest;

    @Test
    void testEmpty() {
        when(stackService.getAllAliveWithImages()).thenReturn(Set.of());

        final UsedImagesListV1Response result = underTest.getUsedImages();

        assertThat(result.getUsedImages()).isEmpty();
    }

    @Test
    void testSingleImage() {
        when(stackService.getAllAliveWithImages()).thenReturn(Set.of(
                createStack("aws-image")));

        final UsedImagesListV1Response result = underTest.getUsedImages();

        assertThat(result.getUsedImages())
                .hasSize(1);
        assertThat(result.getUsedImages().get(0).getNumberOfStacks())
                .isEqualTo(1);
    }

    @Test
    void testMultipleImages() {
        when(stackService.getAllAliveWithImages()).thenReturn(Set.of(
                createStack("aws-image"),
                createStack("aws-image"),
                createStack("azure-image")));

        final UsedImagesListV1Response result = underTest.getUsedImages();

        assertThat(result.getUsedImages())
                .hasSize(2)
                .anyMatch(usedImage -> usedImage.getImage().getImageId().equals("aws-image") && usedImage.getNumberOfStacks() == 2)
                .anyMatch(usedImage -> usedImage.getImage().getImageId().equals("azure-image") && usedImage.getNumberOfStacks() == 1);
    }

    private Stack createStack(String imageId) {
        final Stack stack = new Stack();
        final ImageEntity image = new ImageEntity();
        image.setImageId(imageId);
        stack.setImage(image);
        return stack;
    }

}
