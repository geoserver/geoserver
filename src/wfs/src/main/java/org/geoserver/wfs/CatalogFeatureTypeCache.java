/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Custom FeatureTypeCache that looks up directly from GeoServer catalog.
 *
 * <p>This cache class is used by XML bindings during parsing to obtain feature type information.
 * This custom implementation allows for look up on demand, as opposed to pre-seeding the cache.
 */
public class CatalogFeatureTypeCache extends FeatureTypeCache {

    static final Logger LOGGER = Logging.getLogger(CatalogFeatureTypeCache.class);

    Catalog catalog;

    public CatalogFeatureTypeCache(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public FeatureType get(Name name) {
        // first look up into the parent cache
        FeatureType featureType = super.get(name);
        if (featureType == null) {
            // look up in catalog
            FeatureTypeInfo meta = catalog.getFeatureTypeByName(name);
            if (meta != null) {
                try {
                    featureType = meta.getFeatureType();

                    // throw into the cache
                    put(featureType);
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Could not load underlying feature type for type " + meta.getName(),
                            e);
                }
            }
        }
        return featureType;
    }
}
