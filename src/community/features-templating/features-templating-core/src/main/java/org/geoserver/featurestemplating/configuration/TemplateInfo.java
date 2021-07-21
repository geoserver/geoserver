/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * This class holds the template metadata, such as the name, the identifier, the template file
 * extension, workspace and featureType.
 */
public class TemplateInfo implements Serializable, Comparable<TemplateInfo> {

    private String identifier;

    private String description;

    protected String templateName;

    protected String workspace;

    protected String featureType;

    protected String extension;

    public TemplateInfo() {
        super();
        this.identifier = UUID.randomUUID().toString();
    }

    public TemplateInfo(
            String identifier,
            String templateName,
            String workspace,
            String featureType,
            String extension) {
        this.identifier = identifier;
        this.templateName = templateName;
        this.workspace = workspace;
        this.featureType = featureType;
        this.extension = extension;
    }

    public TemplateInfo(TemplateInfo info) {
        this(
                info.getIdentifier(),
                info.getTemplateName(),
                info.getWorkspace(),
                info.getFeatureType(),
                info.getExtension());
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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
     * Return the full name of the Template file. By full name is meant the templateName preceded by
     * the workspace name and featureTypeInfo name if defined for this instance.
     *
     * @return
     */
    public String getFullName() {
        String fullName = "";
        if (workspace != null) fullName += workspace + ":";
        if (featureType != null) fullName += featureType + ":";
        fullName += templateName;
        return fullName;
    }

    @Override
    public boolean equals(Object info) {
        if (!super.equals(info)) return false;
        if (!lenientEquals(info)) return false;
        TemplateInfo templateInfo = (TemplateInfo) info;
        return Objects.equals(identifier, templateInfo.identifier)
                && Objects.equals(description, templateInfo.description);
    }

    public boolean lenientEquals(Object o) {
        if (o == null) return false;
        TemplateInfo that = (TemplateInfo) o;
        return Objects.equals(templateName, that.templateName)
                && Objects.equals(workspace, that.workspace)
                && Objects.equals(featureType, that.featureType)
                && Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                identifier, templateName, description, workspace, featureType, extension);
    }

    @Override
    public int compareTo(TemplateInfo o) {
        return this.templateName.compareTo(o.getTemplateName());
    }
}
