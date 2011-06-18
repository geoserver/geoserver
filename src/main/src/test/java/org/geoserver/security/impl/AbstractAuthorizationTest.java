package org.geoserver.security.impl;

import static org.easymock.EasyMock.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.DataAccessManagerAdapter;
import org.geoserver.security.ResourceAccessManager;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureStore;
import org.geotools.factory.Hints;
import org.opengis.util.ProgressListener;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.TestingAuthenticationToken;

public abstract class AbstractAuthorizationTest extends SecureObjectsTest {

    protected Authentication rwUser;

    protected Authentication roUser;

    protected Authentication anonymous;

    protected Authentication milUser;
    
    protected TestingAuthenticationToken root;

    protected Catalog catalog;

    protected WorkspaceInfo toppWs;

    protected WorkspaceInfo nurcWs;

    protected LayerInfo statesLayer;

    protected LayerInfo landmarksLayer;

    protected LayerInfo basesLayer;

    protected LayerInfo arcGridLayer;

    protected LayerInfo roadsLayer;

    protected FeatureTypeInfo states;

    protected CoverageInfo arcGrid;

    protected FeatureTypeInfo roads;

    protected FeatureTypeInfo landmarks;

    protected FeatureTypeInfo bases;

    protected DataStoreInfo statesStore;

    protected DataStoreInfo roadsStore;

    protected CoverageStoreInfo arcGridStore;

    protected List<LayerInfo> layers;

    protected List<FeatureTypeInfo> featureTypes;

    protected List<CoverageInfo> coverages;

    protected List<WorkspaceInfo> workspaces;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        rwUser = new TestingAuthenticationToken("rw", "supersecret", new GrantedAuthority[] {
                new GrantedAuthorityImpl("READER"), new GrantedAuthorityImpl("WRITER") });
        roUser = new TestingAuthenticationToken("ro", "supersecret",
                new GrantedAuthority[] { new GrantedAuthorityImpl("READER") });
        anonymous = new TestingAuthenticationToken("anonymous", null);
        milUser = new TestingAuthenticationToken("military", "supersecret",
                new GrantedAuthority[] { new GrantedAuthorityImpl("MILITARY") });
        root = new TestingAuthenticationToken("admin", "geoserver", new GrantedAuthority[] { new GrantedAuthorityImpl(SecureTreeNode.ROOT_ROLE) });

        catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspace((String) anyObject())).andReturn(
                createNiceMock(WorkspaceInfo.class)).anyTimes();
        replay(catalog);

        toppWs = createNiceMock(WorkspaceInfo.class);
        expect(toppWs.getName()).andReturn("topp").anyTimes();
        replay(toppWs);

        nurcWs = createNiceMock(WorkspaceInfo.class);
        expect(nurcWs.getName()).andReturn("nurc").anyTimes();
        replay(nurcWs);

        statesLayer = buildLayer("states", toppWs, FeatureTypeInfo.class);
        roadsLayer = buildLayer("roads", toppWs, FeatureTypeInfo.class);
        landmarksLayer = buildLayer("landmarks", toppWs, FeatureTypeInfo.class);
        basesLayer = buildLayer("bases", toppWs, FeatureTypeInfo.class);
        // let's add one with a dot inside the name
        arcGridLayer = buildLayer("arc.grid", nurcWs, CoverageInfo.class);

        // resources
        states = (FeatureTypeInfo) statesLayer.getResource();
        statesStore = states.getStore();
        arcGrid = (CoverageInfo) arcGridLayer.getResource();
        arcGridStore = (CoverageStoreInfo) arcGrid.getStore();
        roads = (FeatureTypeInfo) roadsLayer.getResource();
        roadsStore = roads.getStore();
        landmarks = (FeatureTypeInfo) landmarksLayer.getResource();
        bases = (FeatureTypeInfo) basesLayer.getResource();
    }

    protected LayerInfo buildLayer(String name, WorkspaceInfo ws,
            Class<? extends ResourceInfo> resourceClass) throws Exception {
        
        FeatureStore fs = createNiceMock(FeatureStore.class);
        replay(fs);
        
        DataStore dstore = createNiceMock(DataStore.class);
        replay(dstore);
        
        StoreInfo store;
        if (resourceClass.equals(CoverageInfo.class)) {
            store = createNiceMock(CoverageStoreInfo.class);
        } else {
            store = createNiceMock(DataStoreInfo.class);
            expect((DataStore)((DataStoreInfo) store).getDataStore(null)).andReturn(dstore);
        }
        expect(store.getWorkspace()).andReturn(ws).anyTimes();
        replay(store);

        ResourceInfo resource = createNiceMock(resourceClass);
        expect(resource.getStore()).andReturn(store).anyTimes();
        expect(resource.getName()).andReturn(name).anyTimes();
        if (resource instanceof FeatureTypeInfo) {
            expect(
                    ((FeatureTypeInfo) resource).getFeatureSource((ProgressListener) anyObject(),
                            (Hints) anyObject())).andReturn(fs).anyTimes();
        }
        replay(resource);

        LayerInfo layer = createNiceMock(LayerInfo.class);
        expect(layer.getName()).andReturn(name).anyTimes();
        expect(layer.getResource()).andReturn(resource).anyTimes();
        replay(layer);

        return layer;
    }

    protected ResourceAccessManager buildManager(String propertyFile) throws Exception {
        return new DataAccessManagerAdapter(buildLegacyAccessManager(propertyFile));
    }
    
    protected DataAccessManager buildLegacyAccessManager(String propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultDataAccessManager(new MemoryDataAccessRuleDAO(catalog, props));
    }
    
    /**
     * Sets up a mock catalog.
     */
    protected void populateCatalog() {
        // build resource collections
        layers = Arrays.asList(statesLayer, roadsLayer, landmarksLayer, basesLayer, arcGridLayer);
        featureTypes = new ArrayList<FeatureTypeInfo>();
        coverages = new ArrayList<CoverageInfo>();
        for (LayerInfo layer : layers) {
            if (layer.getResource() instanceof FeatureTypeInfo)
                featureTypes.add((FeatureTypeInfo) layer.getResource());
            else
                coverages.add((CoverageInfo) layer.getResource());
        }
        workspaces = Arrays.asList(toppWs, nurcWs);

        // prime the catalog
        catalog = createNiceMock(Catalog.class);
        expect(catalog.getFeatureTypeByName("topp:states")).andReturn((FeatureTypeInfo) states)
                .anyTimes();
        expect(catalog.getResourceByName("topp:states", FeatureTypeInfo.class)).andReturn(
                (FeatureTypeInfo) states).anyTimes();
        expect(catalog.getLayerByName("topp:states")).andReturn(statesLayer).anyTimes();
        expect(catalog.getCoverageByName("nurc:arcgrid")).andReturn((CoverageInfo) arcGrid)
                .anyTimes();
        expect(catalog.getResourceByName("nurc:arcgrid", CoverageInfo.class)).andReturn(
                (CoverageInfo) arcGrid).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:roads")).andReturn((FeatureTypeInfo) roads)
                .anyTimes();
        expect(catalog.getFeatureTypeByName("topp:landmarks")).andReturn(
                (FeatureTypeInfo) landmarks).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:bases")).andReturn((FeatureTypeInfo) bases)
                .anyTimes();
        expect(catalog.getDataStoreByName("states")).andReturn((DataStoreInfo) statesStore)
                .anyTimes();
        expect(catalog.getDataStoreByName("roads")).andReturn((DataStoreInfo) roadsStore)
                .anyTimes();
        expect(catalog.getCoverageStoreByName("arcGrid")).andReturn(
                (CoverageStoreInfo) arcGridStore).anyTimes();
        expect(catalog.getLayers()).andReturn(layers).anyTimes();
        expect(catalog.getFeatureTypes()).andReturn(featureTypes).anyTimes();
        expect(catalog.getCoverages()).andReturn(coverages).anyTimes();
        expect(catalog.getWorkspaces()).andReturn(workspaces).anyTimes();
        expect(catalog.getWorkspaceByName("topp")).andReturn(toppWs).anyTimes();
        expect(catalog.getWorkspaceByName("nurc")).andReturn(nurcWs).anyTimes();
        replay(catalog);
    }
}
