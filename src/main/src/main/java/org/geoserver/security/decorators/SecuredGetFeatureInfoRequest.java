/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.Response;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.request.GetFeatureInfoRequest;
import org.geotools.ows.wms.request.GetMapRequest;

/**
 * Wraps a GetFeatureInfo request enforcing GetFeatureInfo limits for each of the layers
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGetFeatureInfoRequest implements GetFeatureInfoRequest {

    List<Layer> queryLayers = new ArrayList<Layer>();
    GetFeatureInfoRequest delegate;
    int x;
    int y;
    GetMapRequest getMap;

    public SecuredGetFeatureInfoRequest(GetFeatureInfoRequest delegate, GetMapRequest getMap) {
        super();
        this.delegate = delegate;
        this.getMap = getMap;
    }

    public void addQueryLayer(Layer layer) {
        queryLayers.add(layer);
    }

    public void setQueryLayers(Set layers) {
        queryLayers.clear();
        queryLayers.addAll(layers);
    }

    public void setQueryPoint(int x, int y) {
        this.x = x;
        this.y = y;
        delegate.setQueryPoint(x, y);
    }

    public URL getFinalURL() {
        // scan and check the layers
        for (int i = 0; i < queryLayers.size(); i++) {
            Layer layer = queryLayers.get(i);
            if (layer instanceof SecuredWMSLayer) {
                SecuredWMSLayer secured = (SecuredWMSLayer) layer;
                final WrapperPolicy policy = secured.getPolicy();
                // check if we can cascade GetFeatureInfo
                if (policy.getLimits() instanceof WMSAccessLimits) {
                    WMSAccessLimits limits = (WMSAccessLimits) policy.getLimits();
                    if (!limits.isAllowFeatureInfo()) {
                        if (policy.getResponse() == org.geoserver.security.Response.CHALLENGE) {
                            SecureCatalogImpl.unauthorizedAccess(layer.getName());
                        } else {
                            throw new IllegalArgumentException(
                                    "Layer " + layer.getName() + " is not queriable");
                        }
                    }
                }

                // add into the request
                delegate.addQueryLayer(layer);
            }
        }

        // add the cql filters
        if (getMap instanceof SecuredGetMapRequest) {
            SecuredGetMapRequest sgm = (SecuredGetMapRequest) getMap;
            String encodedFilter = sgm.buildCQLFilter();
            if (encodedFilter != null) {
                delegate.setProperty("CQL_FILTER", encodedFilter);
            }
        }

        return delegate.getFinalURL();
    }

    // ----------------------------------------------------------------------------------------
    // Pure delegate methods
    // ----------------------------------------------------------------------------------------

    public Response createResponse(HTTPResponse response) throws ServiceException, IOException {
        return delegate.createResponse(response);
    }

    public String getPostContentType() {
        return delegate.getPostContentType();
    }

    public Properties getProperties() {
        return delegate.getProperties();
    }

    public void performPostOutput(OutputStream outputStream) throws IOException {
        delegate.performPostOutput(outputStream);
    }

    public boolean requiresPost() {
        return delegate.requiresPost();
    }

    public void setFeatureCount(int featureCount) {
        delegate.setFeatureCount(featureCount);
    }

    public void setFeatureCount(String featureCount) {
        delegate.setFeatureCount(featureCount);
    }

    public void setInfoFormat(String infoFormat) {
        delegate.setInfoFormat(infoFormat);
    }

    public void setProperty(String name, String value) {
        delegate.setProperty(name, value);
    }
}
