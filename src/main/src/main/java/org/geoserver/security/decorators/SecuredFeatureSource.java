/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

/**
 * Given a {@link FeatureSource} makes sure only the operations allowed by the WrapperPolicy can be
 * performed through it or using a object that can be accessed thru it. Depending on the challenge
 * policy, the object and the related ones will simply hide feature source abilities, or will throw
 * Spring security exceptions
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 * @param <F>
 */
public class SecuredFeatureSource<T extends FeatureType, F extends Feature>
        extends DecoratingFeatureSource<T, F> {

    static final Logger LOGGER = Logging.getLogger(SecuredFeatureSource.class);

    WrapperPolicy policy;

    protected SecuredFeatureSource(FeatureSource<T, F> delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    public DataAccess<T, F> getDataStore() {
        final DataAccess<T, F> store = delegate.getDataStore();
        if (store == null) return null;
        else return (DataAccess) SecuredObjects.secure(store, policy);
    }

    public FeatureCollection<T, F> getFeatures() throws IOException {
        final FeatureCollection<T, F> fc = delegate.getFeatures(getReadQuery());
        if (fc == null) return null;
        else return (FeatureCollection) SecuredObjects.secure(fc, policy);
    }

    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(null, filter));
    }

    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        // mix the external query with the access limits one
        final Query readQuery = getReadQuery();
        final Query mixed = mixQueries(query, readQuery);
        int limitedAttributeSize = mixed.getProperties() != null ? mixed.getProperties().size() : 0;
        final FeatureCollection<T, F> fc = delegate.getFeatures(mixed);
        if (fc == null) {
            return null;
        } else {
            if (limitedAttributeSize > 0
                    && fc.getSchema().getDescriptors().size() > limitedAttributeSize) {
                if (fc instanceof SimpleFeatureCollection) {
                    // the datastore did not honour the query properties?? It's broken, but we can
                    // fix it
                    SimpleFeatureCollection sfc = (SimpleFeatureCollection) fc;
                    SimpleFeatureType target =
                            SimpleFeatureTypeBuilder.retype(
                                    sfc.getSchema(), mixed.getPropertyNames());
                    ReTypingFeatureCollection retyped = new ReTypingFeatureCollection(sfc, target);
                    return (FeatureCollection) SecuredObjects.secure(retyped, policy);
                } else {
                    List<PropertyName> readProps = readQuery.getProperties();
                    List<PropertyName> queryProps = query.getProperties();
                    // logs only if properties have been limited by the security subsystem
                    if (readProps != null
                            && (queryProps == null || !readProps.containsAll(queryProps))) {
                        // complex feature store eh? No way to fix it at least warn the admin
                        LOGGER.log(
                                Level.SEVERE,
                                "Complex store returned more properties than allowed "
                                        + "by security (because they are required by the schema). "
                                        + "Either the security setup is broken or you have a security breach");
                    }
                    return (FeatureCollection) SecuredObjects.secure(fc, policy);
                }
            } else {
                return (FeatureCollection) SecuredObjects.secure(fc, policy);
            }
        }
    }

    protected Query getReadQuery() {
        if (policy.getAccessLevel() == AccessLevel.HIDDEN
                || policy.getAccessLevel() == AccessLevel.METADATA) {
            return new Query(null, Filter.EXCLUDE);
        } else if (policy.getLimits() == null) {
            return Query.ALL;
        } else if (policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits val = (VectorAccessLimits) policy.getLimits();

            // Ugly hack: during WFS transactions the reads we do are used to count the number of
            // features
            // we are deleting/updating: use the write filter instead of the read filter
            Request request = Dispatcher.REQUEST.get();
            if (request != null
                    && request.getService().equalsIgnoreCase("WFS")
                    && request.getRequest().equalsIgnoreCase("Transaction")) {
                return val.getWriteQuery();
            } else {
                return val.getReadQuery();
            }

        } else {
            throw new IllegalArgumentException(
                    "SecureFeatureSources has been fed "
                            + "with unexpected AccessLimits class "
                            + policy.getLimits().getClass());
        }
    }

    /**
     * Mixes two queries with an eye towards security (limiting attributes instead of adding them)
     * and preserves all of the other properties in userQuery (hints, crs handling, sorting)
     */
    protected Query mixQueries(Query userQuery, Query securityQuery) {
        // first rough mix
        Query result =
                new Query(
                        DataUtilities.mixQueries(userQuery, securityQuery, userQuery.getHandle()));

        // check request attributes and use those ones only
        List<PropertyName> securityProperties = securityQuery.getProperties();
        if (securityProperties != null && securityProperties.size() > 0) {
            List<PropertyName> userProperties = userQuery.getProperties();
            if (userProperties == null) {
                result.setProperties(securityProperties);
            } else {
                for (PropertyName pn : userProperties) {
                    if (!securityProperties.contains(pn)) {
                        throw new SecurityException(
                                "Attribute " + pn.getPropertyName() + " is not available");
                    }
                }
                result.setProperties(userProperties);
            }
        }

        // mix the hints, keep all the user ones and override with the query ones
        if (userQuery.getHints() == null) {
            result.setHints(securityQuery.getHints());
        } else if (securityQuery.getHints() == null) {
            result.setHints(userQuery.getHints());
        } else {
            Hints mix = userQuery.getHints();
            mix.putAll(securityQuery.getHints());
            result.setHints(mix);
        }

        // transfer all other properties from the user query
        result.setCoordinateSystem(userQuery.getCoordinateSystem());
        result.setCoordinateSystemReproject(userQuery.getCoordinateSystemReproject());
        result.setStartIndex(userQuery.getStartIndex());
        result.setSortBy(userQuery.getSortBy());

        return result;
    }
}
