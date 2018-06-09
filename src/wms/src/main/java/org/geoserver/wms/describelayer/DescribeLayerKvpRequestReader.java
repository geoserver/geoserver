/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import java.util.List;
import java.util.Map;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.kvp.MapLayerInfoKvpParser;

/**
 * Parses a DescribeLayer request, wich consists only of a list of layer names, given by the <code>
 * "LAYER"</code> parameter.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerKvpRequestReader extends KvpRequestReader {

    private WMS wms;

    public DescribeLayerKvpRequestReader(WMS wms) {
        super(DescribeLayerRequest.class);
        this.wms = wms;
    }

    /**
     * @throws ServiceException if no layers has been requested, or one of the requested layers does
     *     not exists on this server instance, or the version parameter was not provided.
     * @see org.geoserver.ows.KvpRequestReader#read(java.lang.Object, java.util.Map, java.util.Map)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Object read(Object req, Map kvp, Map rawKvp) throws Exception {

        DescribeLayerRequest request = (DescribeLayerRequest) super.read(req, kvp, rawKvp);
        request.setRawKvp(rawKvp);

        final String version = request.getVersion();
        if (null == version) {
            String code = "NoVersionInfo";
            String simpleName = getClass().getSimpleName();
            throw new ServiceException(
                    "Version parameter not provided for DescribeLayer operation", code, simpleName);
        }

        if (!wms.getVersion().equals(version)) {
            throw new ServiceException(
                    "Wrong value for version parameter: "
                            + version
                            + ". This server accetps version "
                            + wms.getVersion(),
                    "InvalidVersion",
                    getClass().getSimpleName());
        }

        List<MapLayerInfo> layers =
                new MapLayerInfoKvpParser("LAYERS", wms).parse((String) rawKvp.get("LAYERS"));
        request.setLayers(layers);
        if (layers == null || layers.size() == 0) {
            throw new ServiceException(
                    "No LAYERS has been requested", "NoLayerRequested", getClass().getName());
        }
        return request;
    }
}
