/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AbstractCatalogDecorator;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.AbstractCatalogFilter;
import org.geoserver.security.CatalogFilterAccessManager;
import org.geoserver.security.DisabledResourceFilter;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.ReadOnlyDataStoreTest;
import org.geoserver.security.decorators.SecuredCoverageInfo;
import org.geoserver.security.decorators.SecuredDataStoreInfo;
import org.geoserver.security.decorators.SecuredFeatureTypeInfo;
import org.geoserver.security.decorators.SecuredLayerGroupInfo;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geoserver.security.decorators.SecuredWMSLayerInfo;
import org.geoserver.security.decorators.SecuredWMTSLayerInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

@SuppressWarnings("unchecked") // the hamcrest assertions in here are a unchecked warning nightmare
public class SecureCatalogImplTest extends AbstractAuthorizationTest {

    public static final Logger LOGGER = Logging.getLogger(SecureCatalogImplTest.class);

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);
    public static final PropertyName RESOURCE_WS_NAME = FF.property("resource.store.workspace.name");

    @Rule
    public GeoServerExtensionsHelper.ExtensionsHelperRule extensions =
            new GeoServerExtensionsHelper.ExtensionsHelperRule();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        populateCatalog();

        SecurityContextHolder.getContext().setAuthentication(null);
        Dispatcher.REQUEST.remove();
    }

    @After
    public void cleanup() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testWideOpen() throws Exception {
        buildManager("wideOpen.properties");

        // use no user at all
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertSame(cascaded, sc.getResourceByName("topp:cascaded", WMSLayerInfo.class));
        assertSame(cascadedWmts, sc.getResourceByName("topp:cascadedWmts", WMTSLayerInfo.class));
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));

        assertThatBoth(
                sc.getFeatureTypes(), sc.list(FeatureTypeInfo.class, Predicates.acceptAll()), equalTo(featureTypes));
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), equalTo(coverages));
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), equalTo(workspaces));
    }

    @Test
    public void testLockedDown() throws Exception {

        buildManager("lockedDown.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        assertNull(sc.getCoverageByName("nurc:arcgrid"));
        assertNull(sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertNull(sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertNull(sc.getWorkspaceByName("topp"));
        assertNull(sc.getDataStoreByName("states"));
        assertNull(sc.getDataStoreByName("roads"));
        assertNull(sc.getCoverageStoreByName("arcGrid"));

        assertThatBoth(sc.getFeatureTypes(), sc.list(FeatureTypeInfo.class, Predicates.acceptAll()), empty());
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), empty());
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), empty());

        // try with write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));

        assertThatBoth(
                sc.getFeatureTypes(), sc.list(FeatureTypeInfo.class, Predicates.acceptAll()), equalTo(featureTypes));
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), equalTo(coverages));
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), equalTo(workspaces));
    }

    @Test
    public void testLockedChallenge() throws Exception {

        buildManager("lockedDownChallenge.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // check a direct access to the data does trigger a security challenge
        try {
            sc.getFeatureTypeByName("topp:states").getFeatureSource(null, null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageByName("nurc:arcgrid").getGridCoverage(null, null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("topp:states", FeatureTypeInfo.class).getFeatureSource(null, null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("nurc:arcgrid", CoverageInfo.class).getGridCoverage(null, null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        sc.getWorkspaceByName("topp");
        try {
            sc.getDataStoreByName("states").getDataStore(null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("roads").getDataStore(null);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageStoreByName("arcGrid").getFormat();
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        // check we still get the lists out so that capabilities can be built

        assertThatBoth(
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                allOf((Matcher) hasSize(featureTypes.size()), (Matcher)
                        everyItem(Matchers.<FeatureTypeInfo>instanceOf(SecuredFeatureTypeInfo.class))));

        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                allOf((Matcher) hasSize(coverages.size()), (Matcher)
                        everyItem(Matchers.<CoverageInfo>instanceOf(SecuredCoverageInfo.class))));

        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), equalTo(workspaces));

        // try with write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));

        assertThatBoth(
                sc.getFeatureTypes(), sc.list(FeatureTypeInfo.class, Predicates.acceptAll()), equalTo(featureTypes));
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), equalTo(coverages));
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), equalTo(workspaces));
    }

    @Test
    public void testLockedMixed() throws Exception {

        buildManager("lockedDownMixed.properties");

        // try with read only user and GetFeatures request
        SecurityContextHolder.getContext().setAuthentication(roUser);
        Request request = org.easymock.EasyMock.createNiceMock(Request.class);
        org.easymock.EasyMock.expect(request.getRequest())
                .andReturn("GetFeatures")
                .anyTimes();
        org.easymock.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);

        // check a direct access does trigger a security challenge
        try {
            sc.getFeatureTypeByName("topp:states");
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageByName("nurc:arcgrid");
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("topp:states", FeatureTypeInfo.class);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("nurc:arcgrid", CoverageInfo.class);
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getWorkspaceByName("topp");
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("states");
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("roads");
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageStoreByName("arcGrid");
            fail("Should have failed with a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        // try with a getCapabilities, make sure the lists are empty
        request = org.easymock.EasyMock.createNiceMock(Request.class);
        org.easymock.EasyMock.expect(request.getRequest())
                .andReturn("GetCapabilities")
                .anyTimes();
        org.easymock.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);

        // check the lists used to build capabilities are empty
        assertThatBoth(sc.getFeatureTypes(), sc.list(FeatureTypeInfo.class, Predicates.acceptAll()), empty());
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), empty());
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), empty());

        // try with write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));

        assertThatBoth(
                sc.getFeatureTypes(), sc.list(FeatureTypeInfo.class, Predicates.acceptAll()), equalTo(featureTypes));
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), equalTo(coverages));
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), equalTo(workspaces));
    }

    @Test
    public void testPublicRead() throws Exception {

        buildManager("publicRead.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
        // .. the following should have been wrapped
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertTrue(sc.getFeatureTypeByName("topp:states") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getResourceByName("topp:states", FeatureTypeInfo.class) instanceof SecuredFeatureTypeInfo);

        assertThatBoth(
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                allOf((Matcher) hasSize(featureTypes.size()), (Matcher)
                        everyItem(Matchers.<FeatureTypeInfo>instanceOf(SecuredFeatureTypeInfo.class))));
        assertThatBoth(sc.getCoverages(), sc.list(CoverageInfo.class, Predicates.acceptAll()), equalTo(coverages));
        assertThatBoth(sc.getWorkspaces(), sc.list(WorkspaceInfo.class, Predicates.acceptAll()), equalTo(workspaces));

        assertNotNull(sc.getLayerByName("topp:states"));
        assertTrue(sc.getLayerByName("topp:states") instanceof SecuredLayerInfo);
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);

        // try with write enabled user (nothing has been wrapped)
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(featureTypes, sc.getFeatureTypes());
        assertEquals(coverages, sc.getCoverages());
        assertEquals(workspaces, sc.getWorkspaces());
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
    }

    @SuppressWarnings("serial")
    @Test
    public void testCatalogFilteredGetLayers() throws Exception {

        CatalogFilterAccessManager filter = new CatalogFilterAccessManager();

        // make a catalog that uses our layers
        Catalog withLayers = new AbstractCatalogDecorator(catalog) {

            @SuppressWarnings("unchecked")
            @Override
            public <T extends CatalogInfo> CloseableIterator<T> list(
                    Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
                return new CloseableIteratorAdapter<>((Iterator<T>) layers.iterator());
            }
        };
        this.catalog = withLayers;
        extensions.singleton("catalog", catalog, Catalog.class);

        // and the secure catalog with the filter
        buildManager("publicRead.properties", filter);

        // base behavior sanity
        assertTrue(layers.size() > 1);
        assertTrue(sc.getLayers().size() > 1);

        // setup a catalog filter that will hide the layer
        // an example of this happening is when the LocalWorkspaceCatalogFilter
        // detects 'LocalLayer.get' contains the local layer
        // the result is it gets filtered out
        filter.setCatalogFilters(Collections.singletonList(new AbstractCatalogFilter() {

            @Override
            public boolean hideLayer(LayerInfo layer) {
                return layer != statesLayer;
            }
        }));

        assertEquals(1, sc.getLayers().size());
        assertEquals(statesLayer.getName(), sc.getLayers().get(0).getName());
    }

    @Test
    public void testCatalogCloseWrappedIterator() throws Exception {
        // create a mock CloseableIterator that expects to be closed
        final CloseableIterator<?> mockIterator = createNiceMock(CloseableIterator.class);
        mockIterator.close();
        expectLastCall().once();
        replay(mockIterator);

        // make a catalog that uses the mock CloseableIterator
        Catalog withLayers = new AbstractCatalogDecorator(catalog) {

            @SuppressWarnings("unchecked")
            @Override
            public <T extends CatalogInfo> CloseableIterator<T> list(
                    Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
                return (CloseableIterator<T>) mockIterator;
            }
        };
        this.catalog = withLayers;
        GeoServerExtensionsHelper.singleton("catalog", catalog, Catalog.class);
        buildManager("publicRead.properties");

        // get the CloseableIterator from SecureCatalogImpl and close it
        @SuppressWarnings("PMD.CloseResource")
        CloseableIterator<LayerInfo> iterator = sc.list(LayerInfo.class, Predicates.acceptAll());
        iterator.close();

        // verify that the mock CloseableIterator was closed
        verify(mockIterator);
    }

    @Test
    public void testComplex() throws Exception {

        buildManager("complex.properties");

        // try with anonymous user
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        // ... roads follows generic ns rule, read only, nobody can write it
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        // ... states requires READER role
        assertNull(sc.getFeatureTypeByName("topp:states"));
        // ... but the datastore is visible since the namespace rules do apply instead
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        // ... landmarks requires WRITER role to be written
        assertTrue(sc.getFeatureTypeByName("topp:landmarks") instanceof SecuredFeatureTypeInfo);
        // ... bases requires one to be in the military
        assertNull(sc.getFeatureTypeByName("topp:bases"));

        // ok, let's try the same with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getFeatureTypeByName("topp:states") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getFeatureTypeByName("topp:landmarks") instanceof SecuredFeatureTypeInfo);
        assertNull(sc.getFeatureTypeByName("topp:bases"));

        // now with the write enabled user
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertSame(landmarks, sc.getFeatureTypeByName("topp:landmarks"));
        assertNull(sc.getFeatureTypeByName("topp:bases"));

        // finally let's try the military type
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertTrue(sc.getFeatureTypeByName("topp:roads") instanceof SecuredFeatureTypeInfo);
        assertTrue(sc.getDataStoreByName("roads") instanceof SecuredDataStoreInfo);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        assertTrue(sc.getDataStoreByName("states") instanceof SecuredDataStoreInfo);
        assertTrue(sc.getFeatureTypeByName("topp:landmarks") instanceof SecuredFeatureTypeInfo);
        // ... bases requires one to be in the military
        assertSame(bases, sc.getFeatureTypeByName("topp:bases"));
    }

    @Test
    public void testLockedLayerInGroupMustNotHideGroup() throws Exception {

        buildManager("lockedLayerInLayerGroup.properties");

        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(roads, sc.getFeatureTypeByName("topp:roads"));
        LayerGroupInfo layerGroup = sc.getLayerGroupByName("topp", "layerGroupWithSomeLockedLayer");
        assertEquals(2, layerGroup.getLayers().size());

        // try with read-only user, not empty LayerGroup should be returned
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        assertSame(roads, sc.getFeatureTypeByName("topp:roads"));
        layerGroup = sc.getLayerGroupByName("topp", "layerGroupWithSomeLockedLayer");
        assertNotNull(layerGroup);
        assertTrue(layerGroup instanceof SecuredLayerGroupInfo);
        assertEquals(1, layerGroup.getLayers().size());

        // try with anonymous user, empty LayerGroup should be returned
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        assertNull(sc.getFeatureTypeByName("topp:roads"));
        layerGroup = sc.getLayerGroupByName("topp", "layerGroupWithSomeLockedLayer");
        assertNotNull(layerGroup);
        assertTrue(layerGroup instanceof SecuredLayerGroupInfo);
        assertEquals(0, layerGroup.getLayers().size());
    }

    @Test
    public void testDisabledLayerGroup() throws Exception {
        CatalogFilterAccessManager manager = new CatalogFilterAccessManager();
        buildManager("publicRead.properties", manager);
        manager.setCatalogFilters(Arrays.asList(new DisabledResourceFilter()));

        assertFalse(namedTreeB.isEnabled());
        Request request = org.easymock.EasyMock.createNiceMock(Request.class);
        org.easymock.EasyMock.expect(request.getRequest())
                .andReturn("GetCapabilities")
                .anyTimes();
        org.easymock.EasyMock.expect(request.getService()).andReturn("WMS").anyTimes();
        org.easymock.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);
        // buildManager("lockedLayerInLayerGroup.properties");
        assertNull(sc.getLayerGroupByName(namedTreeB.getName()));
    }

    @Test
    public void testEoLayerGroupMustBeHiddenIfItsRootLayerIsHidden() throws Exception {
        LayerGroupInfo eoRoadsLayerGroup =
                buildEOLayerGroup("eoRoadsLayerGroup", roadsLayer, lineStyle, toppWs, statesLayer);
        LayerGroupInfo eoStatesLayerGroup =
                buildEOLayerGroup("eoStatesLayerGroup", statesLayer, lineStyle, toppWs, roadsLayer);

        Catalog eoCatalog = createNiceMock(Catalog.class);
        expect(eoCatalog.getLayerGroupByName("topp", eoRoadsLayerGroup.getName()))
                .andReturn(eoRoadsLayerGroup)
                .anyTimes();
        expect(eoCatalog.getLayerGroupByName("topp", eoStatesLayerGroup.getName()))
                .andReturn(eoStatesLayerGroup)
                .anyTimes();
        expect(eoCatalog.getLayerGroups()).andReturn(Arrays.asList(eoRoadsLayerGroup, eoStatesLayerGroup));
        expect(eoCatalog.list(eq(LayerGroupInfo.class), anyObject(Filter.class)))
                .andReturn(new CloseableIteratorAdapter<>(Collections.emptyIterator()))
                .anyTimes();
        replay(eoCatalog);
        this.catalog = eoCatalog;
        extensions.singleton("catalog", eoCatalog, Catalog.class);

        buildManager("lockedLayerInLayerGroup.properties");
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // if root layer is not hidden
        LayerGroupInfo layerGroup = sc.getLayerGroupByName("topp", "eoRoadsLayerGroup");
        assertNotNull(layerGroup);
        assertNotNull(layerGroup.getRootLayer());

        // if root layer is hidden
        layerGroup = sc.getLayerGroupByName("topp", "eoStatesLayerGroup");
        assertNull(layerGroup);
    }

    @Test
    public void testSecurityFilterWideOpen() throws Exception {
        // getting the resourceAccessManager
        ResourceAccessManager resourceManager = buildAccessManager("wideOpen.properties");

        // Workspace test
        Class<? extends CatalogInfo> clazz = WorkspaceInfo.class;
        // Creating filter for anonymous user
        Filter security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        Filter security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Since we should see all the CatalogInfo elements, we should have an include filter
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);

        // PublishedInfo test
        clazz = PublishedInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Since we should see all the CatalogInfo elements, we should have an include filter
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);

        // Style test
        clazz = StyleInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Since we should see all the CatalogInfo elements, we should have an include filter
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);

        // Resource test
        clazz = ResourceInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Since we should see all the CatalogInfo elements, we should have an include filter
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);

        // Coverage
        clazz = CoverageInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Since we should see all the CatalogInfo elements, we should have an include filter
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);
    }

    @Test
    public void testSecurityFilterLockedDown() throws Exception {
        // getting the resourceAccessManager
        ResourceAccessManager resourceManager = buildAccessManager("lockedDown.properties");

        // Workspace test
        Class<? extends CatalogInfo> clazz = WorkspaceInfo.class;
        // Creating filter for anonymous user
        Filter security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        Filter security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        Filter security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we cannot see the CatalogInfo elements, we should have an exclude filter
        // for all the users except those having WRITER role
        assertSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);

        // PublishedInfo test
        clazz = PublishedInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we cannot see the CatalogInfo elements, we should have an exclude filter
        // for all the users except those having WRITER role
        assertSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);

        // Style test
        clazz = StyleInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we cannot see the CatalogInfo elements, we should have an exclude filter
        // for all the users except those having WRITER role
        assertSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);

        // Resource test
        clazz = ResourceInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we cannot see the CatalogInfo elements, we should have an exclude filter
        // for all the users except those having WRITER role
        assertSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);

        // Coverage
        clazz = CoverageInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we cannot see the CatalogInfo elements, we should have an exclude filter
        // for all the users except those having WRITER role
        assertSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);
    }

    @Test
    public void testSecurityFilterWsLock() throws Exception {
        // getting the resourceAccessManager
        ResourceAccessManager resourceManager = buildAccessManager("wsLock.properties");

        // Workspace test
        Class<? extends CatalogInfo> clazz = WorkspaceInfo.class;
        // Creating filter for anonymous user
        Filter security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        Filter security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        Filter security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since only military role can see the topp WorkSpace, we should have a more complex filter
        // for all the users except those having military role
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertNotSame(security3, Filter.INCLUDE);
        assertNotSame(security3, Filter.EXCLUDE);
        // Checks on the workspaces
        List<WorkspaceInfo> ws = catalog.getWorkspaces();
        Iterator<WorkspaceInfo> it = Iterators.filter(ws.iterator(), new PredicateFilter(security));
        while (it.hasNext()) {
            assertSame(it.next(), nurcWs);
        }
        it = Iterators.filter(ws.iterator(), new PredicateFilter(security3));
        while (it.hasNext()) {
            assertSame(it.next(), nurcWs);
        }

        // PublishedInfo test
        clazz = PublishedInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since only military role can see the topp WorkSpace, we should have a more complex filter
        // for all the users except those having military role
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertNotSame(security3, Filter.INCLUDE);
        assertNotSame(security3, Filter.EXCLUDE);
        // Checks on the workspaces
        List<LayerInfo> ly = catalog.getLayers();
        Iterator<LayerInfo> it1 = Iterators.filter(ly.iterator(), new PredicateFilter(security));
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            String wsName = next.getResource().getNamespace().getName();
            assertTrue(wsName.equalsIgnoreCase("nurc"));
        }
        it1 = Iterators.filter(ly.iterator(), new PredicateFilter(security3));
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            String wsName = next.getResource().getNamespace().getName();
            assertTrue(wsName.equalsIgnoreCase("nurc"));
        }

        // Style test
        clazz = StyleInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since only military role can see the topp WorkSpace, we should have a more complex filter
        // for all the users except those having military role
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertNotSame(security3, Filter.INCLUDE);
        assertNotSame(security3, Filter.EXCLUDE);
        // Checks on the workspaces
        List<StyleInfo> sy = catalog.getStyles();
        Iterator<StyleInfo> it3 = Iterators.filter(sy.iterator(), new PredicateFilter(security));
        while (it3.hasNext()) {
            StyleInfo next = it3.next();
            WorkspaceInfo wsi = next.getWorkspace();
            if (wsi != null) {
                String wsName = wsi.getName();
                assertTrue(wsName.equalsIgnoreCase("nurc"));
            }
        }
        it3 = Iterators.filter(sy.iterator(), new PredicateFilter(security3));
        while (it3.hasNext()) {
            StyleInfo next = it3.next();
            WorkspaceInfo wsi = next.getWorkspace();
            if (wsi != null) {
                String wsName = wsi.getName();
                assertTrue(wsName.equalsIgnoreCase("nurc"));
            }
        }

        // Resource test
        clazz = ResourceInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since only military role can see the topp WorkSpace, we should have a more complex filter
        // for all the users except those having military role
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertNotSame(security3, Filter.INCLUDE);
        assertNotSame(security3, Filter.EXCLUDE);
        // Checks on the workspaces
        List<FeatureTypeInfo> fy = catalog.getFeatureTypes();
        Iterator<FeatureTypeInfo> it4 = Iterators.filter(fy.iterator(), new PredicateFilter(security));
        while (it4.hasNext()) {
            FeatureTypeInfo next = it4.next();
            String name = next.getNamespace().getName();
            assertTrue(name.equalsIgnoreCase("nurc"));
        }
        it4 = Iterators.filter(fy.iterator(), new PredicateFilter(security3));
        while (it4.hasNext()) {
            FeatureTypeInfo next = it4.next();
            String name = next.getNamespace().getName();
            assertTrue(name.equalsIgnoreCase("nurc"));
        }

        // Coverage
        clazz = CoverageInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for military user
        security2 = resourceManager.getSecurityFilter(milUser, clazz);
        // Creating filter for writer role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since only military role can see the topp WorkSpace, we should have a more complex filter
        // for all the users except those having military role
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertNotSame(security3, Filter.INCLUDE);
        assertNotSame(security3, Filter.EXCLUDE);
        // Checks on the workspaces
        List<CoverageInfo> cy = catalog.getCoverages();
        Iterator<CoverageInfo> it5 = Iterators.filter(cy.iterator(), new PredicateFilter(security));
        while (it5.hasNext()) {
            CoverageInfo next = it5.next();
            String name = next.getNamespace().getName();
            assertTrue(name.equalsIgnoreCase("nurc"));
        }
        it5 = Iterators.filter(cy.iterator(), new PredicateFilter(security3));
        while (it5.hasNext()) {
            CoverageInfo next = it5.next();
            String name = next.getNamespace().getName();
            assertTrue(name.equalsIgnoreCase("nurc"));
        }
    }

    @Test
    public void testSecurityFilterLayerLock() throws Exception {
        // getting the resourceAccessManager
        ResourceAccessManager resourceManager = buildAccessManager("layerLock.properties");

        // Workspace test
        Class<? extends CatalogInfo> clazz = WorkspaceInfo.class;
        // Creating filter for anonymous user
        Filter security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read only user
        Filter security2 = resourceManager.getSecurityFilter(roUser, clazz);
        // Creating filter for rw role user
        Filter security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since the restriction is only at layer level, workspaces may be seen without problems
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertSame(security3, Filter.INCLUDE);

        // PublishedInfo test
        clazz = PublishedInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read only user
        security2 = resourceManager.getSecurityFilter(roUser, clazz);
        // Creating filter for rw role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we are restricting access on a single layer, all the users
        // except the rw one will have a more complex filter
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertNotSame(security2, Filter.INCLUDE);
        assertNotSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);
        // Checks on the layers
        List<LayerInfo> ly = catalog.getLayers();
        Iterator<LayerInfo> it1 = Iterators.filter(ly.iterator(), new PredicateFilter(security));
        // Checking if the roads layer is present
        // Ensure the base layer is present
        boolean hasBasesLayer = false;
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            assertNotSame(next, roadsLayer);
            assertNotSame(next, statesLayer);
            hasBasesLayer |= next.equals(basesLayer);
        }
        assertTrue(hasBasesLayer);
        boolean hasRoadsLayer = false;
        hasBasesLayer = false;
        it1 = Iterators.filter(ly.iterator(), new PredicateFilter(security2));
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            assertNotSame(next, statesLayer);
            hasRoadsLayer |= next.equals(roadsLayer);
            hasBasesLayer |= next.equals(basesLayer);
        }
        assertTrue(hasRoadsLayer);
        assertTrue(hasRoadsLayer);

        // Style test
        clazz = StyleInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read only user
        security2 = resourceManager.getSecurityFilter(roUser, clazz);
        // Creating filter for rw role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since the restriction is only at layer level, workspaces may be seen without problems
        assertSame(security, Filter.INCLUDE);
        assertSame(security2, Filter.INCLUDE);
        assertSame(security3, Filter.INCLUDE);

        // Resource test
        clazz = ResourceInfo.class;
        // Creating filter for anonymous user
        security = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read only user
        security2 = resourceManager.getSecurityFilter(roUser, clazz);
        // Creating filter for rw role user
        security3 = resourceManager.getSecurityFilter(rwUser, clazz);
        // Since we are restricting access on a single layer, all the users
        // except the rw one will have a more complex filter
        assertNotSame(security, Filter.INCLUDE);
        assertNotSame(security, Filter.EXCLUDE);
        assertNotSame(security2, Filter.INCLUDE);
        assertNotSame(security2, Filter.EXCLUDE);
        assertSame(security3, Filter.INCLUDE);
        // Checks on the featuretypes
        List<FeatureTypeInfo> fy = catalog.getFeatureTypes();
        Iterator<FeatureTypeInfo> it3 = Iterators.filter(fy.iterator(), new PredicateFilter(security));
        hasBasesLayer = false;
        while (it3.hasNext()) {
            FeatureTypeInfo next = it3.next();
            assertNotSame(next, roads);
            assertNotSame(next, states);
            hasBasesLayer |= next.equals(bases);
        }
        assertTrue(hasBasesLayer);
        hasRoadsLayer = false;
        hasBasesLayer = false;
        it3 = Iterators.filter(fy.iterator(), new PredicateFilter(security2));
        while (it3.hasNext()) {
            FeatureTypeInfo next = it3.next();
            hasRoadsLayer |= next.equals(roads);
            hasBasesLayer |= next.equals(bases);
            assertNotSame(next, states);
        }
        assertTrue(hasBasesLayer);
        assertTrue(hasRoadsLayer);
    }

    @Test
    public void testSecurityFilterComplex() throws Exception {
        // getting the resourceAccessManager
        ResourceAccessManager resourceManager = buildAccessManager("complex.properties");

        // Workspace test
        Class<? extends CatalogInfo> clazz = WorkspaceInfo.class;
        // Creating filter for anonymous user
        Filter securityAnonymous = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read write user
        Filter securityReadWriteUser = resourceManager.getSecurityFilter(rwUser, clazz);
        // Creating filter for military user
        Filter securityMilitary = resourceManager.getSecurityFilter(milUser, clazz);
        // anonymous and military can access only to topp
        assertNotSame(securityAnonymous, Filter.EXCLUDE);
        assertNotSame(securityAnonymous, Filter.INCLUDE);
        assertSame(securityReadWriteUser, Filter.INCLUDE);
        assertNotSame(securityMilitary, Filter.EXCLUDE);
        assertNotSame(securityMilitary, Filter.INCLUDE);
        // Checks on the workspaces
        List<WorkspaceInfo> ws = catalog.getWorkspaces();
        Iterator<WorkspaceInfo> it = Iterators.filter(ws.iterator(), new PredicateFilter(securityAnonymous));
        while (it.hasNext()) {
            assertSame(it.next(), toppWs);
        }
        it = Iterators.filter(ws.iterator(), new PredicateFilter(securityMilitary));
        while (it.hasNext()) {
            assertSame(it.next(), toppWs);
        }

        // PublishedInfo test
        clazz = PublishedInfo.class;
        // Creating filter for anonymous user
        // Anonymous can access to topp layers except for states and bases
        securityAnonymous = resourceManager.getSecurityFilter(anonymous, clazz);
        assertNotSame(securityAnonymous, Filter.INCLUDE);
        assertNotSame(securityAnonymous, Filter.EXCLUDE);

        // Creating filter for read write user
        // Read/Writer can access all layers except for bases and arcgrid
        securityReadWriteUser = resourceManager.getSecurityFilter(rwUser, clazz);
        assertNotSame(securityReadWriteUser, Filter.INCLUDE);
        assertNotSame(securityReadWriteUser, Filter.EXCLUDE);

        // Creating filter for military user
        // Military can access only topp layers except for states and can access to arcgrid
        securityMilitary = resourceManager.getSecurityFilter(milUser, clazz);
        assertNotSame(securityMilitary, Filter.INCLUDE);
        assertNotSame(securityMilitary, Filter.EXCLUDE);

        // Check the layer count does not include the groups. Since the catalog is just a mock, we
        // extract the layers and groups, and do counts using the securityAnonymous filter
        List<LayerInfo> layers = catalog.getLayers();
        List<LayerGroupInfo> layerGroups = catalog.getLayerGroups();
        List<PublishedInfo> publisheds = new ArrayList<>();
        publisheds.addAll(layers);
        publisheds.addAll(layerGroups);
        Collection<LayerInfo> filteredLayers = Collections2.filter(layers, new PredicateFilter(securityAnonymous));
        Collection<LayerGroupInfo> filteredGroups =
                Collections2.filter(layerGroups, new PredicateFilter(securityAnonymous));
        Collection<PublishedInfo> filteredPublished =
                Collections2.filter(publisheds, new PredicateFilter(securityAnonymous));
        assertEquals("anonymous layers", 4, filteredLayers.size());
        assertEquals("anonymous layer groups", 1, filteredGroups.size());
        assertEquals("anonymous published", 5, filteredPublished.size());
        // ANON
        Iterator<LayerInfo> it1 = Iterators.filter(layers.iterator(), new PredicateFilter(securityAnonymous));
        // Boolean checking the various layers
        boolean hasRoadsLayer = false;
        boolean hasLandmLayer = false;
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            // topp
            assertNotSame(
                    "Unexpectedly found bases with securityAnonymous filter " + securityAnonymous, next, basesLayer);
            assertNotSame(
                    "Unexpectedly found states with securityAnonymous filter " + securityAnonymous, next, statesLayer);
            hasLandmLayer |= next.equals(landmarksLayer);
            hasRoadsLayer |= next.equals(roadsLayer);
            // Nurc
            assertNotSame(next, arcGridLayer);
        }
        // We see the roads and landmarks layer
        assertTrue(hasRoadsLayer);
        assertTrue(hasLandmLayer);

        // READER/WRITER
        // Reset boolean
        hasRoadsLayer = false;
        boolean hasStatesLayer = false;
        hasLandmLayer = false;
        it1 = Iterators.filter(layers.iterator(), new PredicateFilter(securityReadWriteUser));
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            // Topp
            assertNotSame(next, basesLayer);
            hasStatesLayer |= next.equals(statesLayer);
            hasLandmLayer |= next.equals(landmarksLayer);
            hasRoadsLayer |= next.equals(roadsLayer);
            // Nurc
            assertNotSame(next, arcGridLayer);
        }
        // We see landmarks,states and roads
        assertTrue(hasLandmLayer);
        assertTrue(hasStatesLayer);
        assertTrue(hasRoadsLayer);

        // MILITARY
        // Reset boolean
        boolean hasArcGridLayer = false;
        boolean hasBasesLayer = false;
        hasLandmLayer = false;
        hasRoadsLayer = false;
        it1 = Iterators.filter(layers.iterator(), new PredicateFilter(securityMilitary));
        while (it1.hasNext()) {
            LayerInfo next = it1.next();
            // Topp
            assertNotSame(next, statesLayer);
            hasLandmLayer |= next.equals(landmarksLayer);
            hasRoadsLayer |= next.equals(roadsLayer);
            hasBasesLayer |= next.equals(basesLayer);
            // Nurc
            hasArcGridLayer |= next.equals(arcGridLayer);
        }
        // We see landmarks,bases,arcgrid and roads
        assertTrue(hasLandmLayer);
        assertTrue(hasBasesLayer);
        assertTrue(hasArcGridLayer);
        assertTrue(hasRoadsLayer);

        // Style test
        clazz = StyleInfo.class;
        // Creating filter for anonymous user
        securityAnonymous = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read write user
        securityReadWriteUser = resourceManager.getSecurityFilter(rwUser, clazz);
        // Creating filter for military user
        securityMilitary = resourceManager.getSecurityFilter(milUser, clazz);
        // anonymous and military can access only to topp
        assertNotSame(securityAnonymous, Filter.EXCLUDE);
        assertNotSame(securityAnonymous, Filter.INCLUDE);
        assertSame(securityReadWriteUser, Filter.INCLUDE);
        assertNotSame(securityMilitary, Filter.EXCLUDE);
        assertNotSame(securityMilitary, Filter.INCLUDE);
        // Checks on the workspaces
        List<StyleInfo> sy = catalog.getStyles();
        Iterator<StyleInfo> it2 = Iterators.filter(sy.iterator(), new PredicateFilter(securityAnonymous));
        while (it2.hasNext()) {
            StyleInfo next = it2.next();
            WorkspaceInfo wsi = next.getWorkspace();
            if (wsi != null) {
                String wsName = wsi.getName();
                assertTrue(wsName.equalsIgnoreCase("topp"));
            }
        }
        it2 = Iterators.filter(sy.iterator(), new PredicateFilter(securityMilitary));
        while (it2.hasNext()) {
            StyleInfo next = it2.next();
            WorkspaceInfo wsi = next.getWorkspace();
            if (wsi != null) {
                String wsName = wsi.getName();
                assertTrue(wsName.equalsIgnoreCase("topp"));
            }
        }

        // Resource test
        clazz = ResourceInfo.class;
        // Creating filter for anonymous user
        securityAnonymous = resourceManager.getSecurityFilter(anonymous, clazz);
        // When the filter gets created, if evaluating a ResourceInfo where there is a mismatch
        // between layer and workspace access, get the resource and get its id rather than the layer
        // id
        // in order to exclude it
        assertTrue(securityAnonymous.toString().contains("[states-id]"));
        assertFalse(securityAnonymous.toString().contains("[states-lid]"));
        // Confirming that the security filter creation process can handle a LayerGroup when there
        // is a layer-workspace mismatch
        assertTrue(securityAnonymous.toString().contains("[topp:layerGroupTopp-id]"));
        // Creating filter for read write user
        securityReadWriteUser = resourceManager.getSecurityFilter(rwUser, clazz);
        // Creating filter for military user
        securityMilitary = resourceManager.getSecurityFilter(milUser, clazz);
        // Anonymous can access to topp layers except for states and bases
        // Read/Writer can access all layers except for bases and arcgrid
        // Military can access only topp layers except for states and can access to arcgrid
        assertNotSame(securityAnonymous, Filter.INCLUDE);
        assertNotSame(securityAnonymous, Filter.EXCLUDE);
        assertNotSame(securityReadWriteUser, Filter.INCLUDE);
        assertNotSame(securityReadWriteUser, Filter.EXCLUDE);
        assertNotSame(securityMilitary, Filter.INCLUDE);
        assertNotSame(securityMilitary, Filter.EXCLUDE);
        // Checks on the featuretypes
        List<FeatureTypeInfo> fy = catalog.getFeatureTypes();
        Iterator<FeatureTypeInfo> it3 = Iterators.filter(fy.iterator(), new PredicateFilter(securityAnonymous));

        // Boolean checking the various layers
        hasRoadsLayer = false;
        hasLandmLayer = false;
        while (it3.hasNext()) {
            FeatureTypeInfo next = it3.next();
            // topp
            assertNotSame(next, bases);
            assertNotSame(next, states);
            hasLandmLayer |= next.equals(landmarks);
            hasRoadsLayer |= next.equals(roads);
            // Nurc
            assertNotSame(next, arcGrid);
        }
        // We see the roads and landmarks layer
        assertTrue(hasRoadsLayer);
        assertTrue(hasLandmLayer);

        // READER/WRITER
        // Reset boolean
        hasRoadsLayer = false;
        hasStatesLayer = false;
        hasLandmLayer = false;
        it3 = Iterators.filter(fy.iterator(), new PredicateFilter(securityReadWriteUser));
        while (it3.hasNext()) {
            FeatureTypeInfo next = it3.next();
            // Topp
            assertNotSame(next, bases);
            hasStatesLayer |= next.equals(states);
            hasLandmLayer |= next.equals(landmarks);
            hasRoadsLayer |= next.equals(roads);
            // Nurc
            assertNotSame(next, arcGrid);
        }
        // We see landmarks,states and roads
        assertTrue(hasLandmLayer);
        assertTrue(hasStatesLayer);
        assertTrue(hasRoadsLayer);

        // MILITARY
        // Reset boolean
        hasBasesLayer = false;
        hasLandmLayer = false;
        hasRoadsLayer = false;
        it3 = Iterators.filter(fy.iterator(), new PredicateFilter(securityMilitary));
        while (it3.hasNext()) {
            FeatureTypeInfo next = it3.next();
            // Topp
            assertNotSame(next, states);
            hasLandmLayer |= next.equals(landmarks);
            hasRoadsLayer |= next.equals(roads);
            hasBasesLayer |= next.equals(bases);
        }
        // We see landmarks,bases and roads
        assertTrue(hasLandmLayer);
        assertTrue(hasBasesLayer);
        assertTrue(hasRoadsLayer);

        // Coverage
        clazz = CoverageInfo.class;
        // Creating filter for anonymous user
        securityAnonymous = resourceManager.getSecurityFilter(anonymous, clazz);
        // Creating filter for read write user
        securityReadWriteUser = resourceManager.getSecurityFilter(rwUser, clazz);
        // Creating filter for military user
        securityMilitary = resourceManager.getSecurityFilter(milUser, clazz);
        // Anonymous can access to topp layers except for states and bases
        // Read/Writer can access all layers except for bases and arcgrid
        // Military can access only topp layers except for states and can access to arcgrid
        assertNotSame(securityAnonymous, Filter.INCLUDE);
        assertNotSame(securityAnonymous, Filter.EXCLUDE);
        assertNotSame(securityReadWriteUser, Filter.INCLUDE);
        assertNotSame(securityReadWriteUser, Filter.EXCLUDE);
        assertNotSame(securityMilitary, Filter.INCLUDE);
        assertNotSame(securityMilitary, Filter.EXCLUDE);

        // Checks on the featuretypes
        List<CoverageInfo> cy = catalog.getCoverages();
        Iterator<CoverageInfo> it4 = Iterators.filter(cy.iterator(), new PredicateFilter(securityAnonymous));

        // Boolean checking the various coverages
        while (it4.hasNext()) {
            CoverageInfo next = it4.next();
            // Nurc
            assertNotSame(next, arcGrid);
        }

        // READER/WRITER
        // Reset boolean
        it4 = Iterators.filter(cy.iterator(), new PredicateFilter(securityReadWriteUser));
        while (it4.hasNext()) {
            CoverageInfo next = it4.next();
            // Nurc
            assertNotSame(next, arcGrid);
        }

        // MILITARY
        // Reset boolean
        hasArcGridLayer = false;
        it4 = Iterators.filter(cy.iterator(), new PredicateFilter(securityMilitary));
        while (it4.hasNext()) {
            CoverageInfo next = it4.next();
            // Nurc
            hasArcGridLayer |= next.equals(arcGrid);
        }
        // We see arcgrid
        assertTrue(hasArcGridLayer);
    }

    static <T> void assertThatBoth(List<T> result1, CloseableIterator<T> result2, Matcher<?> expected)
            throws IOException {
        assertThat(result1, (Matcher<List<T>>) expected);
        assertThat(collectAndClose(result2), (Matcher<List<T>>) expected);
    }

    static <T> List<T> collectAndClose(CloseableIterator<T> it) throws IOException {
        if (it == null) return null;
        try (it) {
            LinkedList<T> list = new LinkedList<>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        }
    }

    static class PredicateFilter implements Predicate<CatalogInfo> {

        private Filter f;

        public PredicateFilter(Filter f) {
            this.f = f;
        }

        @Override
        public boolean apply(@Nullable CatalogInfo input) {
            if (input != null) {
                return f.evaluate(input);
            }
            return false;
        }
    }

    @Test
    public void testUnwrapping() {
        // we create a mock a policy without defining any behavior since it will not be used
        WrapperPolicy policy = createNiceMock(WrapperPolicy.class);
        // test that a secured coverage info info is correctly unwrapped to a coverage info
        assertThat(
                SecureCatalogImpl.unwrap(new SecuredCoverageInfo(arcGrid, policy)),
                not(instanceOf(SecuredCoverageInfo.class)));
        // test that a secured feature info info is correctly unwrapped to a feature info
        assertThat(
                SecureCatalogImpl.unwrap(new SecuredFeatureTypeInfo(states, policy)),
                not(instanceOf(SecuredFeatureTypeInfo.class)));
        // test that a secured WMS layer info info is correctly unwrapped to a WMS layer info
        assertThat(
                SecureCatalogImpl.unwrap(new SecuredWMSLayerInfo(cascaded, policy)),
                not(instanceOf(SecuredWMSLayerInfo.class)));
        // test that a secured WMTS layer info info is correctly unwrapped to a WMTS layer info
        assertThat(
                SecureCatalogImpl.unwrap(new SecuredWMTSLayerInfo(cascadedWmts, policy)),
                not(instanceOf(SecuredWMTSLayerInfo.class)));
    }

    @Test
    public void testSettingResourceOnSecureLayerInfo() {
        // we create a mock a policy without defining any behavior since it will not be used
        WrapperPolicy policy = createNiceMock(WrapperPolicy.class);
        // testing for coverages
        LayerInfo coverageLayerInfo = new LayerInfoImpl();
        SecuredLayerInfo secureCoverageLayerInfo = new SecuredLayerInfo(coverageLayerInfo, policy);
        secureCoverageLayerInfo.setResource(new SecuredCoverageInfo(arcGrid, policy));
        assertThat(coverageLayerInfo.getResource(), not(instanceOf(SecuredCoverageInfo.class)));
        assertThat(coverageLayerInfo.getResource(), instanceOf(CoverageInfo.class));
        // testing for features
        LayerInfo featureLayerInfo = new LayerInfoImpl();
        SecuredLayerInfo secureFeatureLayerInfo = new SecuredLayerInfo(featureLayerInfo, policy);
        secureFeatureLayerInfo.setResource(new SecuredFeatureTypeInfo(states, policy));
        assertThat(featureLayerInfo.getResource(), not(instanceOf(SecuredFeatureTypeInfo.class)));
        assertThat(featureLayerInfo.getResource(), instanceOf(FeatureTypeInfo.class));
        // testing for WMS layers
        LayerInfo wmsLayerInfo = new LayerInfoImpl();
        SecuredLayerInfo secureWmsLayerInfo = new SecuredLayerInfo(wmsLayerInfo, policy);
        secureWmsLayerInfo.setResource(new SecuredWMSLayerInfo(cascaded, policy));
        assertThat(wmsLayerInfo.getResource(), not(instanceOf(SecuredWMSLayerInfo.class)));
        assertThat(wmsLayerInfo.getResource(), instanceOf(WMSLayerInfo.class));
        // testing for WMTS layers
        LayerInfo wmtsLayerInfo = new LayerInfoImpl();
        SecuredLayerInfo secureWmtsLayerInfo = new SecuredLayerInfo(wmtsLayerInfo, policy);
        secureWmtsLayerInfo.setResource(new SecuredWMTSLayerInfo(cascadedWmts, policy));
        assertThat(wmtsLayerInfo.getResource(), not(instanceOf(SecuredWMTSLayerInfo.class)));
        assertThat(wmtsLayerInfo.getResource(), instanceOf(WMTSLayerInfo.class));
    }

    @Test
    public void testWmsNamedTreeAMilitaryOnly() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("namedTreeAMilitaryOnly.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNull(sc.getFeatureTypeByName("topp:states"));
        // cannot see the named tree
        assertNull(sc.getLayerGroupByName(namedTreeA.getName()));
        // only contained in the hidden group and in a "single mode" one
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        // not shared
        assertNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // this layer is contained also in containerTreeB
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        // the other layers in groups are also available
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(nestedContainerE.prefixedName()));
        assertNotNull(sc.getLayerByName(forestsLayer.prefixedName()));
        // check the single group is there but lost the states layer
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(1, securedSingleGroup.layers().size());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());

        // check the mil user sees everything instead
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());
    }

    @Test
    public void testWmsNamedTreeAMilitaryOnlyGroupContents() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("lockDownStates.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        final LayerGroupInfo group = sc.getLayerGroupByName(namedTreeA.getName());
        assertNotNull(group);
        // the group should not contain states any more
        final List<LayerInfo> layers = group.layers();
        assertEquals(2, layers.size());
        final List<StyleInfo> styles = group.styles();
        assertEquals(2, styles.size());
        // check the layers and styles are not mis-aligned
        assertEquals("roads", layers.get(0).getName());
        assertEquals("topp-roads-style", styles.get(0).getName());
        assertEquals("cities", layers.get(1).getName());
        assertEquals("nurc-cities-style", styles.get(1).getName());
    }

    @Test
    public void testWfsNamedTreeAMilitaryOnly() throws Exception {
        // prepare the stage, this time for a WFS test, the containment rules won't apply anymore
        setupRequestThreadLocal("WFS");
        buildManager("namedTreeAMilitaryOnly.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        // cannot see the named tree
        assertNull(sc.getLayerGroupByName(namedTreeA.getName()));
        // only contained in the hidden group and in a "single mode" one
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        // not shared
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // this layer is contained also in containerTreeB
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        // the other layers in groups are also available
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNotNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(nestedContainerE.prefixedName()));
        assertNotNull(sc.getLayerByName(forestsLayer.prefixedName()));
        // check the single group is there but lost the states layer
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());

        // check the mil user sees everything instead
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());
    }

    @Test
    public void testWmsContainerTreeBMilitaryOnly() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("containerTreeGroupBMilitaryOnly.properties");

        // try with read only user, layer group A and its contents should be fine
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B and landmarks should not be accessible
        assertNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the nested group and its sub-layer is also not available
        assertNull(sc.getLayerGroupByName(nestedContainerE.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));

        // check the single group is there and fully available
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());

        // check the mil user sees everything instead
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());
    }

    @Test
    public void testWmsBothGroupABMilitaryOnlyMilitaryOnly() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("bothGroupABMilitaryOnly.properties");

        // try with read only user, layer group A and its contents should not be available
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B and landmarks should not be accessible
        assertNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the nested group and its sub-layer is also not available
        assertNull(sc.getLayerGroupByName(nestedContainerE.prefixedName()));
        assertNull(sc.getLayerByName(forestsLayer.prefixedName()));

        // check the single group is there and states is gone
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(1, securedSingleGroup.layers().size());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());

        // check the mil user sees everything instead
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());
    }

    @Test
    public void testWmsSingleGroupCMilitaryOnly() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("singleGroupCMilitaryOnly.properties");

        // try with read only user, layer group A and its contents should be fine
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B and landmarks should also be accessible
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNotNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // check the single group is not available, but its extra layer is
        assertNull(sc.getLayerGroupByName(singleGroupC.prefixedName()));
        assertNotNull(sc.getLayerByName(basesLayer.prefixedName()));

        // check the mil user sees everything instead
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());
    }

    @Test
    public void testWmsWsContainerGroupDMilitaryOnly() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("wsContainerGroupDMilitaryOnly.properties");

        // try with read only user, layer group A and its contents should be fine
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B and landmarks should also be accessible
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNotNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the single group is  available too
        assertNotNull(sc.getLayerGroupByName(singleGroupC.prefixedName()));
        assertNotNull(sc.getLayerByName(basesLayer.prefixedName()));
        // the ws specific group is not available instead, nor its contained layers
        assertNull(sc.getLayerGroupByName("nurc", "wsContainerD"));
        assertNull(sc.getLayerByName(arcGridLayer.prefixedName()));

        // check the mil user sees everything instead
        SecurityContextHolder.getContext().setAuthentication(milUser);
        assertNotNull(sc.getFeatureTypeByName("topp:states"));
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        LayerGroupInfo securedSingleGroup = sc.getLayerGroupByName(singleGroupC.prefixedName());
        assertNotNull(securedSingleGroup);
        assertEquals(2, securedSingleGroup.layers().size());
        assertEquals(
                statesLayer.prefixedName(), securedSingleGroup.layers().get(0).prefixedName());
        assertEquals(
                basesLayer.prefixedName(), securedSingleGroup.layers().get(1).prefixedName());
        LayerGroupInfo wsSpecificGroup = sc.getLayerGroupByName("nurc", "wsContainerD");
        assertNotNull(wsSpecificGroup);
        assertEquals(1, wsSpecificGroup.getLayers().size());
        assertNotNull(sc.getLayerByName(arcGridLayer.prefixedName()));
    }

    @Test
    public void testWMSLayerGroupAllowsAccess() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("namedTreeAllow.properties");

        // try with read only user, only layer group A and its contents should be visible
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNotNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B should not be accessible
        assertNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the single group not available either
        assertNull(sc.getLayerGroupByName(singleGroupC.prefixedName()));
        assertNull(sc.getLayerByName(basesLayer.prefixedName()));
        // the ws specific group is not available either
        assertNull(sc.getLayerGroupByName("nurc", "wsContainerD"));
        assertNull(sc.getLayerByName(arcGridLayer.prefixedName()));
    }

    @Test
    public void testWMSLayerGroupAllowLayerOverride() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("namedTreeAllowLayerOverride.properties");

        // try with read only user, only layer group A and its contents should be visible, but
        // not topp:states
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B should not be accessible
        assertNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the single group not available either
        assertNull(sc.getLayerGroupByName(singleGroupC.prefixedName()));
        assertNull(sc.getLayerByName(basesLayer.prefixedName()));
        // the ws specific group is not available either
        assertNull(sc.getLayerGroupByName("nurc", "wsContainerD"));
        assertNull(sc.getLayerByName(arcGridLayer.prefixedName()));
    }

    @Test
    public void testWMSLayerGroupAllowWorkspaceOverride() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("namedTreeAllowWorkspaceOverride.properties");

        // try with read only user, only layer group A and its contents should be visible
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNotNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B should not be accessible
        assertNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the single group not available either
        assertNull(sc.getLayerGroupByName(singleGroupC.prefixedName()));
        assertNull(sc.getLayerByName(basesLayer.prefixedName()));
        // the ws specific group is not available either
        assertNull(sc.getLayerGroupByName("nurc", "wsContainerD"));
        assertNull(sc.getLayerByName(arcGridLayer.prefixedName()));
    }

    @Test
    public void testWMSLayerGroupDenyWSAllow() throws Exception {
        // prepare the stage
        setupRequestThreadLocal("WMS");
        buildManager("namedTreeDenyWSAllow.properties");

        // try with read only user, the layer group A is not allowed
        SecurityContextHolder.getContext().setAuthentication(roUser);
        assertNull(sc.getLayerGroupByName(namedTreeA.getName()));
        assertNull(sc.getLayerByName(statesLayer.prefixedName()));
        assertNull(sc.getLayerByName(roadsLayer.prefixedName()));
        // however cities are allowed explicitly because they are in the nurc ws
        assertNotNull(sc.getLayerByName(citiesLayer.prefixedName()));
        // layer group B should not be accessible
        assertNull(sc.getLayerGroupByName(containerTreeB.prefixedName()));
        assertNull(sc.getLayerByName(landmarksLayer.prefixedName()));
        // the single group not available either
        assertNull(sc.getLayerGroupByName(singleGroupC.prefixedName()));
        assertNull(sc.getLayerByName(basesLayer.prefixedName()));
        // the ws specific group is made available by the workspace rule
        assertNotNull(sc.getLayerGroupByName("nurc", "wsContainerD"));
        assertNotNull(sc.getLayerByName(arcGridLayer.prefixedName()));

        // check access is working also when iterating over all groups
        try (CloseableIterator<LayerGroupInfo> groupIterator = sc.list(LayerGroupInfo.class, Filter.INCLUDE)) {
            ImmutableList<LayerGroupInfo> groups = ImmutableList.copyOf(groupIterator);
            assertEquals(1, groups.size());
            assertEquals("wsContainerD", groups.get(0).getName());
        }
    }

    @Test
    public void testLayerPositiveException() throws Exception {
        // prepare the stage
        ResourceAccessManager manager = buildManager("layerPositiveException.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct invocation, state is allowed, others not
        assertNotNull(sc.getLayerByName("topp:states"));
        assertNull(sc.getLayerByName("topp:roads"));

        // now check the security filter
        Filter securityFilter = manager.getSecurityFilter(roUser, LayerInfo.class);
        Filter expected =
                FF.or(FF.not(FF.equal(RESOURCE_WS_NAME, FF.literal("topp"), false)), layerIdFilter("states-lid"));
        assertEquals(expected, securityFilter);
    }

    @Test
    public void testLayerPositiveException2() throws Exception {
        // prepare the stage
        ResourceAccessManager manager = buildManager("layerPositiveException2.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct invocation, state is allowed, others not
        assertNotNull(sc.getLayerByName("topp:states"));
        assertNull(sc.getLayerByName("topp:roads"));

        // now check the security filter
        Filter securityFilter = manager.getSecurityFilter(roUser, LayerInfo.class);
        Filter expected = layerIdFilter("states-lid");
        assertEquals(expected, securityFilter);
    }

    @Test
    public void testLayerNegativeException() throws Exception {
        // prepare the stage
        ResourceAccessManager manager = buildManager("layerNegativeException.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct invocation, state is not allowed, others are
        assertNull(sc.getLayerByName("topp:states"));
        assertNotNull(sc.getLayerByName("topp:roads"));

        // now check the security filter
        Filter securityFilter = manager.getSecurityFilter(roUser, LayerInfo.class);
        Filter expected =
                FF.and((FF.equal(RESOURCE_WS_NAME, FF.literal("topp"), false)), FF.not(layerIdFilter("states-lid")));
        assertEquals(expected, securityFilter);
    }

    @Test
    public void testLayerNegativeException2() throws Exception {
        // prepare the stage
        ResourceAccessManager manager = buildManager("layerNegativeException2.properties");

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // direct invocation, state is not allowed, others are
        assertNull(sc.getLayerByName("topp:states"));
        assertNotNull(sc.getLayerByName("topp:roads"));

        // now check the security filter
        Filter securityFilter = manager.getSecurityFilter(roUser, LayerInfo.class);
        Filter expected = FF.not(layerIdFilter("states-lid"));
        assertEquals(expected, securityFilter);
    }

    /**
     * Builds a filter matching a single identifier using the "in" function (same way the DefaultResourceAccessManager
     * builds a filter)
     */
    private static PropertyIsEqualTo layerIdFilter(String id) {
        return FF.equal(FF.function("in", FF.property("id"), FF.literal(id)), FF.literal(true), false);
    }
}
