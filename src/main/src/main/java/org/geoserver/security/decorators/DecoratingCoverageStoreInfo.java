/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.util.decorate.AbstractDecorator;
import org.geotools.util.factory.Hints;

/**
 * Delegates all methods to the provided delegate. Suclasses will override methods in order to
 * perform their decoration work
 *
 * @author Andrea Aime - TOPP
 * @param <T>
 * @param <F>
 */
public class DecoratingCoverageStoreInfo extends AbstractDecorator<CoverageStoreInfo>
        implements CoverageStoreInfo {

    public DecoratingCoverageStoreInfo(CoverageStoreInfo delegate) {
        super(delegate);
    }

    @Override
    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    @Override
    public Map<String, Serializable> getConnectionParameters() {
        return delegate.getConnectionParameters();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public Throwable getError() {
        return delegate.getError();
    }

    @Override
    public AbstractGridFormat getFormat() {
        return delegate.getFormat();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public String getURL() {
        return delegate.getURL();
    }

    @Override
    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public void setError(Throwable t) {
        delegate.setError(t);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setType(String type) {
        delegate.setType(type);
    }

    @Override
    public void setURL(String url) {
        delegate.setURL(url);
    }

    @Override
    public void setWorkspace(WorkspaceInfo workspace) {
        delegate.setWorkspace(workspace);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints)
            throws IOException {
        return delegate.getGridCoverageReader(listener, hints);
    }

    @Override
    public Date getDateModified() {
        return delegate.getDateModified();
    }

    @Override
    public Date getDateCreated() {
        return delegate.getDateCreated();
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        delegate.setDateCreated(dateCreated);
    }

    @Override
    public void setDateModified(Date dateModified) {
        delegate.setDateModified(dateModified);
    }

    @Override
    public boolean isDisableOnConnFailure() {
        return delegate.isDisableOnConnFailure();
    }

    @Override
    public void setDisableOnConnFailure(boolean disableOnConnFailure) {
        delegate.setDisableOnConnFailure(disableOnConnFailure);
    }
}
