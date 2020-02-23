/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory2;

/** Subclasses implementations allow to customize Feature values. */
abstract class FeatureCustomizer {

    protected static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    protected Logger LOGGER = Logging.getLogger(FeatureCustomizer.class);

    String typeName;

    public FeatureCustomizer(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * Customize the provided feature, looking for additional values to be retrieved from the
     * referred resource object.
     */
    abstract void customizeFeature(Feature feature, CatalogInfo resource);

    /** Map of all the registered feature customizers */
    static Map<String, FeatureCustomizer> CUSTOMIZERS;

    static Map<String, FeatureCustomizer> getCustomizers() {
        if (CUSTOMIZERS == null) {
            Map<String, FeatureCustomizer> result = new HashMap<>();
            List<FeatureCustomizer> customizers =
                    GeoServerExtensions.extensions(FeatureCustomizer.class);
            for (FeatureCustomizer customizer : customizers) {
                result.put(customizer.getTypeName(), customizer);
            }
            CUSTOMIZERS = result;
        }
        return CUSTOMIZERS;
    }

    /** Return a customizer instance for the specified typeName */
    public static FeatureCustomizer getCustomizer(String typeName) {
        getCustomizers();
        if (CUSTOMIZERS.containsKey(typeName)) {
            return CUSTOMIZERS.get(typeName);
        }
        return null;
    }
}
