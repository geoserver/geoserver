/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.security.WrapperPolicy;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Simple version of {@link SecuredFeatureCollection}
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SecuredSimpleFeatureCollection extends
        SecuredFeatureCollection<SimpleFeatureType, SimpleFeature> implements
        SimpleFeatureCollection {

    SecuredSimpleFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> delegate,
            WrapperPolicy policy) {
        super(delegate, policy);
    }

    public SimpleFeatureCollection sort(SortBy order) {
        return (SimpleFeatureCollection) super.sort(order);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        return (SimpleFeatureCollection) super.subCollection(filter);
    }
    
    @Override
    public SimpleFeatureIterator features() {
        return (SimpleFeatureIterator) super.features();
    }

}
