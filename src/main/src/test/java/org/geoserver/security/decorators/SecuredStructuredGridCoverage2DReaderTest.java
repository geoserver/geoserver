/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.data.Query;
import org.geotools.api.data.Transaction;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.InsufficientAuthenticationException;

/** Checks the granule access limits are honored by {@link SecuredStructuredGridCoverage2DReader#getGranules}. */
public class SecuredStructuredGridCoverage2DReaderTest {

    private static final String COVERAGE = "test";

    private static final String TIME_FILTER = "time AFTER 2020-01-01T00:00:00Z";

    private StructuredGridCoverage2DReader delegate;

    private GranuleSource delegateGranules;

    private Filter readFilter;

    @Before
    public void setUp() throws Exception {
        readFilter = ECQL.toFilter("elevation < 100");
        delegateGranules = mock(GranuleSource.class);
        delegate = mock(StructuredGridCoverage2DReader.class);
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateGranules);
    }

    /** No wrapping when the limits do not restrict granules: the delegate source must come back untouched. */
    @Test
    public void testUnrestrictedReturnsDelegate() throws Exception {
        assertSame(delegateGranules, reader(Filter.INCLUDE).getGranules(COVERAGE, true));
        assertSame(delegateGranules, reader(null).getGranules(COVERAGE, true));
    }

    @Test
    public void testSecurityFilterOnlyWhenQueryHasNone() throws Exception {
        secured().getGranules(new Query(COVERAGE));
        assertEquals(readFilter, capturedQuery().getFilter());
    }

    @Test
    public void testSecurityFilterAndedWithQueryFilter() throws Exception {
        secured().getGranules(new Query(COVERAGE, ECQL.toFilter(TIME_FILTER)));
        assertEquals(
                ECQL.toFilter(TIME_FILTER + " AND elevation < 100"),
                capturedQuery().getFilter());
    }

    /**
     * The allowed area is a pixel level crop for the data, but the granule index can only include or exclude whole
     * records, so it has to reach the query as a footprint intersection.
     */
    @Test
    public void testAreaRestrictionAppliedAsFootprintIntersection() throws Exception {
        MultiPolygon area = area();
        when(delegateGranules.getSchema()).thenReturn(spatialGranule(0).getSchema());
        reader(Filter.INCLUDE, area, WrapperPolicy::readOnlyHide)
                .getGranules(COVERAGE, true)
                .getGranules(new Query(COVERAGE));
        assertEquals(intersects(area), capturedQuery().getFilter());
    }

    @Test
    public void testAreaRestrictionAndedWithReadFilterAndQuery() throws Exception {
        MultiPolygon area = area();
        when(delegateGranules.getSchema()).thenReturn(spatialGranule(0).getSchema());
        reader(readFilter, area, WrapperPolicy::readOnlyHide)
                .getGranules(COVERAGE, true)
                .getGranules(new Query(COVERAGE, ECQL.toFilter(TIME_FILTER)));
        And filter = (And) capturedQuery().getFilter();
        assertThat(filter.getChildren(), contains(ECQL.toFilter(TIME_FILTER), readFilter, intersects(area)));
    }

    /**
     * An area declaring its own SRID has to reach the index in the footprint CRS, the same rule
     * {@link SecuredFeatureSource} applies to its clip geometries: comparing raw ordinates would hide every granule.
     */
    @Test
    public void testAreaRestrictionReprojectedToTheFootprintCRS() throws Exception {
        MultiPolygon area = area();
        area.setSRID(3857);
        when(delegateGranules.getSchema()).thenReturn(spatialGranule(0).getSchema());
        reader(Filter.INCLUDE, area, WrapperPolicy::readOnlyHide)
                .getGranules(COVERAGE, true)
                .getGranules(new Query(COVERAGE));

        MathTransform toWgs84 = CRS.findMathTransform(CRS.decode("EPSG:3857", true), WGS84, true);
        assertEquals(intersects(JTS.transform(area, toWgs84)), capturedQuery().getFilter());
    }

    /** Without a geometry there is nothing to compare the area against, so every granule would come back. */
    @Test
    public void testAreaRestrictionRefusedWhenGranulesHaveNoGeometry() throws Exception {
        when(delegateGranules.getSchema()).thenReturn(locationOnlyGranule().getSchema());
        StructuredGridCoverage2DReader reader = reader(Filter.INCLUDE, area(), WrapperPolicy::readOnlyHide);
        IOException exception = assertThrows(IOException.class, () -> reader.getGranules(COVERAGE, true));
        assertThat(exception.getMessage(), containsString("no geometry"));
    }

    private static MultiPolygon area() throws Exception {
        return (MultiPolygon) new WKTReader().read("MULTIPOLYGON(((0 0, 10 0, 10 10, 0 10, 0 0)))");
    }

    /** The filter the area restriction is expected to turn into, on the default geometry of the granule schema. */
    private static Filter intersects(Geometry area) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        return ff.intersects(ff.property("geom"), ff.literal(area));
    }

    @Test
    public void testCountFiltered() throws Exception {
        secured().getCount(new Query(COVERAGE));
        assertEquals(readFilter, captured(GranuleSource::getCount).getFilter());
    }

    @Test
    public void testBoundsFiltered() throws Exception {
        secured().getBounds(new Query(COVERAGE));
        assertEquals(readFilter, captured(GranuleSource::getBounds).getFilter());
    }

    /** With write access granted, removals must be scoped to the granules the read filter leaves visible. */
    @Test
    public void testRemovalScopedToVisibleGranules() throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);

        GranuleSource granules = reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);
        assertThat(granules, instanceOf(GranuleStore.class));
        ((GranuleStore) granules).removeGranules(ECQL.toFilter(TIME_FILTER));

        verify(delegateStore).removeGranules(ECQL.toFilter(TIME_FILTER + " AND elevation < 100"));
    }

    /** A granule whose values escape the read filter would be invisible to its own author, so it is refused. */
    @Test
    public void testAddRefusedWhenGranuleEscapesTheReadFilter() throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);
        doAnswer(invocation -> ((SimpleFeatureCollection) invocation.getArgument(0)).toArray())
                .when(delegateStore)
                .addGranules(any());
        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);

        granules.addGranules(granuleCollection(50));
        verify(delegateStore).addGranules(any());

        assertThrows(UnsupportedOperationException.class, () -> granules.addGranules(granuleCollection(500)));
    }

    /** The delegate picks how it reads the collection, and every path has to go through the check. */
    @Test
    public void testAddRefusedWhateverTheDelegateReads() throws Exception {
        assertThrows(UnsupportedOperationException.class, () -> addConsumedBy(SimpleFeatureCollection::toArray));
        assertThrows(
                UnsupportedOperationException.class,
                () -> addConsumedBy(
                        granules -> granules.subCollection(Filter.INCLUDE).toArray()));
        // DataUtilities.visit, which accepts() routes through, reports the refusal as an IOException instead
        assertThrows(IOException.class, () -> addConsumedBy(granules -> granules.accepts(feature -> {}, null)));
    }

    /** Adds a granule the read filter excludes to a store whose delegate consumes the collection the given way. */
    private void addConsumedBy(GranuleRead read) throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        doAnswer(invocation -> {
                    read.apply(invocation.getArgument(0));
                    return null;
                })
                .when(delegateStore)
                .addGranules(any());
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);

        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);
        granules.addGranules(granuleCollection(500));
    }

    private interface GranuleRead {
        void apply(SimpleFeatureCollection granules) throws Exception;
    }

    /** The caller owns its transaction: a refusal must not undo the work it did on it behind its back. */
    @Test
    @SuppressWarnings("PMD.CloseResource") // the transaction is a mock, there is nothing to close
    public void testAddRefusalLeavesTheTransactionAlone() throws Exception {
        Transaction transaction = mock(Transaction.class);
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegateStore.getTransaction()).thenReturn(transaction);
        doAnswer(invocation -> ((SimpleFeatureCollection) invocation.getArgument(0)).toArray())
                .when(delegateStore)
                .addGranules(any());
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);

        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);
        assertThrows(UnsupportedOperationException.class, () -> granules.addGranules(granuleCollection(500)));

        verify(transaction, never()).rollback();
    }

    /** The refusal arrives wrapped in an IOException on this path, and still has to stop the add. */
    @Test
    @SuppressWarnings("PMD.CloseResource") // the transaction is a mock, there is nothing to close
    public void testAddRefusedWhenTheDelegateReadsWithAccepts() throws Exception {
        Transaction transaction = mock(Transaction.class);
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegateStore.getTransaction()).thenReturn(transaction);
        doAnswer(invocation -> {
                    ((SimpleFeatureCollection) invocation.getArgument(0)).accepts(feature -> {}, null);
                    return null;
                })
                .when(delegateStore)
                .addGranules(any());
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);

        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);
        assertThrows(IOException.class, () -> granules.addGranules(granuleCollection(500)));

        verify(transaction, never()).rollback();
    }

    /** An empty filter property name is the default geometry, not an attribute the granules are missing. */
    @Test
    public void testDefaultGeometryFilterResolvedAgainstTheSchema() throws Exception {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        readFilter = ff.bbox(ff.property(""), new ReferencedEnvelope(0, 10, 0, 10, WGS84));
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegateStore.getSchema()).thenReturn(spatialGranule(0).getSchema());
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);
        doAnswer(invocation -> ((SimpleFeatureCollection) invocation.getArgument(0)).toArray())
                .when(delegateStore)
                .addGranules(any());
        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);

        granules.addGranules(spatialGranule(5));
        verify(delegateStore).addGranules(any());

        assertThrows(UnsupportedOperationException.class, () -> granules.addGranules(spatialGranule(50)));
    }

    /** A single granule collection holding a point at the given coordinates, on the default geometry attribute. */
    private static SimpleFeatureCollection spatialGranule(double position) {
        SimpleFeatureTypeBuilder type = new SimpleFeatureTypeBuilder();
        type.setName("granule");
        type.setCRS(WGS84);
        type.add("geom", Point.class);
        type.setDefaultGeometry("geom");
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type.buildFeatureType());
        builder.add(new GeometryFactory().createPoint(new Coordinate(position, position)));
        return DataUtilities.collection(builder.buildFeature(null));
    }

    /**
     * The read filter is evaluated in memory, and an attribute the granules do not carry evaluates to false rather than
     * raising, so without this gate every granule would be refused with the wrong reason.
     */
    @Test
    public void testAddRefusedWhenGranulesLackTheFilterAttribute() throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegateStore.getSchema()).thenReturn(granuleCollection(50).getSchema());
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);
        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);

        UnsupportedOperationException refused =
                assertThrows(UnsupportedOperationException.class, () -> granules.addGranules(locationOnlyGranule()));

        assertThat(refused.getMessage(), containsString("elevation"));
        verify(delegateStore, never()).addGranules(any());
    }

    /** The gate must stay out of the way when the granules do carry the attributes the read filter is based on. */
    @Test
    public void testAddAllowedWhenGranulesCarryTheFilterAttribute() throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegateStore.getSchema()).thenReturn(granuleCollection(50).getSchema());
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);
        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);

        granules.addGranules(granuleCollection(50));

        verify(delegateStore).addGranules(any());
    }

    /** A granule missing the attribute the read filter keys on. */
    private static SimpleFeatureCollection locationOnlyGranule() {
        SimpleFeatureTypeBuilder type = new SimpleFeatureTypeBuilder();
        type.setName("granule");
        type.add("location", String.class);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type.buildFeatureType());
        builder.add("a.tif");
        return DataUtilities.collection(builder.buildFeature(null));
    }

    /** Writing an attribute the read filter is based on would let a caller hide granules from itself. */
    @Test
    public void testUpdateRefusedOnRestrictedAttribute() throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);
        GranuleStore granules =
                (GranuleStore) reader(readFilter, WrapperPolicy::readWrite).getGranules(COVERAGE, false);

        assertThrows(
                UnsupportedOperationException.class,
                () -> granules.updateGranules(new String[] {"elevation"}, new Object[] {500}, Filter.INCLUDE));

        granules.updateGranules(new String[] {"location"}, new Object[] {"a.tif"}, Filter.INCLUDE);
        verify(delegateStore).updateGranules(new String[] {"location"}, new Object[] {"a.tif"}, readFilter);
    }

    /** A single granule collection carrying the given elevation, the attribute the read filter is based on. */
    private static SimpleFeatureCollection granuleCollection(int elevation) {
        SimpleFeatureTypeBuilder type = new SimpleFeatureTypeBuilder();
        type.setName("granule");
        type.add("elevation", Integer.class);
        type.add("location", String.class);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type.buildFeatureType());
        builder.addAll(elevation, "a.tif");
        return DataUtilities.collection(builder.buildFeature(null));
    }

    /** An unknown coverage comes back as a null source, and a wrapper around it would fail on first use instead. */
    @Test
    public void testNullSourceNotWrapped() throws Exception {
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(null);
        assertNull(reader(readFilter).getGranules("missing", true));
        assertNull(reader(Filter.INCLUDE).getGranules("missing", true));
    }

    /**
     * A delegate is free to return a store even for a read only request, and the caller would then write through it
     * without ever passing the write access check.
     */
    @Test
    public void testStoreHiddenOnReadOnlyRequest() throws Exception {
        GranuleStore delegateStore = mock(GranuleStore.class);
        when(delegate.getGranules(any(), anyBoolean())).thenReturn(delegateStore);

        assertThat(reader(readFilter).getGranules(COVERAGE, true), not(instanceOf(GranuleStore.class)));
        assertThat(reader(Filter.INCLUDE).getGranules(COVERAGE, true), not(instanceOf(GranuleStore.class)));

        // the read filter still applies, hiding the store must not cost the restriction
        reader(readFilter).getGranules(COVERAGE, true).getGranules(new Query(COVERAGE));
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(delegateStore).getGranules(captor.capture());
        assertEquals(readFilter, captor.getValue().getFilter());
    }

    /** A read only policy must refuse the mutable store outright, not silently downgrade it. */
    @Test
    public void testWriteAccessRefusedWhenReadOnly() {
        SecuredStructuredGridCoverage2DReader hide = reader(readFilter, WrapperPolicy::readOnlyHide);
        assertThrows(UnsupportedOperationException.class, () -> hide.getGranules(COVERAGE, false));

        // challenge mode asks the caller to authenticate, and the test context is anonymous
        SecuredStructuredGridCoverage2DReader challenge = reader(readFilter, WrapperPolicy::readOnlyChallenge);
        assertThrows(InsufficientAuthenticationException.class, () -> challenge.getGranules(COVERAGE, false));
    }

    /** A read only policy must refuse writes even when no granule filter is configured. */
    @Test
    public void testWriteAccessRefusedWithoutReadFilter() {
        SecuredStructuredGridCoverage2DReader reader = reader(Filter.INCLUDE);
        assertThrows(UnsupportedOperationException.class, () -> reader.getGranules(COVERAGE, false));
        assertThrows(UnsupportedOperationException.class, () -> reader.delete(true));
        assertThrows(UnsupportedOperationException.class, () -> reader.removeCoverage(COVERAGE, true));
        assertThrows(UnsupportedOperationException.class, () -> reader.createCoverage(COVERAGE, null));
        assertThrows(UnsupportedOperationException.class, () -> reader.harvest(COVERAGE, "file", null));
    }

    /** The checks must not become a wall: with write access granted every operation has to reach the delegate. */
    @Test
    public void testWriteOperationsReachTheDelegateWithWriteAccess() throws Exception {
        when(delegate.removeCoverage(COVERAGE, true)).thenReturn(true);
        when(delegate.harvest(COVERAGE, "file", null)).thenReturn(List.of());

        SecuredStructuredGridCoverage2DReader reader = reader(readFilter, WrapperPolicy::readWrite);
        reader.createCoverage(COVERAGE, null);
        assertTrue(reader.removeCoverage(COVERAGE, true));
        assertEquals(List.of(), reader.harvest(COVERAGE, "file", null));
        reader.delete(true);

        verify(delegate).createCoverage(COVERAGE, null);
        verify(delegate).delete(true);
    }

    /** An operation targeting the whole reader has no coverage name of its own, the message has to find one. */
    @Test
    public void testWriteRefusalNamesTheCoverage() throws Exception {
        when(delegate.getGridCoverageNames()).thenReturn(new String[] {COVERAGE});
        SecuredStructuredGridCoverage2DReader reader = reader(readFilter);

        assertThat(
                assertThrows(UnsupportedOperationException.class, () -> reader.delete(true))
                        .getMessage(),
                containsString(COVERAGE));
    }

    /** The reader has to advertise itself as read only, or callers will walk into the write path and fail late. */
    @Test
    public void testReadOnlyReportedFromPolicy() {
        when(delegate.isReadOnly()).thenReturn(false);
        assertTrue(reader(readFilter).isReadOnly());
        assertFalse(reader(readFilter, WrapperPolicy::readWrite).isReadOnly());
    }

    private GranuleSource secured() throws Exception {
        return reader(readFilter).getGranules(COVERAGE, true);
    }

    private Query capturedQuery() throws Exception {
        return captured(GranuleSource::getGranules);
    }

    /** Replays the given read on the verification proxy, to grab the {@link Query} handed to the delegate. */
    private Query captured(QueryRead read) throws Exception {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        read.apply(verify(delegateGranules), captor.capture());
        return captor.getValue();
    }

    private interface QueryRead {
        void apply(GranuleSource source, Query query) throws Exception;
    }

    private SecuredStructuredGridCoverage2DReader reader(Filter readFilter) {
        return reader(readFilter, WrapperPolicy::readOnlyHide);
    }

    private SecuredStructuredGridCoverage2DReader reader(
            Filter readFilter, Function<CoverageAccessLimits, WrapperPolicy> policy) {
        return reader(readFilter, null, policy);
    }

    private SecuredStructuredGridCoverage2DReader reader(
            Filter readFilter, MultiPolygon area, Function<CoverageAccessLimits, WrapperPolicy> policy) {
        CoverageAccessLimits limits = new CoverageAccessLimits(CatalogMode.HIDE, readFilter, area, null);
        return new SecuredStructuredGridCoverage2DReader(delegate, policy.apply(limits));
    }
}
