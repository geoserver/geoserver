/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.kvp;

import java.util.Map;
import net.opengis.ows11.AcceptVersionsType;
import net.opengis.wps10.GetCapabilitiesType;
import org.geoserver.ows.kvp.OWS11AcceptVersionsKvpParser;

/**
 * GetCapabilities KVP request reader
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Andrea Aime, OpenGeo
 */
public class GetCapabilitiesKvpRequestReader extends WPSKvpRequestReader {
    public GetCapabilitiesKvpRequestReader() {
        super(GetCapabilitiesType.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // make sure we get the right accepts versions param -> workaround for GEOS-1719
        if (rawKvp.containsKey("acceptVersions")) {
            OWS11AcceptVersionsKvpParser avp = new OWS11AcceptVersionsKvpParser();
            AcceptVersionsType avt =
                    (AcceptVersionsType) avp.parse((String) rawKvp.get("acceptVersions"));
            kvp.put("acceptVersions", avt);
        }
        request = super.read(request, kvp, rawKvp);

        return request;
    }
}
