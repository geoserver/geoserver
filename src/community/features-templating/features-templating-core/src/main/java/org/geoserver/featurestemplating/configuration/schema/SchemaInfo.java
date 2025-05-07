/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * This class holds the template metadata, such as the name, the identifier, the template file extension, workspace and
 * featureType.
 */
public class SchemaInfo implements Serializable, Comparable<SchemaInfo> {

    private String identifier;

    private String description;

    protected String schemaName;

    protected String workspace;

    protected String featureType;

    protected String extension;

    public SchemaInfo() {
        super();
        this.identifier = UUID.randomUUID().toString();
    }

    public SchemaInfo(String identifier, String schemaName, String workspace, String featureType, String extension) {
        this.identifier = identifier;
        this.schemaName = schemaName;
        this.workspace = workspace;
        this.featureType = featureType;
        this.extension = extension;
    }

    public SchemaInfo(SchemaInfo info) {
        this(
                info.getIdentifier(),
                info.getSchemaName(),
                info.getWorkspace(),
                info.getFeatureType(),
                info.getExtension());
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Return the full name of the Template file. By full name is meant the templateName preceded by the workspace name
     * and featureTypeInfo name if defined for this instance.
     *
     * @return
     */
    public String getFullName() {
        String fullName = "";
        if (workspace != null) fullName += workspace + ":";
        if (featureType != null) fullName += featureType + ":";
        fullName += schemaName;
        return fullName;
    }

    @Override
    public boolean equals(Object info) {
        if (!super.equals(info)) return false;
        if (!lenientEquals(info)) return false;
        SchemaInfo templateInfo = (SchemaInfo) info;
        return Objects.equals(identifier, templateInfo.identifier)
                && Objects.equals(description, templateInfo.description);
    }

    public boolean lenientEquals(Object o) {
        if (o == null) return false;
        SchemaInfo that = (SchemaInfo) o;
        return Objects.equals(schemaName, that.schemaName)
                && Objects.equals(workspace, that.workspace)
                && Objects.equals(featureType, that.featureType)
                && Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, schemaName, description, workspace, featureType, extension);
    }

    @Override
    public int compareTo(SchemaInfo o) {
        return this.schemaName.compareTo(o.getSchemaName());
    }
}
