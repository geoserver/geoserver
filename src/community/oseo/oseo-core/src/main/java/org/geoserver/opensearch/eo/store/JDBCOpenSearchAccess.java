/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.List;

import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.Repository;
import org.geotools.data.ServiceInfo;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * A data store building OpenSearch for EO records based on a wrapped data store providing all expected tables in form of simple features (and
 * leveraging joins to put them together into complex features as needed).
 * 
 * The delegate store is fetched on demand to avoid being caught in a ResourcePool dispose
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCOpenSearchAccess implements OpenSearchAccess {

    Repository repository;

    Name delegateStoreName;

    public JDBCOpenSearchAccess(Repository repository, Name delegateStoreName) {
        // TODO: maybe get a direct Catalog reference so that we can lookup by store id, which is
        // stable though renames?
        this.repository = repository;
        this.delegateStoreName = delegateStoreName;
    }

    @Override
    public ServiceInfo getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSchema(FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Name> getNames() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureType getSchema(Name name) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureSource<FeatureType, Feature> getFeatureSource(Name typeName) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

}
