/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.util.Objects;
import java.util.UUID;

/**
 * This class holds the template metadata, such as the name, the identifier, the template file
 * extension, workspace and featureType.
 */
public class TemplateInfo extends AbstractFeatureTemplateInfo {

    private String identifier;

    private String description;

    public TemplateInfo() {
        super();
        this.identifier = UUID.randomUUID().toString();
    }

    public TemplateInfo(
            String templateName, String workspace, String featureType, String extension) {
        super(templateName, workspace, featureType, extension);
    }

    public TemplateInfo(AbstractFeatureTemplateInfo templateInfo) {
        this(
                templateInfo.templateName,
                templateInfo.workspace,
                templateInfo.featureType,
                templateInfo.extension);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public int hashCode() {
        return Objects.hash(super.hashCode(), identifier, description);
    }

    @Override
    public boolean equals(Object info) {
        if (!super.equals(info)) return false;
        if (!lenientEquals(info)) return false;
        TemplateInfo templateInfo = (TemplateInfo) info;
        return Objects.equals(identifier, templateInfo.identifier);
    }
}
