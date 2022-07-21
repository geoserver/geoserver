/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate wmts store info.
 *
 * <p>Subclasses will override selected methods to perform their "decoration" job.
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class DecoratingWMTSStoreInfo extends AbstractDecorator<WMTSStoreInfo>
        implements WMTSStoreInfo {

    public DecoratingWMTSStoreInfo(WMTSStoreInfo delegate) {
        super(delegate);
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
    public String getCapabilitiesURL() {
        return delegate.getCapabilitiesURL();
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
    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public void setCapabilitiesURL(String url) {
        delegate.setCapabilitiesURL(url);
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
    public void setWorkspace(WorkspaceInfo workspace) {
        delegate.setWorkspace(workspace);
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public void setUsername(String user) {
        delegate.setUsername(user);
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public void setPassword(String password) {
        delegate.setPassword(password);
    }

    @Override
    public int getMaxConnections() {
        return delegate.getMaxConnections();
    }

    @Override
    public void setMaxConnections(int maxConcurrentConnections) {
        delegate.setMaxConnections(maxConcurrentConnections);
    }

    @Override
    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }

    @Override
    public void setReadTimeout(int timeoutSeconds) {
        delegate.setReadTimeout(timeoutSeconds);
    }

    @Override
    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    @Override
    public void setConnectTimeout(int timeoutSeconds) {
        delegate.setConnectTimeout(timeoutSeconds);
    }

    @Override
    public boolean isUseConnectionPooling() {
        return delegate.isUseConnectionPooling();
    }

    @Override
    public void setUseConnectionPooling(boolean useHttpConnectionPooling) {
        delegate.setUseConnectionPooling(useHttpConnectionPooling);
    }

    @Override
    public WebMapTileServer getWebMapTileServer(ProgressListener listener) throws IOException {

        return delegate.getWebMapTileServer(listener);
    }

    @Override
    public String getHeaderName() {
        return delegate.getHeaderName();
    }

    @Override
    public void setHeaderName(String headerName) {
        delegate.setHeaderName(headerName);
    }

    @Override
    public String getHeaderValue() {
        return delegate.getHeaderValue();
    }

    @Override
    public void setHeaderValue(String headerValue) {
        delegate.setHeaderValue(headerValue);
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
