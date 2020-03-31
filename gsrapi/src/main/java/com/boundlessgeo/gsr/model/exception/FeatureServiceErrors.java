package com.boundlessgeo.gsr.model.exception;

import java.util.List;

/**
 * Service errors defined in https://developers.arcgis.com/rest/services-reference/feature-service-error-codes.htm
 */
public class FeatureServiceErrors {

    public static ServiceError nonSpecific(List<String> details) {
        return new ServiceError(1000, "Non-specific edit content error.", details);
    }
    public static ServiceError notNullable(String fieldName, List<String> details) {
        return new ServiceError(1001, "Field: "+ fieldName + "is not nullable. Cannot set null value.", details);
    }
    public static ServiceError noMValues(List<String> details) {
        return new ServiceError(1002, "Geometry does not have m-values", details);
    }
    public static ServiceError rolledBack1(List<String> details) {
        return new ServiceError(1003, "Operation rolled back.", details);
    }
    public static ServiceError geometryUpdateNotAllowed(List<String> details) {
        return new ServiceError(1004, "Geometry update not allowed.", details);
    }
    public static ServiceError trueCurveNotAllowed(List<String> details) {
        return new ServiceError(1005, "True curve update not allowed.", details);
    }
    public static ServiceError untrustedHtml(String fieldName, List<String> details) {
        return new ServiceError(1006, "Invalid untrusted Html content detected in field: "+fieldName+".", details);
    }
    public static ServiceError permissionDenied(List<String> details) {
        return new ServiceError(1007, "Edit denied due to ownership-based access control.", details);
    }
    public static ServiceError addAttachmentFailed(List<String> details) {
        return new ServiceError(1008, "Adding an attachment failed.", details);
    }
    public static ServiceError updateAttachmentFailed(List<String> details) {
        return new ServiceError(1009, "Updating an attachment failed.", details);
    }
    public static ServiceError deleteAttachmentFailed(List<String> details) {
        return new ServiceError(1010, "Deleting an attachment failed.", details);
    }
    public static ServiceError objectMissing(List<String> details) {
        return new ServiceError(1011, "Object is missing.", details);
    }
    public static ServiceError rolledBack2(List<String> details) {
        return new ServiceError(1012, "Operation rolled back.", details);
    }
    public static ServiceError objectMissingOrPermissionDenied(List<String> details) {
        return new ServiceError(1013, "Object is missing or edit denied due to ownership-based access control.", details);
    }
    public static ServiceError noZValue(List<String> details) {
        return new ServiceError(1014, "Geometry does not have z-values.", details);
    }
    public static ServiceError geometryNotSet(List<String> details) {
        return new ServiceError(1015, "Geometry could not be set.", details);
    }
    public static ServiceError setValueFailed(String fieldName, List<String> details) {
        return new ServiceError(1016, "Setting of value for "+fieldName+" failed.", details);
    }
    public static ServiceError insertError(List<String> details) {
        return new ServiceError(1017, "Internal error during object insert.", details);
    }
    public static ServiceError deleteError(List<String> details) {
        return new ServiceError(1018, "Internal error during object delete.", details);
    }
    public static ServiceError updateError(List<String> details) {
        return new ServiceError(1019, "Internal error during object update.", details);
    }
    public static ServiceError missingGlobalId(List<String> details) {
        return new ServiceError(1020, "Missing GlobalID.", details);
    }
    public static ServiceError emptyGlobalId(List<String> details) {
        return new ServiceError(1021, "Empty GlobalID.", details);
    }
    public static ServiceError uniqueConstraintViolation(List<String> details) {
        return new ServiceError(1022, "Unique constraint violation error on inserting the value.", details);
    }
    public static ServiceError notEditiable(String fieldName, List<String> details) {
        return new ServiceError(1023, "Field: "+fieldName+" is not editable.", details);
    }
    public static ServiceError editsRolledBack(String layerId, List<String> details) {
        return new ServiceError(2000, "Edits to layer "+layerId+" rolled back.", details);
    }
    public static ServiceError notFound(List<String> details) {
        return new ServiceError(2001, "Layer not found.", details);
    }
}
