/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.util.ProgressListener;

/** Default implementation of {@link StoreInfo}. */
@SuppressWarnings("serial")
public abstract class StoreInfoImpl implements StoreInfo {

    protected String id;

    protected String name;

    protected String description;

    protected String type;

    protected boolean enabled;

    protected WorkspaceInfo workspace;

    protected transient Catalog catalog;

    protected Map<String, Serializable> connectionParameters = new HashMap<>();

    protected MetadataMap metadata = new MetadataMap();

    protected Throwable error;

    protected boolean _default;

    protected Date dateCreated;

    protected Date dateModified;

    protected String modifiedBy;

    protected boolean disableOnConnFailure;

    protected StoreInfoImpl() {}

    protected StoreInfoImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    protected StoreInfoImpl(Catalog catalog, String id) {
        this(catalog);
        setId(id);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public WorkspaceInfo getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    @Override
    public Map<String, Serializable> getConnectionParameters() {
        return connectionParameters;
    }

    public void setConnectionParameters(Map<String, Serializable> connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    @Override
    public synchronized MetadataMap getMetadata() {
        if (metadata == null) {
            metadata = new MetadataMap();
        }
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    @Override
    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        // subclasses should override
        return null;
    }

    public Iterator<?> getResources(ProgressListener monitor) throws IOException {
        // subclasses should override
        return null;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(name)
                .append(']')
                .toString();
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean isDefault() {
        return _default;
    }

    public void setDefault(boolean _default) {
        this._default = _default;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectionParameters == null) ? 0 : connectionParameters.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
        result = prime * result + (disableOnConnFailure ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof StoreInfo)) {
            return false;
        }

        final StoreInfo other = (StoreInfo) obj;
        if (connectionParameters == null) {
            if (other.getConnectionParameters() != null) return false;
        } else if (!connectionParameters.equals(other.getConnectionParameters())) return false;
        if (description == null) {
            if (other.getDescription() != null) return false;
        } else if (!description.equals(other.getDescription())) return false;
        if (enabled != other.isEnabled()) return false;
        if (id == null) {
            if (other.getId() != null) return false;
        } else if (!id.equals(other.getId())) return false;
        if (name == null) {
            if (other.getName() != null) return false;
        } else if (!name.equals(other.getName())) return false;
        if (workspace == null) {
            if (other.getWorkspace() != null) return false;
        } else if (!workspace.equals(other.getWorkspace())) return false;
        if (disableOnConnFailure != other.isDisableOnConnFailure()) return false;
        return true;
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

    @Override
    public boolean isDisableOnConnFailure() {
        return disableOnConnFailure;
    }

    @Override
    public void setDisableOnConnFailure(boolean disableOnConnFailure) {
        this.disableOnConnFailure = disableOnConnFailure;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String userName) {
        this.modifiedBy = userName;
    }
}
