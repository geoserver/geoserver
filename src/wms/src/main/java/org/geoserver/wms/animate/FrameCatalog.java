/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import java.util.Map;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;

/**
 * The Frame Catalog initializes the list of frames to be produced.<br>
 * The catalog takes care of splitting the "avalues" parameter and assigning values to each frame.
 * <br>
 * Notice that the catalog is not delegated to the frame production, it just handles the frames
 * metadata.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class FrameCatalog {

    private String parameter;

    private String[] values;

    private GetMapRequest getMapRequest;

    private WebMapService wms;

    private WMS wmsConfiguration;

    /** Default Constructor. */
    private FrameCatalog() {}

    /** Frame Catalog Constructor. */
    public FrameCatalog(GetMapRequest request, WebMapService wms, WMS wmsConfiguration) {
        this();

        this.getMapRequest = request;
        this.wms = wms;
        this.wmsConfiguration = wmsConfiguration;

        Map<String, String> rawKvp = request.getRawKvp();
        String aparam = KvpUtils.caseInsensitiveParam(rawKvp, "aparam", null);
        String avalues = KvpUtils.caseInsensitiveParam(rawKvp, "avalues", null);

        if (aparam != null && !aparam.isEmpty() && avalues != null && !avalues.isEmpty()) {
            this.parameter = aparam;
            this.values = avalues.split("(?<!\\\\)(,)");
        } else {
            dispose();
            throw new RuntimeException(
                    "Missing \"animator\" mandatory params \"aparam\" and \"avalues\".");
        }

        if (this.values.length > this.getWmsConfiguration().getMaxAllowedFrames()) {
            dispose();
            throw new RuntimeException(
                    "Request too long; reached the maximum allowed number of frames.");
        }
    }

    /** @return the parameter */
    public String getParameter() {
        return parameter;
    }

    /** @return the values */
    public String[] getValues() {
        return values;
    }

    /** @return the getMapRequest */
    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    /** @return the wms */
    public WebMapService getWms() {
        return wms;
    }

    /** @return the wmsConfiguration */
    public WMS getWmsConfiguration() {
        return wmsConfiguration;
    }

    /** Creates Frames visitors. Still not producing any image here. */
    void getFrames(FrameCatalogVisitor visitor) {
        for (String value : values) {
            visitor.visit(
                    this.getMapRequest, this.wms, this.wmsConfiguration, this.parameter, value);
        }
    }

    /** Dispose the catalog, removing all stored informations. */
    void dispose() {
        this.parameter = null;
        this.values = null;
        this.getMapRequest = null;
    }
}
