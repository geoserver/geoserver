/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders.geojson;

import static org.geoserver.wfstemplating.builders.impl.RootBuilder.VendorOption.FLAT_OUTPUT;

import org.geoserver.wfstemplating.builders.impl.RootBuilder;

/** GeoJson root builder used to support flat_output vendor parameter */
public class GeoJsonRootBuilder extends RootBuilder {

    public GeoJsonRootBuilder() {
        supportedOptions.add(FLAT_OUTPUT.getVendorOptionName());
    }
}
