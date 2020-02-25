/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.Response;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

/**
 * Wraps a GetMap request enforcing map limits for each of the layers
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGetMapRequest implements GetMapRequest {
    static final Logger LOGGER = Logging.getLogger(SecuredGetMapRequest.class);

    GetMapRequest delegate;
    List<Layer> layers = new ArrayList<Layer>();
    List<String> styles = new ArrayList<String>();

    // we should add layers to the delegate only once, also if
    // getFinalURL is called many times
    boolean layersAddedToDelegate = false;

    public SecuredGetMapRequest(GetMapRequest delegate) {
        this.delegate = delegate;
    }

    public void addLayer(Layer layer, String styleName) {
        layers.add(layer);
        if (styleName != null) {
            styles.add(styleName);
        } else {
            styles.add("");
        }
    }

    public void addLayer(Layer layer, StyleImpl style) {
        layers.add(layer);
        if (style != null && style.getName() != null) {
            styles.add(style.getName());
        } else {
            styles.add("");
        }
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
        styles.add("");
    }

    public void addLayer(String layerName, String styleName) {
        throw new UnsupportedOperationException(
                "The secured implementation only supports adding layers using Layer and StyleImpl objects");
    }

    public void addLayer(String layerName, StyleImpl style) {
        throw new UnsupportedOperationException(
                "The secured implementation only supports adding layers using Layer and StyleImpl objects");
    }

    public URL getFinalURL() {
        String encodedFilter = buildCQLFilter();
        if (encodedFilter != null) {
            delegate.setProperty("CQL_FILTER", encodedFilter);
        }

        return delegate.getFinalURL();
    }

    /** Checks security and build the eventual CQL filter to cascade */
    public String buildCQLFilter() {
        List<Filter> layerFilters = new ArrayList<Filter>();
        // scan and check the layers
        boolean layerFiltersFound = false;
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (layer instanceof SecuredWMSLayer) {
                SecuredWMSLayer secured = (SecuredWMSLayer) layer;
                final WrapperPolicy policy = secured.getPolicy();
                if (policy.getResponse() == org.geoserver.security.Response.CHALLENGE) {
                    SecureCatalogImpl.unauthorizedAccess(layer.getName());
                }
                // collect read filters
                if (policy.getLimits() instanceof WMSAccessLimits) {
                    WMSAccessLimits limits = (WMSAccessLimits) policy.getLimits();
                    layerFilters.add(limits.getReadFilter());
                    layerFiltersFound |= limits.getReadFilter() != null;

                    if (limits.getRasterFilter() != null) {
                        /*
                         * To implement this we'd have to change the code in
                         * SecuredWebMapServer.issueRequest(GetMapRequest) to parse the image,
                         * apply a crop, and encode it back.
                         * Also, if there are multiple layers in the request we'd have to group
                         * them by same raster filter, issue separate request in a format that
                         * supports transparency, crop, merge them back
                         */
                        LOGGER.severe(
                                "Sorry, raster filters for cascaded wms layers "
                                        + "have not been implemented yet");
                    }
                }

                if (!layersAddedToDelegate) {
                    // add into the request
                    delegate.addLayer(layer, styles.get(i));
                }
            }
        }

        // do we have filters? If so encode as cql hoping to find a GeoServer on the other side
        // TODO: handle eventual original CQL filters
        String encodedFilter = null;
        if (layerFiltersFound) {
            StringBuilder sb = new StringBuilder();
            for (Filter filter : layerFilters) {
                if (filter != null) {
                    sb.append(CQL.toCQL(filter));
                }
                sb.append(";");
            }
            // remove last ";"
            sb.setLength(sb.length() - 1);
            encodedFilter = ResponseUtils.urlEncode(sb.toString());
        }

        layersAddedToDelegate = true;

        return encodedFilter;
    }

    // -----------------------------------------------------------------------------------------
    // Purely delegated methods
    // -----------------------------------------------------------------------------------------

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

    public void setBBox(org.opengis.geometry.Envelope box) {
        delegate.setBBox(box);
    }

    public void setBBox(CRSEnvelope box) {
        delegate.setBBox(box);
    }

    public void setBBox(String bbox) {
        delegate.setBBox(bbox);
    }

    public void setBGColour(String bgColour) {
        delegate.setBGColour(bgColour);
    }

    public void setDimensions(Dimension imageDimension) {
        delegate.setDimensions(imageDimension);
    }

    public void setDimensions(int width, int height) {
        delegate.setDimensions(width, height);
    }

    public void setDimensions(String width, String height) {
        delegate.setDimensions(width, height);
    }

    public void setElevation(String elevation) {
        delegate.setElevation(elevation);
    }

    public void setExceptions(String exceptions) {
        delegate.setExceptions(exceptions);
    }

    public void setFormat(String format) {
        delegate.setFormat(format);
    }

    public void setProperties(Properties p) {
        delegate.setProperties(p);
    }

    public void setProperty(String name, String value) {
        delegate.setProperty(name, value);
    }

    public void setSampleDimensionValue(String name, String value) {
        delegate.setSampleDimensionValue(name, value);
    }

    public void setSRS(String srs) {
        delegate.setSRS(srs);
    }

    public void setTime(String time) {
        delegate.setTime(time);
    }

    public void setTransparent(boolean transparent) {
        delegate.setTransparent(transparent);
    }

    public void setVendorSpecificParameter(String name, String value) {
        delegate.setVendorSpecificParameter(name, value);
    }

    public void setVersion(String version) {
        delegate.setVersion(version);
    }

    public Response createResponse(HTTPResponse response) throws ServiceException, IOException {
        return delegate.createResponse(response);
    }
}
