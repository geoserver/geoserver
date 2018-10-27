/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.ows.wms.LayerDescription;
import org.geotools.util.logging.Logging;

/**
 * DescribeLayerModel the model object used to handle a DescribeLayer response
 *
 * @author carlo cancellieri - Geosolutions
 */
public class DescribeLayerModel {

    private static final Logger LOGGER = Logging.getLogger(DescribeLayerModel.class);

    private final List<LayerDescription> layerDescriptions = new ArrayList<LayerDescription>();

    private final String version;

    public DescribeLayerModel(final DescribeLayerRequest request) throws ServiceException {

        this.version = request.getVersion();

        final String baseURL = request.getBaseUrl();
        final List<MapLayerInfo> layersInfo = request.getLayers();
        for (MapLayerInfo layer : layersInfo) {

            String owsUrl = null;
            String owsType = null;
            URL owsURL = null;
            if (MapLayerInfo.TYPE_VECTOR == layer.getType()) {
                owsUrl = buildURL(baseURL, "wfs", null, URLType.SERVICE);
                owsUrl = appendQueryString(owsUrl, "");
                try {
                    owsURL = new URL(owsUrl);
                } catch (MalformedURLException e) {
                    LOGGER.warning(e.getLocalizedMessage());
                }
                owsType = "WFS";
            } else if (MapLayerInfo.TYPE_RASTER == layer.getType()) {
                owsUrl = buildURL(baseURL, "wcs", null, URLType.SERVICE);
                owsUrl = appendQueryString(owsUrl, "");
                try {
                    owsURL = new URL(owsUrl);
                } catch (MalformedURLException e) {
                    LOGGER.warning(e.getLocalizedMessage());
                }
                owsType = "WCS";
            } else {
                // non vector nor raster layer, LayerDescription will not contain these
                // attributes
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.warning(
                            "Non vector nor raster layer, LayerDescription will not contain these attributes");
            }

            final LayerDescription layerDesc = new LayerDescription();
            layerDesc.setName(layer.getName());
            layerDesc.setOwsType(owsType);
            layerDesc.setOwsURL(owsURL);

            // populate
            layerDescriptions.add(layerDesc);
        }
    }

    public List<LayerDescription> getLayerDescriptions() {
        return layerDescriptions;
    }

    public String getVersion() {
        return version;
    }
}
