/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.util.Map;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geotools.util.Version;

/**
 * This utility reads in a GetCapabilities KVP request and turns it into an appropriate internal
 * CapabilitiesRequest object, upon request.
 *
 * @author Rob Hranac, TOPP
 * @author Gabriel Roldan
 * @version $Id$
 */
public class CapabilitiesKvpReader extends KvpRequestReader {

    private WMS wms;

    public CapabilitiesKvpReader(WMS wms) {
        super(GetCapabilitiesRequest.class);
        this.wms = wms;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public GetCapabilitiesRequest read(Object req, Map kvp, Map rawKvp) throws Exception {
        GetCapabilitiesRequest request = (GetCapabilitiesRequest) super.read(req, kvp, rawKvp);
        request.setRawKvp(rawKvp);

        String version = request.getVersion();
        if (null == version || version.length() == 0) {
            version = (String) rawKvp.get("WMTVER");
        }

        // kind of a silly check but the cite tests put some rules about using WMTVER vs VERSION
        // depending on which one shows up as a kvp parameter first in order, which actualy
        // violates http get, but we do a check here to throw out one if it does not match
        // an available wms version
        if (rawKvp.containsKey("VERSION") && rawKvp.containsKey("WMTVER")) {
            String ver = (String) rawKvp.get("VERSION");
            String wmtver = (String) rawKvp.get("WMTVER");

            if (WMS.version(ver, true) != null && WMS.version(wmtver, true) == null) {
                version = ver;
            } else if (WMS.version(ver, true) == null && WMS.version(wmtver, true) != null) {
                version = wmtver;
            }
        }

        // version negotation
        Version requestedVersion = WMS.version(version);
        Version negotiatedVersion = wms.negotiateVersion(requestedVersion);
        request.setVersion(negotiatedVersion.toString());

        if (rawKvp.containsKey("ROOTLAYER")) {
            request.setRootLayerEnabled(Boolean.valueOf((String) rawKvp.get("ROOTLAYER")));
        }

        return request;
    }
}
