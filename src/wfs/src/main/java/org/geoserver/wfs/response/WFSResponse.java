/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.wfs.WFSInfo;

/**
 * Base class for WFS response objects.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class WFSResponse extends Response {

    protected GeoServer gs;

    public WFSResponse(GeoServer gs, Class binding) {
        super(binding);
        this.gs = gs;
    }

    public WFSResponse(GeoServer gs, Class binding, String outputFormat) {
        super(binding, outputFormat);
        this.gs = gs;
    }

    public WFSResponse(GeoServer gs, Class binding, Set<String> outputFormats) {
        super(binding, outputFormats);
        this.gs = gs;
    }

    protected WFSInfo getInfo() {
        WFSInfo wfs = gs.getService(WFSInfo.class);
        if (wfs == null) {
            throw new IllegalArgumentException("A valid WFS object must be provided");
        }

        return wfs;
    }
}
