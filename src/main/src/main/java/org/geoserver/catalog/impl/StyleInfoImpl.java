/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import java.util.Date;
import org.geoserver.catalog.*;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;

public class StyleInfoImpl implements StyleInfo {

    protected String id;

    protected String name;

    protected WorkspaceInfo workspace;

    // not used, maininting this property for xstream backward compatability
    protected Version sldVersion = null;

    protected String format = SLDHandler.FORMAT;

    protected Version languageVersion = SLDHandler.VERSION_10;

    protected String filename;

    protected LegendInfo legend;

    protected transient Catalog catalog;

    protected MetadataMap metadata = new MetadataMap();

    protected Date dateCreated;

    protected Date dateModified;

    protected StyleInfoImpl() {}

    public StyleInfoImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkspaceInfo getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String language) {
        this.format = language;
    };

    public Version getFormatVersion() {
        return languageVersion;
    }

    public void setFormatVersion(Version version) {
        this.languageVersion = version;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Style getStyle() throws IOException {
        // for capability document request
        // remote style does not exist in local catalog
        // do not look for this style inside ResourcePool
        if (metadata != null)
            if (metadata.containsKey("isRemote")) return WMSLayerInfoImpl.getStyleInfo(this);
        return catalog.getResourcePool().getStyle(this);
    }

    public StyledLayerDescriptor getSLD() throws IOException {
        return catalog.getResourcePool().getSld(this);
    }

    public LegendInfo getLegend() {
        return legend;
    }

    public void setLegend(LegendInfo legend) {
        this.legend = legend;
    }

    @Override
    public MetadataMap getMetadata() {
        // non nullable
        checkMetadataNotNull();
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    private void checkMetadataNotNull() {
        if (metadata == null) metadata = new MetadataMap();
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((filename == null) ? 0 : filename.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((languageVersion == null) ? 0 : languageVersion.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof StyleInfo)) return false;
        final StyleInfo other = (StyleInfo) obj;
        if (filename == null) {
            if (other.getFilename() != null) return false;
        } else if (!filename.equals(other.getFilename())) return false;
        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;
        if (name == null) {
            if (other.getName() != null) return false;
        } else if (!name.equals(other.getName())) return false;
        if (workspace == null) {
            if (other.getWorkspace() != null) return false;
        } else if (!workspace.equals(other.getWorkspace())) return false;
        if (format == null) {
            if (other.getFormat() != null) return false;
        } else {
            if (!format.equals(other.getFormat())) return false;
        }
        if (languageVersion == null) {
            if (other.getFormatVersion() != null) return false;
        } else if (!languageVersion.equals(other.getFormatVersion())) return false;

        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(prefixedName())
                .append(']')
                .toString();
    }

    @Override
    public String prefixedName() {
        if (workspace != null) {
            return workspace.getName() + ":" + getName();
        } else {
            return getName();
        }
    }

    protected Object readResolve() {
        // this check is here to enable smooth migration from old configurations that don't have
        // the version property, and a transition from the deprecated sldVersion property

        if (format == null) {
            format = SLDHandler.FORMAT;
        }

        if (languageVersion == null && sldVersion != null) {
            languageVersion = sldVersion;
        }
        if (languageVersion == null) {
            languageVersion = SLDHandler.VERSION_10;
        }

        return this;
    }

    @Override
    public Date getDateModified() {
        return this.dateModified;
    }

    @Override
    public Date getDateCreated() {
        return this.dateCreated;
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }
}
