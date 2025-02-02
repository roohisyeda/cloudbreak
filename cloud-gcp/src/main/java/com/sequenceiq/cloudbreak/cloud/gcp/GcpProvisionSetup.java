package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getBucket;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getImageName;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getMissingServiceAccountKeyError;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getTarName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Images.Get;
import com.google.api.services.compute.Compute.Images.Insert;
import com.google.api.services.compute.model.GuestOsFeature;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Image.RawDisk;
import com.google.api.services.compute.model.ImageList;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.Storage.Buckets;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpStorageFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;

@Service
public class GcpProvisionSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpProvisionSetup.class);

    private static final String READY = "READY";

    private static final int MAX_RECURSION_NUMBER = 100;

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStorageFactory gcpStorageFactory;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        try {
            String projectId = getProjectId(credential);
            String imageName = image.getImageName();
            Compute compute = gcpComputeFactory.buildCompute(credential);
            ImageList list = compute.images().list(projectId).execute();
            if (!containsSpecificImage(list, imageName)) {
                Storage storage = gcpStorageFactory.buildStorage(credential, cloudContext.getName());
                String accountId = authenticatedContext.getCloudContext().getAccountUUID();
                Bucket bucket = new Bucket();
                String bucketName = GcpLabelUtil.transformLabelKeyOrValue(String.format("%s-%s", accountId, projectId));
                bucket.setName(bucketName);
                bucket.setLocation(authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName());
                bucket.setStorageClass("STANDARD");
                try {
                    Buckets.Insert ins = storage.buckets().insert(projectId, bucket);
                    ins.execute();
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                        String msg = String.format("Failed to create bucket with name '%s':", bucketName);
                        LOGGER.warn(msg, ex);
                        throw ex;
                    } else {
                        LOGGER.info("No need to create bucket as it exists already with name: {}", bucketName);
                    }
                }
                String tarName = getTarName(imageName);
                rewriteUntilDone(getBucket(imageName), tarName, bucket.getName(), tarName, storage);

                Image gcpApiImage = new Image();
                String finalImageName = getImageName(imageName);
                gcpApiImage.setName(finalImageName);
                RawDisk rawDisk = new RawDisk();
                rawDisk.setSource(String.format("http://storage.googleapis.com/%s/%s", bucket.getName(), tarName));
                gcpApiImage.setRawDisk(rawDisk);
                GuestOsFeature uefiCompatible = new GuestOsFeature().setType("UEFI_COMPATIBLE");
                GuestOsFeature multiIpSubnet = new GuestOsFeature().setType("MULTI_IP_SUBNET");
                gcpApiImage.setGuestOsFeatures(List.of(uefiCompatible, multiIpSubnet));
                try {
                    Insert ins = compute.images().insert(projectId, gcpApiImage);
                    ins.execute();
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() != HttpStatus.SC_CONFLICT) {
                        String detailedMessage = ex.getDetails().getMessage();
                        String msg = String.format("Failed to create image with name '%s' in project '%s': %s", finalImageName, projectId, detailedMessage);
                        LOGGER.warn(msg, ex);
                        throw ex;
                    } else {
                        LOGGER.info("No need to create image as it exists already with name '{}' in project '{}':", finalImageName, projectId);
                    }
                }
            }
        } catch (Exception e) {
            Long stackId = cloudContext.getId();
            String msg = String.format("Error occurred on %s stack during the setup: %s", stackId, e.getMessage());
            LOGGER.warn(msg, e);
            throw new CloudConnectorException(msg, e);
        }
    }

    private void rewriteUntilDone(final String sourceBucket, final String sourceKey, final String destBucket,
        final String destKey, Storage storage) throws IOException {
        rewriteUntilDone(sourceBucket, sourceKey, destBucket, destKey, null, storage, 1);
    }

    private void rewriteUntilDone(final String sourceBucket, final String sourceKey, final String destBucket,
        final String destKey, final String rewriteToken, Storage storage, int recursionNumber) throws IOException {
        Storage.Objects.Rewrite rewrite = storage.objects().rewrite(sourceBucket, sourceKey, destBucket, destKey, new StorageObject());
        if (rewriteToken != null) {
            rewrite.setRewriteToken(rewriteToken);
        }
        RewriteResponse rewriteResponse = rewrite.execute();

        if (recursionNumber > MAX_RECURSION_NUMBER) {
            throw new CloudConnectorException(
                    String.format("Image copy from %s/%s to %s/%s reached the maximum number. Exiting the recursion.",
                            sourceBucket,
                            sourceKey,
                            destBucket,
                            destKey));
        }
        if (!rewriteResponse.getDone()) {
            String rewriteToken2 = rewriteResponse.getRewriteToken();
            Long totalBytesRewritten = rewriteResponse.getTotalBytesRewritten();
            LOGGER.debug("Rewriting not finished, bytes completed: {}. Calling rewrite again with token {}. Recursion reached the {}/100 attempt.",
                    totalBytesRewritten,
                    rewriteToken2,
                    recursionNumber);
            rewriteUntilDone(sourceBucket, sourceKey, destBucket, destKey, rewriteToken2, storage, ++recursionNumber);
        }
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        String projectId = getProjectId(credential);
        String imageName = image.getImageName();
        try {
            Image gcpApiImage = new Image();
            gcpApiImage.setName(getImageName(imageName));
            Compute compute = gcpComputeFactory.buildCompute(credential);
            Get getImages = compute.images().get(projectId, gcpApiImage.getName());
            String status = getImages.execute().getStatus();
            LOGGER.debug("Status of image {} copy: {}", gcpApiImage.getName(), status);
            if (READY.equals(status)) {
                return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
            }
        } catch (TokenResponseException e) {
            getMissingServiceAccountKeyError(e, projectId);
        } catch (IOException e) {
            LOGGER.info("Failed to retrieve image copy status", e);
            return new ImageStatusResult(ImageStatus.CREATE_FAILED, 0);
        }
        return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
    }

    @Override
    public void prerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) {
    }

    @Override
    public void validateParameters(AuthenticatedContext authenticatedContext, Map<String, String> parameters) {

    }

    @Override
    public void scalingPrerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, boolean upscale) {

    }

    private boolean containsSpecificImage(ImageList imageList, String imageUrl) {
        try {
            for (Image image : imageList.getItems()) {
                if (image.getName().equals(getImageName(imageUrl))) {
                    return true;
                }
            }
        } catch (NullPointerException ignored) {
            return false;
        }
        return false;
    }
}
