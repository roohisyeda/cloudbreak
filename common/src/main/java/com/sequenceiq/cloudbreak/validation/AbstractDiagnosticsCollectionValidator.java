package com.sequenceiq.cloudbreak.validation;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;

public abstract class AbstractDiagnosticsCollectionValidator<TELEMETRY, STATUS> {

    private final SupportBundleConfiguration supportBundleConfiguration;

    public AbstractDiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        this.supportBundleConfiguration = supportBundleConfiguration;
    }

    public SupportBundleConfiguration getSupportBundleConfiguration() {
        return supportBundleConfiguration;
    }

    public void validate(TELEMETRY telemetry, DiagnosticsDestination destination, STATUS stackStatus,
            String stackName, String version) {
        validate(telemetry, destination, stackStatus, stackName, version, false);
    }

    public void validate(TELEMETRY telemetry, DiagnosticsDestination destination, STATUS stackStatus,
            String stackName, String version, boolean cmBundle) {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        validateStackStatus(stackStatus, stackName);
        MinAppVersionChecker versionChecker = getMinAppVersionChecker();
        if (versionChecker != null && !versionChecker.isAppVersionValid(version)) {
            validationBuilder.error(String.format("Required %s min major/minor version is %d.%d for using diagnostics. Try it on newer environment.",
                    getStackType(), versionChecker.getMinMajorVersion(), versionChecker.getMinMinorVersion()));
        } else if (telemetry == null) {
            validationBuilder.error(String.format("Telemetry is not enabled for %s (name: '%s')", getStackType(),  stackName));
        } else if (DiagnosticsDestination.CLOUD_STORAGE.equals(destination)) {
            validateCloudStorageSettings(telemetry, stackName, validationBuilder);
        } else if (DiagnosticsDestination.ENG.equals(destination) && cmBundle) {
            validationBuilder.error("Cluster log collection with ENG destination is not supported for CM based diagnostics");
        } else if (DiagnosticsDestination.ENG.equals(destination) && isClusterLogCollectionDisabled(telemetry)) {
            validationBuilder.error(
                    String.format("Cluster log collection is not enabled for %s (name: '%s')", getStackType(), stackName));
        } else if (DiagnosticsDestination.SUPPORT.equals(destination) && !isSupportBundleEnabled(cmBundle)) {
            validationBuilder.error(
                    String.format("Destination %s is not supported yet.", DiagnosticsDestination.SUPPORT.name()));
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    protected abstract void validateStackStatus(STATUS stackStatus, String stackName);

    protected abstract boolean isSupportBundleEnabled(boolean cmBundle);

    protected abstract boolean isClusterLogCollectionDisabled(TELEMETRY telemetry);

    protected abstract void validateCloudStorageSettings(TELEMETRY telemetry, String stackName,
            ValidationResult.ValidationResultBuilder validationBuilder);

    protected abstract String getStackType();

    protected abstract MinAppVersionChecker getMinAppVersionChecker();
}
