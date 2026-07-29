/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.CQL2Conformance;
import org.geoserver.ogcapi.ECQLConformance;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wfs.WFSInfo;

public class FeaturesTestSupport extends OGCApiTestSupport {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wfs", "http://www.opengis.net/wfs/3.0");
        namespaces.put("sld", "http://www.opengis.net/sld");
    }

    /** A test body that may throw, used by {@link #withFilterLanguagesDisabled}. */
    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Runs the body with every filter language conformance class turned off, so no filter can be parsed, and always
     * resets the three flags to their default (null) afterwards.
     */
    protected void withFilterLanguagesDisabled(ThrowingRunnable body) throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        CQL2Conformance cql2 = CQL2Conformance.configuration(wfs);
        ECQLConformance ecql = ECQLConformance.configuration(wfs);
        cql2.setText(false);
        cql2.setJSON(false);
        ecql.setText(false);
        gs.save(wfs);
        try {
            body.run();
        } finally {
            cql2.setText(null);
            cql2.setJSON(null);
            ecql.setText(null);
            gs.save(wfs);
        }
    }
}
