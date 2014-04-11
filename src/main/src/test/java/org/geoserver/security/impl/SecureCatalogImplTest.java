/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AbstractCatalogDecorator;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AbstractCatalogFilter;
import org.geoserver.security.CatalogFilterAccessManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.ResourceAccessManagerWrapper;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.decorators.ReadOnlyDataStoreTest;
import org.geoserver.security.decorators.SecuredCoverageInfo;
import org.geoserver.security.decorators.SecuredDataStoreInfo;
import org.geoserver.security.decorators.SecuredFeatureTypeInfo;
import org.geoserver.security.decorators.SecuredLayerGroupInfo;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecureCatalogImplTest extends AbstractAuthorizationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        populateCatalog();
        
        Dispatcher.REQUEST.remove();
    }
    
    SecureCatalogImpl buildTestObject(String propertyFile, Catalog catalog) throws Exception {
        return buildTestObject(propertyFile, catalog, null);
    }
    
    @SuppressWarnings("serial")
    SecureCatalogImpl buildTestObject(String propertyFile, Catalog catalog, ResourceAccessManagerWrapper wrapper) throws Exception{
        // hack to override the getSecurityWrapper method on the access manager to return the 
        // securecatalog that itself requires the resourcemanager before being created.
        // Outside of testing, this is handled using GeoServerExtensions.bean
        SecureCatalogImpl sc;
        final SecureCatalogImpl[] scHolder =  new SecureCatalogImpl[1];
        ResourceAccessManager manager = buildManager(propertyFile, new IAnswer<SecureCatalogImpl>(){

            @Override
            public SecureCatalogImpl answer() throws Throwable {
                return scHolder[0];
            }
            
        });
        
        if(wrapper!=null) {
            wrapper.setDelegate(manager);
            manager=wrapper;
        }
        
        sc = new SecureCatalogImpl(catalog, manager) {

            @Override
            protected boolean isAdmin(Authentication authentication) {
                return false;
            }
            
        };
        
        scHolder[0]=sc;
        return sc;
    }
    
    @Test 
    public void testWideOpen() throws Exception {
        SecureCatalogImpl sc = buildTestObject("wideOpen.properties", catalog);
        
        // use no user at all
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(arcGrid, sc.getCoverageByName("nurc:arcgrid"));
        assertSame(states, sc.getResourceByName("topp:states", FeatureTypeInfo.class));
        assertSame(arcGrid, sc.getResourceByName("nurc:arcgrid", CoverageInfo.class));
        assertEquals(toppWs, sc.getWorkspaceByName("topp"));
        assertSame(statesStore, sc.getDataStoreByName("states"));
        assertSame(roadsStore, sc.getDataStoreByName("roads"));
        assertSame(arcGridStore, sc.getCoverageStoreByName("arcGrid"));
        
        assertThatBoth(
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                equalTo(featureTypes));
        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                equalTo(coverages));
        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                equalTo(workspaces));
    }

    @Test
    public void testLockedDown() throws Exception {
        
        SecureCatalogImpl sc = buildTestObject("lockedDown.properties", catalog);
        
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
        
        assertThatBoth(
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                empty());
        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                empty());
        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                empty());

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
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                equalTo(featureTypes));
        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                equalTo(coverages));
        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                equalTo(workspaces));
    }
    
    @Test
    public void testLockedChallenge() throws Exception {

        SecureCatalogImpl sc = buildTestObject("lockedDownChallenge.properties", catalog);

        // try with read only user
        SecurityContextHolder.getContext().setAuthentication(roUser);

        // check a direct access to the data does trigger a security challenge
        try {
            sc.getFeatureTypeByName("topp:states").getFeatureSource(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageByName("nurc:arcgrid").getGridCoverage(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");

        }
        try {
            sc.getResourceByName("topp:states", FeatureTypeInfo.class).getFeatureSource(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("nurc:arcgrid", CoverageInfo.class).getGridCoverage(null, null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        sc.getWorkspaceByName("topp");
        try {
            sc.getDataStoreByName("states").getDataStore(null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("roads").getDataStore(null);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageStoreByName("arcGrid").getFormat();
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        
        // check we still get the lists out so that capabilities can be built
        
        assertThatBoth(
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                
                allOf(hasSize(featureTypes.size()),
                  everyItem(Matchers.<FeatureTypeInfo>instanceOf(SecuredFeatureTypeInfo.class))));

        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
               
                allOf(hasSize(coverages.size()),
                  everyItem(Matchers.<CoverageInfo>instanceOf(SecuredCoverageInfo.class))));

        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                
                equalTo(workspaces));

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
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                equalTo(featureTypes));
        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                equalTo(coverages));
        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                equalTo(workspaces));
    }
    
    @Test
    public void testLockedMixed() throws Exception {
        
        SecureCatalogImpl sc = buildTestObject("lockedDownMixed.properties", catalog);

        // try with read only user and GetFeatures request
        SecurityContextHolder.getContext().setAuthentication(roUser);
        Request request = org.easymock.classextension.EasyMock.createNiceMock(Request.class);
        org.easymock.classextension.EasyMock.expect(request.getRequest()).andReturn("GetFeatures").anyTimes();
        org.easymock.classextension.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);

        // check a direct access does trigger a security challenge
        try {
            sc.getFeatureTypeByName("topp:states");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageByName("nurc:arcgrid");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("topp:states", FeatureTypeInfo.class);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getResourceByName("nurc:arcgrid", CoverageInfo.class);
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getWorkspaceByName("topp");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("states");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getDataStoreByName("roads");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        try {
            sc.getCoverageStoreByName("arcGrid");
            fail("Should have failed with a security exception");
        } catch(Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e)==false)
                fail("Should have failed with a security exception");
        }
        
        // try with a getCapabilities, make sure the lists are empty
        request = org.easymock.classextension.EasyMock.createNiceMock(Request.class);
        org.easymock.classextension.EasyMock.expect(request.getRequest()).andReturn("GetCapabilities").anyTimes();
        org.easymock.classextension.EasyMock.replay(request);
        Dispatcher.REQUEST.set(request);
        
        // check the lists used to build capabilities are empty
        assertThatBoth(
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                empty());
        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                empty());
        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                empty());
        
        

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
                sc.getFeatureTypes(),
                sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
                equalTo(featureTypes));
        assertThatBoth(
                sc.getCoverages(),
                sc.list(CoverageInfo.class, Predicates.acceptAll()),
                equalTo(coverages));
        assertThatBoth(
                sc.getWorkspaces(),
                sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
                equalTo(workspaces));
    }

    @Test
    public void testPublicRead() throws Exception {
        
        SecureCatalogImpl sc = buildTestObject("publicRead.properties", catalog);

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
        
        assertThatBoth(sc.getFeatureTypes(),
              sc.list(FeatureTypeInfo.class, Predicates.acceptAll()),
              allOf(hasSize(featureTypes.size()),
                everyItem(Matchers.<FeatureTypeInfo>instanceOf(SecuredFeatureTypeInfo.class))));
        assertThatBoth(sc.getCoverages(),
              sc.list(CoverageInfo.class, Predicates.acceptAll()),
              equalTo(coverages));
        assertThatBoth(sc.getWorkspaces(),
              sc.list(WorkspaceInfo.class, Predicates.acceptAll()),
              equalTo(workspaces));
       
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
            public <T extends CatalogInfo> CloseableIterator<T> list(Class<T> of, Filter filter, Integer offset, Integer count, SortBy sortBy) {
                return new CloseableIteratorAdapter<T>((Iterator<T>) layers.iterator());
            }
        };

        // and the secure catalog with the filter
        SecureCatalogImpl sc = this.buildTestObject("publicRead.properties", withLayers, filter);

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
    public void testComplex() throws Exception {
        
        SecureCatalogImpl sc = buildTestObject("complex.properties", catalog);

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
        
        SecureCatalogImpl sc = buildTestObject("lockedLayerInLayerGroup.properties", catalog);
        
        
        SecurityContextHolder.getContext().setAuthentication(rwUser);
        assertSame(states, sc.getFeatureTypeByName("topp:states"));
        assertSame(roads, sc.getFeatureTypeByName("topp:roads"));
        LayerGroupInfo layerGroup = sc.getLayerGroupByName("topp", "layerGroupWithSomeLockedLayer");        
        assertSame(layerGroupWithSomeLockedLayer, layerGroup);
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
    public void testEoLayerGroupMustBeHiddenIfItsRootLayerIsHidden() throws Exception {
        LayerGroupInfo eoRoadsLayerGroup = buildLayerGroup("eoRoadsLayerGroup", LayerGroupInfo.Mode.EO, roadsLayer, lineStyle, toppWs, statesLayer);
        LayerGroupInfo eoStatesLayerGroup = buildLayerGroup("eoStatesLayerGroup", LayerGroupInfo.Mode.EO, statesLayer, lineStyle, toppWs, roadsLayer);
        
        Catalog eoCatalog = createNiceMock(Catalog.class);
        expect(eoCatalog.getLayerGroupByName("topp", eoRoadsLayerGroup.getName())).andReturn(eoRoadsLayerGroup).anyTimes();
        expect(eoCatalog.getLayerGroupByName("topp", eoStatesLayerGroup.getName())).andReturn(eoStatesLayerGroup).anyTimes();        
        replay(eoCatalog);
        
        SecureCatalogImpl sc = this.buildTestObject("lockedLayerInLayerGroup.properties", eoCatalog);
        SecurityContextHolder.getContext().setAuthentication(roUser);
        
        // if root layer is not hidden
        LayerGroupInfo layerGroup = sc.getLayerGroupByName("topp", "eoRoadsLayerGroup");                
        assertNotNull(layerGroup);
        assertNotNull(layerGroup.getRootLayer());
        
        // if root layer is hidden
        layerGroup = sc.getLayerGroupByName("topp", "eoStatesLayerGroup");                
        assertNull(layerGroup);        
    }
    
    static <T> void assertThatBoth(List<T> result1, CloseableIterator<T> result2, Matcher<? super List<T>> expected) throws IOException {
        assertThat(result1, expected);
        assertThat(collectAndClose(result2), expected);
    }
    
    static <T> List<T> collectAndClose(CloseableIterator<T> it) throws IOException {
        if(it==null) return null;
        try {
            LinkedList<T> list = new LinkedList<T>();
            while(it.hasNext()) {
                list.add(it.next());
            }
            return list;
        } finally {
            it.close();
        }
    }

}
