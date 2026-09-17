/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.util.factory.Hints;

/**
 * {@link GranuleStore} wrapper keeping every write inside the set of granules the security read filter allows to see,
 * so a caller cannot touch, create or hide granules it could not read back.
 */
class SecuredGranuleStore extends SecuredGranuleSource implements GranuleStore {

    private final GranuleStore storeDelegate;

    /** Lazily resolved, then immutable: the schema read needs IO the constructor has no way to report. */
    private volatile Set<String> restrictedAttributes;

    SecuredGranuleStore(GranuleStore delegate, Filter readFilter) {
        super(delegate, readFilter);
        this.storeDelegate = delegate;
    }

    /**
     * Attributes the read filter keys on, resolved against the granule schema so a namespace prefix or an XPath in the
     * filter turns into the plain name the caller uses. Racing callers compute the same set, so the read is unguarded.
     */
    private Set<String> restrictedAttributes() {
        Set<String> resolved = restrictedAttributes;
        if (resolved == null) {
            SimpleFeatureType schema = storeSchema();
            FilterAttributeExtractor extractor = new FilterAttributeExtractor(schema);
            readFilter.accept(extractor, null);
            resolved = extractor.getAttributeNameSet().stream()
                    .map(name -> normalize(name, schema))
                    .collect(Collectors.toUnmodifiableSet());
            restrictedAttributes = resolved;
        }
        return resolved;
    }

    private SimpleFeatureType storeSchema() {
        try {
            return storeDelegate.getSchema();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the granule schema to apply the read restrictions", e);
        }
    }

    /**
     * Local name in lower case, granule catalogs being case insensitive about attribute names. An empty property name
     * is the default geometry, see OGC Filter Encoding 2.0 section 7.4.2, and the schema is the only place it is named.
     */
    private static String normalize(String attributeName, SimpleFeatureType schema) {
        if (attributeName.isEmpty()) {
            GeometryDescriptor geometry = schema.getGeometryDescriptor();
            return geometry == null ? "" : normalize(geometry.getLocalName(), schema);
        }
        int cut = Math.max(attributeName.lastIndexOf(':'), attributeName.lastIndexOf('/'));
        return attributeName.substring(cut + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * @throws UnsupportedOperationException if a granule lands outside the read filter, possibly wrapped in an
     *     IOException by the delegate. Granules taken before the refusal stay in the index, rolling back is up to the
     *     caller owning the transaction
     * @throws IllegalStateException if the granule schema cannot be read
     */
    @Override
    public void addGranules(SimpleFeatureCollection granules) {
        checkVerifiable(granules.getSchema());
        // checking as the delegate iterates keeps it to one pass, so one shot collections keep working, and a
        // refused granule never reaches the delegate
        storeDelegate.addGranules(new CheckedGranules(granules, readFilter));
    }

    /**
     * Fails closed when the read filter keys on attributes the granules do not carry: evaluating it in memory would
     * quietly return false for every one of them, reporting a blanket denial instead of the missing attribute.
     */
    private void checkVerifiable(SimpleFeatureType schema) {
        Set<String> available = schema.getAttributeDescriptors().stream()
                .map(descriptor -> normalize(descriptor.getLocalName(), schema))
                .collect(Collectors.toSet());
        for (String restricted : restrictedAttributes()) {
            if (!available.contains(restricted)) {
                throw new UnsupportedOperationException("Cannot check the granules to add against the read "
                        + "restrictions, which are based on " + restricted + ", an attribute the granules do not have");
            }
        }
    }

    @Override
    public int removeGranules(Filter filter) {
        return storeDelegate.removeGranules(and(filter));
    }

    @Override
    public int removeGranules(Filter filter, Hints hints) {
        return storeDelegate.removeGranules(and(filter), hints);
    }

    /**
     * @throws UnsupportedOperationException if the update writes an attribute the read filter is based on, as the new
     *     value could push the granule out of the caller's own view
     * @throws IllegalStateException if the granule schema cannot be read
     */
    @Override
    public void updateGranules(String[] attributeNames, Object[] attributeValues, Filter filter) {
        SimpleFeatureType schema = storeSchema();
        for (String attributeName : attributeNames) {
            if (restrictedAttributes().contains(normalize(attributeName, schema))) {
                throw new UnsupportedOperationException(
                        "Cannot update " + attributeName + ", the granule read restrictions are based on it");
            }
        }
        storeDelegate.updateGranules(attributeNames, attributeValues, and(filter));
    }

    @Override
    public Transaction getTransaction() {
        return storeDelegate.getTransaction();
    }

    @Override
    public void setTransaction(Transaction transaction) {
        storeDelegate.setTransaction(transaction);
    }

    /**
     * Granule collection failing the iteration as soon as a granule the read filter excludes shows up. The filter is
     * evaluated in memory, so its geometries must already be in the granule CRS: the area restriction is reprojected
     * upstream, geometry literals written into the admin read filter are not, exactly as on the vector side.
     */
    private static class CheckedGranules extends DecoratingSimpleFeatureCollection {

        private final Filter readFilter;

        CheckedGranules(SimpleFeatureCollection delegate, Filter readFilter) {
            super(delegate);
            this.readFilter = readFilter;
        }

        @Override
        public SimpleFeatureIterator features() {
            return new DecoratingSimpleFeatureIterator(super.features()) {
                @Override
                public SimpleFeature next() {
                    SimpleFeature granule = super.next();
                    if (!readFilter.evaluate(granule)) {
                        throw new UnsupportedOperationException(
                                "Granule " + granule.getID() + " is excluded by the read restrictions");
                    }
                    return granule;
                }
            };
        }

        // the decorator forwards these to the delegate, handing out granules the checked iterator never saw

        @Override
        public SimpleFeatureCollection subCollection(Filter filter) {
            return new CheckedGranules(super.subCollection(filter), readFilter);
        }

        @Override
        public SimpleFeatureCollection sort(SortBy order) {
            return new CheckedGranules(super.sort(order), readFilter);
        }

        @Override
        public Object[] toArray() {
            return DataUtilities.list(this).toArray();
        }

        @Override
        public <F> F[] toArray(F[] array) {
            return DataUtilities.list(this).toArray(array);
        }
    }
}
