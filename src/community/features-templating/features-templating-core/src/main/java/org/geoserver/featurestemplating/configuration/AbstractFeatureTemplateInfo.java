/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.Serializable;
import java.util.Objects;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;

/**
 * Abstract class for a TemplateInfo. An AbstractTemplateInfo holds information about a template
 * file.
 */
public abstract class AbstractFeatureTemplateInfo
        implements Serializable, Comparable<AbstractFeatureTemplateInfo> {

    protected String templateName;

    protected String workspace;

    protected String featureType;

    protected String extension;

    public AbstractFeatureTemplateInfo() {}

    public AbstractFeatureTemplateInfo(
            String templateName, String workspace, String featureType, String extension) {
        this.templateName = templateName;
        this.workspace = workspace;
        this.featureType = featureType;
        this.extension = extension;
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

    @Override
    public int compareTo(AbstractFeatureTemplateInfo o) {
        return this.templateName.compareTo(o.getTemplateName());
    }

    public Resource getTemplateResource() {
        Resource resource = null;
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        GeoServerDataDirectory dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        if (featureType != null) {
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(featureType);
            resource = dd.get(fti, templateName + "." + extension);
        } else if (workspace != null) {
            WorkspaceInfo ws = catalog.getWorkspaceByName(workspace);
            resource = dd.get(ws, templateName + "." + extension);
        } else {
            resource = dd.get(TemplateInfoDaoImpl.TEMPLATE_DIR, templateName + "." + extension);
        }
        return resource;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return lenientEquals(o);
    }

    public boolean lenientEquals(Object o) {
        if (o == null) return false;
        AbstractFeatureTemplateInfo that = (AbstractFeatureTemplateInfo) o;
        return Objects.equals(templateName, that.templateName)
                && Objects.equals(workspace, that.workspace)
                && Objects.equals(featureType, that.featureType)
                && Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateName, workspace, featureType, extension);
    }
}
