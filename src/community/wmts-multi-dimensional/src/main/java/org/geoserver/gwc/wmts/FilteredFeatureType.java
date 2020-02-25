/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.view.DefaultView;
import org.geotools.feature.SchemaException;
import org.geotools.util.factory.Hints;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/** Utility class that returns a feature collection wrapped with a filter. */
public class FilteredFeatureType extends DecoratingFeatureTypeInfo {

    private final Filter filter;

    public FilteredFeatureType(FeatureTypeInfo info, Filter filter) {
        super(info);
        this.filter = filter;
    }

    @Override
    public FeatureSource getFeatureSource(ProgressListener listener, Hints hints)
            throws IOException {
        FeatureSource featureSource = super.getFeatureSource(listener, hints);
        if (!(featureSource instanceof SimpleFeatureSource)) {
            throw new IllegalStateException(
                    "Cannot apply dynamic dimension restrictions to complex features.");
        }
        SimpleFeatureSource simpleSource = (SimpleFeatureSource) featureSource;
        try {
            return new DefaultView(
                    simpleSource, new Query(simpleSource.getSchema().getTypeName(), filter));
        } catch (SchemaException exception) {
            throw new IOException("Failed to restrict the domain.", exception);
        }
    }
}
