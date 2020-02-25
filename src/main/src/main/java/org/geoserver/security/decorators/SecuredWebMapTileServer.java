/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesResponse;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.request.GetFeatureInfoRequest;
import org.geotools.ows.wms.response.GetFeatureInfoResponse;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.request.GetTileRequest;
import org.geotools.tile.Tile;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Applies security around the web map tile server.
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class SecuredWebMapTileServer extends WebMapTileServer {

    WebMapTileServer delegate;

    public SecuredWebMapTileServer(WebMapTileServer delegate) throws IOException, ServiceException {
        super(delegate.getCapabilities());
        this.delegate = delegate;
    }

    @Override
    public GetFeatureInfoRequest createGetFeatureInfoRequest(GetTileRequest getTileRequest) {
        return null;
    }

    @Override
    public GetTileRequest createGetTileRequest() {
        return delegate.createGetTileRequest();
    }

    // -------------------------------------------------------------------------------------------
    //
    // Purely delegated methods
    //
    // -------------------------------------------------------------------------------------------

    public GetCapabilitiesResponse issueRequest(GetCapabilitiesRequest request)
            throws IOException, ServiceException {
        if (delegate != null) {
            return delegate.issueRequest(request);
        } else {
            return null;
        }
    }

    @Override
    public GetFeatureInfoResponse issueRequest(GetFeatureInfoRequest request) {
        return delegate.issueRequest(request);
    }

    @Override
    public Set<Tile> issueRequest(GetTileRequest request) throws ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public WMTSCapabilities getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public GeneralEnvelope getEnvelope(Layer layer, CoordinateReferenceSystem crs) {
        return delegate.getEnvelope(layer, crs);
    }

    @Override
    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public ResourceInfo getInfo(Layer resource) {
        return delegate.getInfo(resource);
    }

    @Override
    public void setLoggingLevel(Level newLevel) {
        delegate.setLoggingLevel(newLevel);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return "SecuredWebMapTileServer " + delegate.toString();
    }
}
