/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v2_0;

import java.util.Map;
import org.geotools.geometry.jts.CurvedGeometryFactory;
import org.geotools.gml3.v3_2.GMLConfiguration;
import org.geotools.wfs.v2_0.WFS;

/** Extend GeoTools WFSConfiguration to provide a custom binding class for GetFeatureType */
public class WFSConfiguration extends org.geotools.wfs.v2_0.WFSConfiguration {

    WFSConfiguration() {
        // OGC and OWS add two extra GML configurations in the mix, make sure to configure them
        // all with a geomtetry factory supporting curves
        CurvedGeometryFactory gf = new CurvedGeometryFactory(Double.MAX_VALUE);
        for (Object configuration : allDependencies()) {
            if (configuration instanceof GMLConfiguration) {
                GMLConfiguration gml = (GMLConfiguration) configuration;
                gml.setGeometryFactory(gf);
            }
        }
    }

    @Override
    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);
        bindings.put(WFS.GetFeatureType, org.geoserver.wfs.xml.v2_0.GetFeatureTypeBinding.class);
    }
}
