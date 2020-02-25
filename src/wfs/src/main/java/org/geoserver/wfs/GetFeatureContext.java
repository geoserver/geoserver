/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/** {@link GetFeatureCallback} context object. */
public final class GetFeatureContext {

    private final GetFeatureRequest request;
    private FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
    private Query query;
    private final FeatureTypeInfo featureTypeInfo;

    /**
     * Builds the {@link GetFeatureCallback} context
     *
     * @param request The full GetFeature request
     * @param featureTypeInfo The feature type being queried
     * @param featureSource The feature source used for this query
     * @param query The query that will be run
     */
    GetFeatureContext(
            GetFeatureRequest request,
            FeatureTypeInfo featureTypeInfo,
            FeatureSource<? extends FeatureType, ? extends Feature> featureSource,
            Query query) {
        this.request = request;
        this.featureSource = featureSource;
        this.query = query;
        this.featureTypeInfo = featureTypeInfo;
    }

    /**
     * The full GetFeature request being run. The object returned may be mutable, but the callback
     * is strongly suggested not to attempt any modification, the behavior of doing so is undefined
     * and might change over time
     */
    public GetFeatureRequest getRequest() {
        return request;
    }

    /**
     * The feature type being queried. The object returned may be mutable, but the callback is
     * strongly suggested not to attempt any modification, the behavior of doing so is undefined and
     * might change over time
     */
    public FeatureTypeInfo getFeatureTypeInfo() {
        return featureTypeInfo;
    }

    /**
     * The feature source being queried. The object returned may be mutable, but the callback is
     * strongly suggested not to attempt any modification, the behavior of doing so is undefined and
     * might change over time
     */
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource() {
        return featureSource;
    }

    /** The query being run */
    public Query getQuery() {
        return query;
    }

    /** Allows to replace the query being run with another one */
    public void setQuery(Query query) {
        this.query = query;
    }
}
