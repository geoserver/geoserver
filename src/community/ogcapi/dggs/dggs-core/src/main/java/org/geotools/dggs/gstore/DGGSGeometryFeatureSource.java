/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.gstore;

import static org.geotools.dggs.DGGSInstance.WORLD;
import static org.geotools.dggs.gstore.DGGSStore.DGGS_INTRINSIC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.collections4.iterators.SingletonIterator;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.ReTypeFeatureReader;
import org.geotools.data.simple.EmptySimpleFeatureReader;
import org.geotools.data.sort.SortedFeatureReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.EmptyIterator;
import org.geotools.dggs.DGGSFilterVisitor;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.DGGSSetFunction;
import org.geotools.dggs.Zone;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.FeatureAttributeVisitor;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;

class DGGSGeometryFeatureSource extends ContentFeatureSource implements DGGSFeatureSource {

    static final Logger LOGGER = Logging.getLogger(DGGSGeometryFeatureSource.class);

    private final DGGSGeometryStore store;
    private DGGSResolutionCalculator resolutions;

    public DGGSGeometryFeatureSource(ContentEntry entry, DGGSGeometryStore store) {
        super(entry, Query.ALL);
        this.store = store;
        this.resolutions = store.resolutions;
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        if (query == Query.ALL
                || query == null
                || query.getFilter() == Filter.INCLUDE
                || query.getFilter() == null) return WORLD;

        // TODO: compute bounds based on actual filtering?
        return null;
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        // TODO: have fast counts for neighbors, children and parents too
        Envelope bounds = getFilterBounds(query.getFilter());
        if (bounds.isNull()) {
            return 0;
        }

        int targetResolution = resolutions.getTargetResolution(query, 0);
        if (!resolutions.isValid(targetResolution)) return 0;

        // delegate to the DGGS, which can do optimized count calculations
        Filter filter = query.getFilter();
        if (filter == null || filter == Filter.INCLUDE || filter instanceof BBOX) {
            long count = store.dggs.countZonesFromEnvelope(bounds, targetResolution);
            if (count > Integer.MAX_VALUE) {
                LOGGER.warning("Count exceeds integer range, returning MAXINT");
                return Integer.MAX_VALUE;
            }

            return (int) count;
        } else {
            // cannot make it fast
            return -1;
        }
    }

    private Envelope getFilterBounds(Filter filter) {
        Envelope bounds = (Envelope) filter.accept(ExtractBoundsFilterVisitor.BOUNDS_VISITOR, null);
        if (bounds == null) bounds = WORLD;
        return bounds;
    }

    @Override
    protected boolean canSort() {
        // at least for natural ordering, DDGS has a predictable iteration order
        return true;
    }

    @Override
    protected boolean canFilter() {
        return true;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query originalQuery)
            throws IOException {
        SimpleFeatureType readSchema = getReadSchema(query);

        Query query = injectDGGS(originalQuery);
        Iterator<Zone> iterator = getZoneIterator(query);
        if (iterator == null) {
            return new EmptySimpleFeatureReader(readSchema);
        }

        // reading might need properties used by filtering and sorting only
        SimpleFeatureType resultSchema = getResultSchema(query);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                new ZonesFeatureIterator(iterator, readSchema, store.dggs.getExtraProperties());

        // filter if necessary
        Filter filter = query.getFilter();
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            reader = new FilteringFeatureReader<>(reader, filter);
        }

        // sorting, for basic paging just use the DGGS iteration order
        SortBy[] sortBy = query.getSortBy();
        if (sortBy != null && sortBy.length != 0) {
            if (sortBy != SortBy.UNSORTED
                    && !(sortBy.length == 1 && sortBy[0] == SortBy.NATURAL_ORDER)) {
                reader = new SortedFeatureReader(DataUtilities.simple(reader), query);
            }
        }

        // retyping at end if necessary
        if (!FeatureTypes.equals(readSchema, resultSchema)) {
            reader = new ReTypeFeatureReader(reader, resultSchema);
        }

