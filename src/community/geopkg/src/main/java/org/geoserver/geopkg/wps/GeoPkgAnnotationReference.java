/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

public class GeoPkgAnnotationReference {
    String tableName;
    String keyColumnName;
    Long keyValue;
    GeoPkgSemanticAnnotation annotation;

    public GeoPkgAnnotationReference(String tableName, GeoPkgSemanticAnnotation annotation) {
        this.tableName = tableName;
        this.annotation = annotation;
    }

    public GeoPkgAnnotationReference(
            String tableName,
            String keyColumnName,
            Long keyValue,
            GeoPkgSemanticAnnotation annotation) {
        this.tableName = tableName;
        this.keyColumnName = keyColumnName;
        this.keyValue = keyValue;
        this.annotation = annotation;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getKeyColumnName() {
        return keyColumnName;
    }

    public void setKeyColumnName(String keyColumnName) {
        this.keyColumnName = keyColumnName;
    }

    public Long getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(Long keyValue) {
        this.keyValue = keyValue;
    }

    public GeoPkgSemanticAnnotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(GeoPkgSemanticAnnotation annotation) {
        this.annotation = annotation;
    }

    @Override
    public String toString() {
        return "GeoPkgAnnotationReference{"
                + "tableName='"
                + tableName
                + '\''
                + ", keyColumnName='"
                + keyColumnName
                + '\''
                + ", keyValue="
                + keyValue
                + ", association="
                + annotation
                + '}';
    }
}
