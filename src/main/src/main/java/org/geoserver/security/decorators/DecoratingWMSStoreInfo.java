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
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate wms store info. Subclasses will override selected methods
 * to perform their "decoration" job
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DecoratingWMSStoreInfo extends AbstractDecorator<WMSStoreInfo>
        implements WMSStoreInfo {

    public DecoratingWMSStoreInfo(WMSStoreInfo delegate) {
        super(delegate);
    }

    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    public String getCapabilitiesURL() {
        return delegate.getCapabilitiesURL();
    }

    public Catalog getCatalog() {
        return delegate.getCatalog();
    }

    public Map<String, Serializable> getConnectionParameters() {
        return delegate.getConnectionParameters();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public Throwable getError() {
        return delegate.getError();
    }

    public String getId() {
        return delegate.getId();
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    public String getName() {
        return delegate.getName();
    }

    public String getType() {
        return delegate.getType();
    }

    public WebMapServer getWebMapServer(ProgressListener listener) throws IOException {
        return delegate.getWebMapServer(listener);
    }

    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public void setCapabilitiesURL(String url) {
        delegate.setCapabilitiesURL(url);
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void setError(Throwable t) {
        delegate.setError(t);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setType(String type) {
        delegate.setType(type);
    }

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

    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }

    public void setReadTimeout(int timeoutSeconds) {
        delegate.setReadTimeout(timeoutSeconds);
    }

    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    public void setConnectTimeout(int timeoutSeconds) {
        delegate.setConnectTimeout(timeoutSeconds);
    }

    public boolean isUseConnectionPooling() {
        return delegate.isUseConnectionPooling();
    }

    public void setUseConnectionPooling(boolean useHttpConnectionPooling) {
        delegate.setUseConnectionPooling(useHttpConnectionPooling);
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
}
