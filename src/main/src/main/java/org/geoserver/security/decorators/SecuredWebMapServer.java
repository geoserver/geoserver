/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.logging.Level;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesResponse;
import org.geotools.geometry.GeneralBounds;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.request.DescribeLayerRequest;
import org.geotools.ows.wms.request.GetFeatureInfoRequest;
import org.geotools.ows.wms.request.GetLegendGraphicRequest;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.ows.wms.request.GetStylesRequest;
import org.geotools.ows.wms.request.PutStylesRequest;
import org.geotools.ows.wms.response.DescribeLayerResponse;
import org.geotools.ows.wms.response.GetFeatureInfoResponse;
import org.geotools.ows.wms.response.GetLegendGraphicResponse;
import org.geotools.ows.wms.response.GetMapResponse;
import org.geotools.ows.wms.response.GetStylesResponse;
import org.geotools.ows.wms.response.PutStylesResponse;

/**
 * Applies security around the web map server
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredWebMapServer extends WebMapServer {

    WebMapServer delegate;

    public SecuredWebMapServer(WebMapServer delegate) throws IOException, ServiceException {
        super(delegate.getCapabilities());
        this.delegate = delegate;
    }

    @Override
    public GetFeatureInfoRequest createGetFeatureInfoRequest(GetMapRequest getMapRequest) {
        return new SecuredGetFeatureInfoRequest(
                delegate.createGetFeatureInfoRequest(getMapRequest), getMapRequest);
    }

    @Override
    public GetMapRequest createGetMapRequest() {
        return new SecuredGetMapRequest(delegate.createGetMapRequest());
    }

    // -------------------------------------------------------------------------------------------
    //
    // Purely delegated methods
    //
    // -------------------------------------------------------------------------------------------

    @Override
    public GetStylesResponse issueRequest(GetStylesRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public PutStylesResponse issueRequest(PutStylesRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public GetLegendGraphicResponse issueRequest(GetLegendGraphicRequest request)
            throws IOException, ServiceException {

        return delegate.issueRequest(request);
    }

    @Override
    public DescribeLayerResponse issueRequest(DescribeLayerRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public GetCapabilitiesResponse issueRequest(GetCapabilitiesRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public GetFeatureInfoResponse issueRequest(GetFeatureInfoRequest request)
            throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public GetMapResponse issueRequest(GetMapRequest request) throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    @Override
    public DescribeLayerRequest createDescribeLayerRequest() throws UnsupportedOperationException {
        return delegate.createDescribeLayerRequest();
    }

    @Override
    public GetLegendGraphicRequest createGetLegendGraphicRequest()
            throws UnsupportedOperationException {
        return delegate.createGetLegendGraphicRequest();
    }

    @Override
    public GetStylesRequest createGetStylesRequest() throws UnsupportedOperationException {
        return delegate.createGetStylesRequest();
    }

    @Override
    public PutStylesRequest createPutStylesRequest() throws UnsupportedOperationException {
        return delegate.createPutStylesRequest();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public WMSCapabilities getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public GeneralBounds getEnvelope(Layer layer, CoordinateReferenceSystem crs) {
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
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void setLoggingLevel(Level newLevel) {
        delegate.setLoggingLevel(newLevel);
    }

    @Override
    public String toString() {
        return "SecuredWebMapServer " + delegate.toString();
    }
}
