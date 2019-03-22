/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geoserver.wms.describelayer.XMLDescribeLayerResponse;

/**
 * Holds the pre-validated parameters of a <code>DescribeLayer</code> request.
 *
 * <p>This pre-validation must to be done by the request reader, so the content of this object is
 * assumed to be valid.
 *
 * @author Gabriel Roldan
 * @author Carlo Cancellieri
 * @version $Id$
 */
public class DescribeLayerRequest extends WMSRequest {

    /** Holds the FeatureTypes parsed from the request's <code>LAYERS</code> parameter. */
    private List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>(2);

    /**
     * Holds the GetMap part of the GetFeatureInfo request, which is meant to provide enough context
     * information about the map over the DescribeLayer request is being made.
     */
    private GetMapRequest getMapRequest;

    /**
     * Holder for the optional <code>EXCEPTIONS</code> parameter, defaults to <code>
     * "application/vnd.ogc.se_xml"</code>
     */
    private static final String DEFAULT_EXCEPTION_FORMAT = "application/vnd.ogc.se_xml";

    private String exeptionFormat = DEFAULT_EXCEPTION_FORMAT;

    /** Holder for the <code>outputFormat</code> optional parameter */
    private String outputFormat = XMLDescribeLayerResponse.DESCLAYER_MIME_TYPE;

    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    public void setGetMapRequest(GetMapRequest getMapRequest) {
        this.getMapRequest = getMapRequest;
    }

    public String getExeptionFormat() {
        return exeptionFormat;
    }

    public void setExeptionFormat(String exeptionFormat) {
        this.exeptionFormat = exeptionFormat;
    }

    public DescribeLayerRequest() {
        super("DescribeLayer");
    }

    public void addLayer(MapLayerInfo layer) {
        if (layer == null) {
            throw new NullPointerException();
        }

        layers.add(layer);
    }

    public List<MapLayerInfo> getLayers() {
        return new ArrayList<MapLayerInfo>(layers);
    }

    public void setLayers(List<MapLayerInfo> layers) {
        this.layers = layers;
    }

    /** @return Returns the describeFormat. */
    public String getOutputFormat() {
        return outputFormat;
    }

    /** @param outputFormat The describeFormat to set. */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("DescribeLayerRequest[layers=");

        for (Iterator<MapLayerInfo> it = layers.iterator(); it.hasNext(); ) {
            sb.append(((MapLayerInfo) it.next()).getName());

            if (it.hasNext()) {
                sb.append(',');
            }
        }

        sb.append(']');

        return sb.toString();
    }
}
