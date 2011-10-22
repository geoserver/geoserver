/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.security;

import java.io.IOException;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredFeatureSource;
import org.geotools.data.FeatureDiffReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Read only wrapper around a {@link VersioningFeatureSource}
 * 
 * @author Andrea Aime - TOPP
 */
public class ReadOnlyVersioningFeatureSource extends SecuredFeatureSource<SimpleFeatureType, SimpleFeature> implements VersioningFeatureSource {

    ReadOnlyVersioningFeatureSource(FeatureSource delegate, WrapperPolicy policy) {
        super(delegate, policy);
    }

    public FeatureDiffReader getDifferences(String fromVersion, String toVersion, Filter filter,
            String[] userIds) throws IOException {
        return ((VersioningFeatureSource) delegate).getDifferences(fromVersion, toVersion, filter,
                userIds);
    }

    public SimpleFeatureCollection getLog(String fromVersion,
            String toVersion, Filter filter, String[] userIds, int maxRows) throws IOException {
        return ((VersioningFeatureSource) delegate).getLog(fromVersion, toVersion, filter, userIds,
                maxRows);
    }

    public SimpleFeatureCollection getVersionedFeatures()
            throws IOException {
        return ((VersioningFeatureSource) delegate).getVersionedFeatures();
    }

    public SimpleFeatureCollection getVersionedFeatures(Query q)
            throws IOException {
        return ((VersioningFeatureSource) delegate).getVersionedFeatures(q);
    }

    public SimpleFeatureCollection getVersionedFeatures(Filter f)
            throws IOException {
        return ((VersioningFeatureSource) delegate).getVersionedFeatures(f);
    }
    
    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return ((VersioningFeatureSource) delegate).getFeatures();
    }
    
    @Override
    public SimpleFeatureCollection getFeatures(Filter filter)
            throws IOException {
        return ((VersioningFeatureSource) delegate).getFeatures(filter);
    }
    
    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        return ((VersioningFeatureSource) delegate).getFeatures(query);
    }


}
