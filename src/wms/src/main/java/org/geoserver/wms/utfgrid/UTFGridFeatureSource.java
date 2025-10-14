/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;

/**
 * A tiny wrapper that forces the attributes needed by UTFGrid
 *
 * @author Andrea Aime - GeoSolutions
 */
class UTFGridFeatureSource<T extends FeatureType, F extends Feature> extends DecoratingFeatureSource<T, F> {

    String[] propertyNames;

    public UTFGridFeatureSource(FeatureSource<T, F> delegate, String[] propertyNames) {
        super(delegate);
        this.propertyNames = propertyNames;
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        Query q = new Query(query);
        if (propertyNames == null || propertyNames.length == 0) {
            // no property selection, we return them all
            q.setProperties(Query.ALL_PROPERTIES);
        } else {
            // properties got selected, mix them with the ones needed by the renderer
            if (query.getPropertyNames() == null || query.getPropertyNames().length == 0) {
                q.setPropertyNames(propertyNames);
            } else {
                Set<String> names = new LinkedHashSet<>(Arrays.asList(propertyNames));
                names.addAll(Arrays.asList(q.getPropertyNames()));
                String[] newNames = names.toArray(new String[names.size()]);
                q.setPropertyNames(newNames);
            }
        }
        return super.getFeatures(q);
    }
}
