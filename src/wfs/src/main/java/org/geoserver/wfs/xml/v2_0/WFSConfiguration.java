/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v2_0;

import java.util.Map;
import org.geotools.wfs.v2_0.WFS;

/**
 * Extend GeoTools WFSConfiguration to provide a custom binding class for GetFeatureType
 */
public class WFSConfiguration extends org.geotools.wfs.v2_0.WFSConfiguration {

    @Override
    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);
        bindings.put(WFS.GetFeatureType,org.geoserver.wfs.xml.v2_0.GetFeatureTypeBinding.class);
    }
}
