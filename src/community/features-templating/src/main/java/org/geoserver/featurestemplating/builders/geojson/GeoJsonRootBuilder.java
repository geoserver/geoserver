/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.geojson;

import static org.geoserver.featurestemplating.builders.impl.RootBuilder.VendorOption.*;

import java.util.Arrays;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;

/** GeoJson root builder used to support flat_output vendor parameter */
public class GeoJsonRootBuilder extends RootBuilder {

    public GeoJsonRootBuilder() {
        supportedOptions.addAll(
                Arrays.asList(FLAT_OUTPUT.getVendorOptionName(), SEPARATOR.getVendorOptionName()));
    }
}
