/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Set;
import org.geoserver.catalog.Predicates;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * {@link GranuleSource} wrapper ANDing a security read filter into every query, so granule enumeration honors the same
 * access limits the coverage read path applies.
 */
class SecuredGranuleSource implements GranuleSource {

    protected final GranuleSource delegate;

    protected final Filter readFilter;

    /**
     * @param readFilter the access limits filter, not null, {@link Filter#INCLUDE} when the wrapper is only there to
     *     keep a {@link org.geotools.coverage.grid.io.GranuleStore} delegate from leaking its write methods
     */
    SecuredGranuleSource(GranuleSource delegate, Filter readFilter) {
        this.delegate = delegate;
        this.readFilter = readFilter;
    }

    /** A null query is a legitimate "all granules" request for some sources, secure it all the same. */
    protected Query secureQuery(Query q) {
        Query secured = q == null ? new Query() : new Query(q);
        secured.setFilter(and(secured.getFilter()));
        return secured;
    }

    /** Returns the security filter alone when there is nothing to AND, the conjunction otherwise. */
    protected Filter and(Filter filter) {
        if (filter == null || Filter.INCLUDE.equals(filter)) {
            return readFilter;
        }
        if (Filter.INCLUDE.equals(readFilter)) {
            return filter;
        }
        return Predicates.and(filter, readFilter);
    }

    @Override
    public SimpleFeatureCollection getGranules(Query q) throws IOException {
        return delegate.getGranules(secureQuery(q));
    }

    @Override
    public int getCount(Query q) throws IOException {
        return delegate.getCount(secureQuery(q));
    }

    @Override
    public ReferencedEnvelope getBounds(Query q) throws IOException {
        return delegate.getBounds(secureQuery(q));
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return delegate.getSchema();
    }

    @Override
    public void dispose() throws IOException {
        delegate.dispose();
    }

    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return delegate.getSupportedHints();
    }
}
