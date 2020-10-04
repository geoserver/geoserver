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
import static org.geotools.dggs.gstore.DGGSStore.VP_RESOLUTION;
import static org.geotools.dggs.gstore.DGGSStore.VP_RESOLUTION_DELTA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.geotools.dggs.ChildrenFunction;
import org.geotools.dggs.DGGSFilterVisitor;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.DGGSSetFunction;
import org.geotools.dggs.NeighborFunction;
import org.geotools.dggs.Zone;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.function.EnvFunction;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class DGGSGeometryFeatureSource extends ContentFeatureSource implements DGGSFeatureSource {

    static final Logger LOGGER = Logging.getLogger(DGGSGeometryFeatureSource.class);

    /* The GetMap scale denominator, as an {@link Double}, duplicated here to avoid a dependency
     * onto gs-wms */
    private static final String WMS_SCALE_DENOMINATOR = "WMS_SCALE_DENOMINATOR";

    private static final double OGC_DEGREE_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360;
    private static final double DISTANCE_SCALE_FACTOR = 0.0254 / (25.4 / 0.28);

    private final DGGSGeometryStore store;

    public DGGSGeometryFeatureSource(ContentEntry entry, DGGSGeometryStore store) {
        super(entry, Query.ALL);
        this.store = store;
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

        int targetResolution = getTargetResolution(query);
        if (targetResolution < 0 || targetResolution >= store.levelThresholds.length) return 0;

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

    /** Extends the propertyNames array by eventual attributes needed by the Query filter */
    private String[] extendAttributesByFilter(Query query, String[] propertyNames) {
        FilterAttributeExtractor attributeExtractor = new FilterAttributeExtractor();
        query.getFilter().accept(attributeExtractor, null);
        Set<String> filterAttributes = attributeExtractor.getAttributeNameSet();
        if (Arrays.stream(propertyNames).anyMatch(a -> !filterAttributes.contains(a))) {
            LinkedHashSet<String> extendedAttributes =
                    new LinkedHashSet<>(Arrays.asList(propertyNames));
            extendedAttributes.addAll(filterAttributes);
            propertyNames = extendedAttributes.toArray(new String[extendedAttributes.size()]);
        }
        return propertyNames;
    }

    /**
     * Injects the DGGS instance into the DGGS Sspecific functions
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

    private long getOptimizedCount(Query query) {
        Filter filter = query.getFilter();
        if (filter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo pe = (PropertyIsEqualTo) filter;
            Expression ex1 = pe.getExpression1();
            Expression ex2 = pe.getExpression2();
            if (ex1 instanceof DGGSSetFunction && ((DGGSSetFunction) ex1).isStable()) {
                return ((DGGSSetFunction) ex1).countMatched();
            } // should we handle backwards comparison too?
        }

        // no optimization possible
        return -1l;
    }

    /**
     * Returs a zone iterator providing the neighbors
     *
     * @param function
     * @return
     */
    private Iterator<Zone> getIteratorFromNeighbor(NeighborFunction function) {
        List<Expression> parameters = function.getParameters();
        String referenceZoneId = parameters.get(1).evaluate(null, String.class);
        Integer distance = parameters.get(2).evaluate(null, Integer.class);
        if (referenceZoneId == null || distance == null) return new EmptyIterator();

        // the function is lenient, make this implementation too
        try {
            store.dggs.getZone(referenceZoneId);
            return store.dggs.neighbors(referenceZoneId, distance);
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING, "Failed to evalute neighbors, returning an empty iterator", e);
            return new EmptyIterator();
        }
    }

    /**
     * Returs a zone iterator providing the children at a rigen resolution
     *
     * @param function
     * @return
     */
    private Iterator<Zone> getIteratorFromChildren(ChildrenFunction function) {
        List<Expression> parameters = function.getParameters();
        String referenceZoneId = parameters.get(1).evaluate(null, String.class);
        Integer resolution = parameters.get(2).evaluate(null, Integer.class);
        if (referenceZoneId == null || resolution == null) return new EmptyIterator();

        // the function is lenient, make this implementation too
        try {
            store.dggs.getZone(referenceZoneId); // validation
            return store.dggs.children(referenceZoneId, resolution);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to evalute children, returning an empty iterator", e);
            return new EmptyIterator();
        }
    }

    private boolean literalTrue(Expression ex) {
        return ex instanceof Literal
                && Boolean.TRUE.equals(((Literal) ex).evaluate(null, Boolean.class));
    }

    /**
     * Checks if all input expressions to the function are {@link Literal}, with the exception of
     * the first one
     *
     * @param function
     * @return
     */
    private boolean cacheableDGGSCall(Function function) {
        // TODO: allow the resolution to be provided as a env call, to make the calls
        // to children, parent, polygon, point work across zoom ins/out in the display
        return function.getParameters().stream().skip(1).allMatch(e -> e instanceof Literal);
    }

    private Iterator<Zone> getIteratorFromEnvelope(Query query) {
        Envelope bounds = getFilterBounds(query.getFilter());

        if (bounds.isNull()) {
            return null;
        }

        // map geometry to list of ids, then create an iterator against them
        int targetResolution = getTargetResolution(query);
        if (targetResolution < 0 || targetResolution >= store.levelThresholds.length) return null;

        if (query.getFilter() instanceof BBOX) {
            query.setFilter(Filter.INCLUDE); // replaced filter with source iterator
        }
        return store.dggs.zonesFromEnvelope(bounds, targetResolution);
    }

    private int getTargetResolution(Query query) {
        Hints hints = query.getHints();

        Optional<Map> viewParams =
                Optional.ofNullable(hints.get(Hints.VIRTUAL_TABLE_PARAMETERS))
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast);

        // did the user ask for a specific resolution?
        Optional<Integer> requestedResolution =
                viewParams.map(m -> m.get(VP_RESOLUTION)).map(n -> safeConvert(n, Integer.class));
        if (requestedResolution.isPresent()) {
            return validateResolution(requestedResolution.get().intValue());
        }

        // the simplificaiton distance varies too much as we pan around on projections
        // with significant deformations (e.g., polar, web mercator) leading to resolution
        // switches and excess zone generation. Try something more stable and predictable first,
        // like the OGC scale denominator.
        Optional<Double> distance;
        Double sd =
                safeConvert(EnvFunction.getLocalValues().get(WMS_SCALE_DENOMINATOR), Double.class);
        if (sd != null) {
            distance = Optional.of(scaleToDistance(DefaultGeographicCRS.WGS84, sd));
        } else {
            distance =
                    Optional.ofNullable(hints.get(Hints.GEOMETRY_DISTANCE))
                            .filter(Number.class::isInstance)
                            .map(Number.class::cast)
                            .map(n -> n.doubleValue());
        }

        // do we have a resoution delta?
        int resolutionDelta =
                viewParams
                        .map(m -> m.get(VP_RESOLUTION_DELTA))
                        .map(n -> safeConvert(n, Integer.class))
                        .orElse(0);

        // compute resolution and eventually apply delta
        return distance.map(n -> getResolutionFromThresholds(n.doubleValue()) + resolutionDelta)
                .orElse(0);
    }

    private <T> T safeConvert(Object n, Class<T> target) {
        return Converters.convert(n, target, new Hints(ConverterFactory.SAFE_CONVERSION, true));
    }

    /**
     * Converts a scale denominator to a generalization distance using the OGC SLD scale denominator
     * computation rules
     *
     * @param crs The CRS of the data
     * @param scaleDenominator The target scale denominator
     * @return
     */
    static double scaleToDistance(CoordinateReferenceSystem crs, double scaleDenominator) {
        return scaleDenominator * DISTANCE_SCALE_FACTOR / RendererUtilities.toMeters(1, crs);
    }

    private int getResolutionFromThresholds(double distance) {
        if (distance == 0) return 0;

        double[] thresholds = store.levelThresholds;
        for (int i = 0; i < thresholds.length; i++) {
            if (thresholds[i] < distance) return i;
        }
        return thresholds.length - 1;
    }

    private int validateResolution(int resolution) {
        if (resolution < 0 || resolution >= store.levelThresholds.length) {
            throw new IllegalArgumentException(
                    "Requested resolution "
                            + resolution
                            + " is not valid, please provide a value between 0 and "
                            + (store.levelThresholds.length - 1));
        }

        return resolution;
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
}
