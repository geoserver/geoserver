/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.io.Serial;
import java.util.Objects;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Geoserver wrapper for a complex features feature source.
 *
 * <p>Handles the final query build taking into account the definition query from the FeatureTypeInfo if exists.
 *
 * @author Fernando Miño - Geosolutions
 */
public class GeoserverComplexFeatureSource extends DecoratingFeatureSource<FeatureType, Feature> {
    @Serial
    private static final long serialVersionUID = 1L;

    protected static final FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);
    /** provided FeatureTypeInfo for getting the declared query and more */
    private final FeatureTypeInfo ftypeInfo;

    public GeoserverComplexFeatureSource(FeatureSource<FeatureType, Feature> delegate, FeatureTypeInfo ftypeInfo)
            throws DataSourceException {
        super(delegate);
        this.ftypeInfo = Objects.requireNonNull(ftypeInfo);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Filter filter) throws IOException {
        filter = buildFilter(filter);
        return delegate.getFeatures(filter);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Query query) throws IOException {
        query = buildQuery(query);
        return delegate.getFeatures(query);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures() throws IOException {
        return delegate.getFeatures(getDefaultQuery());
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return delegate.getBounds(getDefaultQuery());
    }

    /** Builds and return the default Query for this layer's featureType when no request query is provided. */
    protected Query getDefaultQuery() throws DataSourceException {
        return new Query(ftypeInfo.getQualifiedNativeName().getLocalPart(), buildFilter(Filter.INCLUDE));
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        query = buildQuery(query);
        return delegate.getBounds(query);
    }

    @Override
    public int getCount(Query query) throws IOException {
        query = buildQuery(query);
        return delegate.getCount(query);
    }

    /**
     * Builds the final query mixing the request query with the layer default configured query (if exists) with a
     * conjunction (and operator).
     *
     * @param query the requested query.
     * @return the final mixed query.
     */
    protected Query buildQuery(Query query) throws DataSourceException {
        Filter filter = buildFilter(query.getFilter());
        Query newQuery = new Query(query);
        newQuery.setFilter(filter);
        return newQuery;
    }

    /**
     * Builds the final filter mixing the request filter with the layer default configured filter (if exists) with a
     * conjunction (and operator).
     *
     * @param filter the requested filter.
     * @return the final mixed filter.
     */
    private Filter buildFilter(Filter filter) throws DataSourceException {
        filter = nullSafeCheck(filter);
        Filter newFilter = filter;
        try {
            Filter definitionQuery = nullSafeCheck(ftypeInfo.filter());
            if (definitionQuery == Filter.INCLUDE) return filter;
            SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
            Filter simplifiedDefinitionQuery = (Filter) definitionQuery.accept(visitor, null);
            if (filter == Filter.INCLUDE) {
                newFilter = simplifiedDefinitionQuery;
            } else if (simplifiedDefinitionQuery != Filter.INCLUDE) {
                newFilter = FF.and(simplifiedDefinitionQuery, filter);
            }
        } catch (Exception ex) {
            throw new DataSourceException("Can't create the definition filter", ex);
        }

        return newFilter;
    }

    private Filter nullSafeCheck(Filter filter) {
        if (filter == null) return Filter.INCLUDE;
        return filter;
    }

    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        return iface != null && iface.isAssignableFrom(this.getClass());
    }

    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        // TODO Auto-generated method stub
        try {
            if (iface != null && iface.isAssignableFrom(this.getClass())) {
                return (T) this;
            }
            throw new java.sql.SQLException("Auto-generated unwrap failed; Revisit implementation");
        } catch (Exception e) {
            throw new java.sql.SQLException(e);
        }
    }
}
