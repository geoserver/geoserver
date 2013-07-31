package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServerInfo;
import org.hamcrest.integration.EasyMock2Adapter;
import org.junit.Before;

public class EventHzSynchronizerRecvTest extends HzSynchronizerRecvTest {

    ConfigurationListener configListener;
    
    @Before
    public void setUpConfigListener(){
        configListener = createMock(ConfigurationListener.class);
    }

    Info info(String id) {
        EasyMock2Adapter.adapt(hasProperty("id", is(id)));
        return null;
    }

    @Override
    protected HzSynchronizer getSynchronizer() {
        return new EventHzSynchronizer(cluster, getGeoServer()) {

            @Override
            ScheduledExecutorService getNewExecutor() {
                return getMockExecutor();
            }
            
        };
    }
    
    @Override
    protected void expectationTestDisableLayer(LayerInfo info, String layerName, String id) throws Exception {
        expect(getCatalog().getLayer(id) ).andReturn(info);
        getCatalog().firePostModified((CatalogInfo)info(id)); expectLastCall();
        
    }

    @Override
    protected void expectationTestStoreDelete(DataStoreInfo info, String storeName, String storeId, Class clazz)
            throws Exception {
        expect(getCatalog().getStore(storeId, clazz) ).andReturn(info);
        getCatalog().fireRemoved((CatalogInfo)info(storeId)); expectLastCall();
    }

    @Override
    protected void expectationTestContactChange(GeoServerInfo info,
            String globalId) throws Exception {
        expect(getGeoServer().getGlobal()).andReturn(info);
        
        // TODO: Expect this instead of mocking ConfigurationListener
        //expect(getGeoServer().fireGlobalPostModified());
        
        configListener.handlePostGlobalChange((GeoServerInfo)info(globalId));expectLastCall();
        expect(getGeoServer().getListeners()).andReturn(Arrays.asList(configListener));
    }

    @Override
    public List<Object> myMocks() {
        List<Object> mocks = new ArrayList<Object>();
        mocks.addAll(super.myMocks());
        mocks.add(configListener);
        return mocks;
    }

    @Override
    protected void expectationTestMultipleChange(GeoServerInfo gsInfo,
            String globalId, LayerInfo layerInfo, String layerId) throws Exception{
        
        expectationTestContactChange(gsInfo, globalId);
        expectationTestContactChange(gsInfo, globalId);
        expectationTestContactChange(gsInfo, globalId);
        expectationTestDisableLayer(layerInfo, null, layerId);
        expectationTestDisableLayer(layerInfo, null, layerId);
        expectationTestContactChange(gsInfo, globalId);
        expectationTestDisableLayer(layerInfo, null, layerId);
        expectationTestDisableLayer(layerInfo, null, layerId);
    }

    @Override
    protected void expectationTestTwoAddressChangeNoPause(GeoServerInfo gsInfo,
            String globalId) throws Exception {
        expectationTestContactChange(gsInfo, globalId);
        expectationTestContactChange(gsInfo, globalId);
    }

    @Override
    protected void expectationTestTwoAddressChangeWithPause(
            GeoServerInfo gsInfo, String globalId) throws Exception {
        expectationTestContactChange(gsInfo, globalId);
        expectationTestContactChange(gsInfo, globalId);
    }

    @Override
    protected void expectationTestTwoLayerChangeNoPause(LayerInfo layerInfo,
            String layerId) throws Exception {
        expect(getCatalog().getLayer(layerId) ).andReturn(layerInfo).anyTimes();
        getCatalog().firePostModified((CatalogInfo)info(layerId)); expectLastCall().times(2);
    }

    @Override
    protected void expectationTestTwoLayerChangeWithPause(LayerInfo layerInfo,
            String layerId) throws Exception {
        expect(getCatalog().getLayer(layerId) ).andReturn(layerInfo).anyTimes();
        getCatalog().firePostModified((CatalogInfo)info(layerId)); expectLastCall().times(2);
    }
}