        return reader;
    }

    SimpleFeatureType getResultSchema(Query q) {
        if (q.getPropertyNames() == null) {
            return getSchema();
        } else {
            return SimpleFeatureTypeBuilder.retype(getSchema(), q.getPropertyNames());
        }
    }

    SimpleFeatureType getReadSchema(Query q) {
        if (q.getPropertyNames() == Query.ALL_NAMES) {
            return getSchema();
        }
        // Step 1: start with requested property names
        LinkedHashSet<String> attributes = new LinkedHashSet<>();
        attributes.addAll(Arrays.asList(q.getPropertyNames()));

        Filter filter = q.getFilter();
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            // Step 2: Add query attributes (if needed)
            FilterAttributeExtractor fat = new FilterAttributeExtractor(getSchema());
            filter.accept(fat, null);
            attributes.addAll(fat.getAttributeNameSet());
        }

        // Step 3, adding sorting attributes if needed
        if (q.getSortBy() != null && q.getSortBy().length > 0) {
            for (SortBy sort : q.getSortBy()) {
                if (sort.getPropertyName() != null) {
                    attributes.add(sort.getPropertyName().getPropertyName());
                }
            }
        }
        return SimpleFeatureTypeBuilder.retype(getSchema(), new ArrayList<>(attributes));
    }

    /**
     * Injects the DGGS instance into the DGGS specific functions
     *
     * @param originalQuery
     * @return
     */
    private Query injectDGGS(Query originalQuery) {
        Filter originalFilter = originalQuery.getFilter();
        Filter filter = (Filter) originalFilter.accept(new DGGSFilterVisitor(store.dggs), null);
        Query query = new Query(originalQuery);
        query.setFilter(filter);
        return query;
    }

    private Iterator<Zone> getZoneIterator(Query query) {
        Filter filter = query.getFilter();
        if (filter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo pe = (PropertyIsEqualTo) filter;
            Expression ex1 = pe.getExpression1();
            Expression ex2 = pe.getExpression2();
            if (ex1 instanceof DGGSSetFunction && ((DGGSSetFunction) ex1).isStable()) {
                query.setFilter(Filter.INCLUDE); // replaced filter with source iterator
                return ((DGGSSetFunction) ex1).getMatchedZones();
            } // should we handle backwards comparison too?
            else if (ex1 instanceof PropertyName
                    && ((PropertyName) ex1).getPropertyName().equals("zoneId")
                    && ex2 instanceof Literal) {
                query.setFilter(Filter.INCLUDE); // replaced filter with source iterator
                Zone zone = store.dggs.getZone(ex2.evaluate(null, String.class));
                if (zone == null) return new EmptyIterator();
                return new SingletonIterator<>(zone);
            }
        }

        // fallback, go for geographical lookup
        return getIteratorFromEnvelope(query);
    }

    private Iterator<Zone> getIteratorFromEnvelope(Query query) {
        Envelope bounds = getFilterBounds(query.getFilter());

        if (bounds.isNull()) {
            return null;
        }

        // map geometry to list of ids, then create an iterator against them
        int targetResolution = resolutions.getTargetResolution(query, 0);
        if (!store.resolutions.isValid(targetResolution)) return null;

        if (query.getFilter() instanceof BBOX) {
            query.setFilter(Filter.INCLUDE); // replaced filter with source iterator
        }
        return store.dggs.zonesFromEnvelope(bounds, targetResolution, false);
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName(entry.getName());
        tb.add(DGGSGeometryStore.ZONE_ID, String.class);
        tb.userData(DGGS_INTRINSIC, true);
        tb.add(DGGSGeometryStore.RESOLUTION, Integer.class);
        tb.userData(DGGS_INTRINSIC, true);
        tb.add(DGGSGeometryStore.GEOMETRY, Geometry.class, DefaultGeographicCRS.WGS84);
        store.dggs.getExtraProperties().forEach(ad -> tb.add(ad));
        return tb.buildFeatureType();
    }

    @Override
    protected void addHints(Set<Hints.Key> hints) {
        hints.add(Hints.GEOMETRY_DISTANCE);
    }

    @Override
    public DGGSInstance getDGGS() {
        return store.dggs;
    }

    @Override
    protected boolean canRetype() {
        return true;
    }

    @Override
    protected boolean handleVisitor(Query query, FeatureVisitor visitor) throws IOException {
        if (visitor instanceof FeatureAttributeVisitor) {
            FeatureAttributeVisitor fav = (FeatureAttributeVisitor) visitor;
            Set<String> attributes = getAttributeSet(fav);
            // can optimize a few visits based on resolution alone
            if (attributes != null
                    && attributes.equals(Collections.singleton(DGGSStore.RESOLUTION))) {
                int[] resolutions = getDGGS().getResolutions();
                if (fav instanceof MinVisitor) {
                    ((MinVisitor) fav).setValue(resolutions[0]);
                    return true;
                } else if (fav instanceof MaxVisitor) {
                    ((MaxVisitor) fav).setValue(resolutions[resolutions.length - 1]);
                    return true;
                } else if (fav instanceof UniqueVisitor) {
                    // converting an array to a list it's harder than it seems, Arrays.asList
                    // would produce a List with one item, the array given as a param
                    List<Integer> rl =
                            Arrays.stream(resolutions)
                                    .mapToObj(v -> v)
                                    .collect(Collectors.toList());
                    ((UniqueVisitor) fav).setValue(rl);
                    return true;
                }
            }
        }
        // fall back on default behavior
        return super.handleVisitor(query, visitor);
    }

    private Set<String> getAttributeSet(FeatureAttributeVisitor fav) {
        Set<String> result = new HashSet<>();
        for (Expression ex : fav.getExpressions()) {
            AttributeDescriptor ad = ex.evaluate(getSchema(), AttributeDescriptor.class);
            if (ad == null) return null;
            result.add(ad.getLocalName());
        }
        return result;
    }
}
