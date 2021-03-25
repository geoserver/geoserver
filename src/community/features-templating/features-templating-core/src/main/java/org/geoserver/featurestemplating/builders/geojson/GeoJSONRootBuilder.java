/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.geojson;

import static org.geoserver.featurestemplating.builders.impl.RootBuilder.VendorOption.*;

import java.util.Arrays;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;

/** GeoJson root builder used to support flat_output vendor parameter */
public class GeoJSONRootBuilder extends RootBuilder {

    public GeoJSONRootBuilder() {
        supportedOptions.addAll(
                Arrays.asList(FLAT_OUTPUT.getVendorOptionName(), SEPARATOR.getVendorOptionName()));
    }

    /**
     * Check if a builder tree needs to be reloaded by checking if the flat output the matching of
     * the vendor option flat_output value and the builder type. This due the fact that vendor
     * options can be provided with env function.
     *
     * @return true if reload is needed otherwise false.
     */
    @Override
    public boolean needsReload() {
        TemplateBuilder aChild = getChildren().get(0);
        boolean isCachedFlattened = aChild instanceof FlatBuilder;
        String strFlat = getVendorOption(FLAT_OUTPUT.getVendorOptionName());
        boolean isFlatOutput = strFlat != null ? Boolean.valueOf(strFlat) : false;
        if (isCachedFlattened && !isFlatOutput) return true;
        else if (!isCachedFlattened && isFlatOutput) return true;
        else return false;
    }
}
