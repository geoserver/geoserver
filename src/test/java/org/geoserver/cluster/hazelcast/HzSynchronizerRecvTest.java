package org.geoserver.cluster.hazelcast;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.*;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.cluster.Event;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.junit.Test;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;


public abstract class HzSynchronizerRecvTest extends HzSynchronizerTest {

    protected abstract void expectationTestDisableLayer(LayerInfo info, String layerName, String layerId) throws Exception;

    @Test
    public void testDisableLayer() throws Exception {
        LayerInfo info;
        final String layerName = "testLayer";
        final String layerId = "Layer-TEST";
        final String layerWorkspace = null; // LayerInfo doesn't have a workspace property
        
        {
            info = createMock(LayerInfo.class);
    
            expect(info.getName()).andStubReturn(layerName);
            expect(info.getId()).andStubReturn(layerId);
            
            expectationTestDisableLayer(info, layerName, layerId);
        }
        replay(info);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt = new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evt);
        }
        waitForSync();
        verify(info);
    }
    
    protected abstract void expectationTestStoreDelete(DataStoreInfo info, String StoreName, String storeId, Class clazz) throws Exception;

    @Test
    public void testStoreDelete() throws Exception {
        DataStoreInfo info;
        WorkspaceInfo wsInfo;
        final String storeName = "testStore";
        final String storeId = "Store-TEST";
        final String storeWorkspace = "Workspace-TEST";
        
        {
            info = createMock(DataStoreInfo.class);
            wsInfo = createMock(WorkspaceInfo.class);
    
            expect(info.getName()).andStubReturn(storeName);
            expect(info.getId()).andStubReturn(storeId);
            expect(info.getWorkspace()).andStubReturn(wsInfo);
            
            expect(wsInfo.getId()).andStubReturn(storeWorkspace);
            
            expectationTestStoreDelete(info, storeName, storeId, DataStoreInfo.class);
        }
        replay(info, wsInfo);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt = new ConfigChangeEvent(storeId, storeName, DataStoreInfoImpl.class, Type.REMOVE);
            evt.setWorkspaceId(storeWorkspace);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evt);
        }
        waitForSync();
        verify(info, wsInfo);
    }
    
    protected abstract void expectationTestContactChange(GeoServerInfo info, String storeId) throws Exception;

    @Test
    public void testContactChange() throws Exception {
        GeoServerInfo info;
        final String globalName = null;
        final String globalId = "GeoServer-TEST";
        final String globalWorkspace = null;
        
        {
            info = createMock(GeoServerInfo.class);
    
            expect(info.getId()).andStubReturn(globalId);
            
            expectationTestContactChange(info, globalId);
        }
        replay(info);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt = new ConfigChangeEvent(globalId, null, GeoServerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evt);

        }
        waitForSync();
        verify(info);
    }
    
    protected abstract void expectationTestMultipleChange(GeoServerInfo gsInfo,
            String globalId, LayerInfo layerInfo, String layerId) throws Exception;
    
    @Test
    public void testMultipleChange() throws Exception {
        GeoServerInfo gsInfo;
        final String globalId = "GeoServer-TEST";
        
        LayerInfo layerInfo;
        final String layerName = "testlayer";
        final String layerId = "Layer-TEST";
        
        {
            gsInfo = createMock(GeoServerInfo.class);
            layerInfo = createMock(LayerInfo.class);
    
            expect(gsInfo.getId()).andStubReturn(globalId);
            expect(layerInfo.getId()).andStubReturn(layerId);
            expect(layerInfo.getName()).andStubReturn(layerName);
            
            expectationTestMultipleChange(gsInfo, globalId, layerInfo, layerId);
        }
        replay(gsInfo, layerInfo);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtGs = new ConfigChangeEvent(globalId, null, GeoServerInfoImpl.class, Type.MODIFY);
            ConfigChangeEvent evtLayer = new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evtGs);
            mockMessage(evtGs);
            mockMessage(evtGs);
            mockMessage(evtLayer);
            mockMessage(evtLayer);
            mockMessage(evtGs);
            
            waitForSync();
            
            mockMessage(evtLayer);
            mockMessage(evtLayer);
            
            waitForSync();
        }
        verify(gsInfo, layerInfo);
    }
    
    protected abstract void expectationTestTwoAddressChangeNoPause(GeoServerInfo gsInfo,
            String globalId) throws Exception;
    
    @Test
    public void testTwoAddressChangeNoPause() throws Exception {
        GeoServerInfo gsInfo;
        final String globalId = "GeoServer-TEST";
        
        {
            gsInfo = createMock(GeoServerInfo.class);
            
            expect(gsInfo.getId()).andStubReturn(globalId);
            
            expectationTestTwoAddressChangeNoPause(gsInfo, globalId);
        }
        replay(gsInfo);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtGs = new ConfigChangeEvent(globalId, null, GeoServerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evtGs);
            mockMessage(evtGs);
            
            waitForSync();
        }
        verify(gsInfo);
    }
    
    protected abstract void expectationTestTwoAddressChangeWithPause(GeoServerInfo gsInfo,
            String globalId) throws Exception;
    
    @Test
    public void testTwoAddressChangeWithPause() throws Exception {
        GeoServerInfo gsInfo;
        final String globalId = "GeoServer-TEST";
        
        {
            gsInfo = createMock(GeoServerInfo.class);
            
            expect(gsInfo.getId()).andStubReturn(globalId);
            
            expectationTestTwoAddressChangeWithPause(gsInfo, globalId);
        }
        replay(gsInfo);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtGs = new ConfigChangeEvent(globalId, null, GeoServerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evtGs);
            
            waitForSync();
            
            mockMessage(evtGs);
            
            waitForSync();
        }
        verify(gsInfo);
    }
    
    protected abstract void expectationTestTwoLayerChangeNoPause(LayerInfo layerInfo, String layerId) throws Exception;
    
    @Test
    public void testTwoLayerChangeNoPause() throws Exception {
        
        LayerInfo layerInfo;
        final String layerName = "testlayer";
        final String layerId = "Layer-TEST";
        
        {
            layerInfo = createMock(LayerInfo.class);
    
            expect(layerInfo.getId()).andStubReturn(layerId);
            expect(layerInfo.getName()).andStubReturn(layerName);
            
            expectationTestTwoLayerChangeNoPause(layerInfo, layerId);
        }
        replay(layerInfo);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtLayer = new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evtLayer);
            mockMessage(evtLayer);

            waitForSync();
        }
        verify(layerInfo);
    }
    
    protected abstract void expectationTestTwoLayerChangeWithPause(LayerInfo layerInfo, String layerId) throws Exception;
    
    @Test
    public void testTwoLayerChangeWithPause() throws Exception {

        LayerInfo layerInfo;
        final String layerName = "testlayer";
        final String layerId = "Layer-TEST";
        
        {
            layerInfo = createMock(LayerInfo.class);
    
            expect(layerInfo.getId()).andStubReturn(layerId);
            expect(layerInfo.getName()).andStubReturn(layerName);
            
            expectationTestTwoLayerChangeWithPause(layerInfo, layerId);
        }
        replay(layerInfo);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtLayer = new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);
            
            // Mock a message coming in from the cluster
            
            mockMessage(evtLayer);
            waitForSync();
            
            mockMessage(evtLayer);
            waitForSync();
        }
        verify(layerInfo);
    }
 
    protected void mockMessage(ConfigChangeEvent evt) {
        evt.setSource(remoteAddress);
        Message<Event> msg = new Message<Event>(TOPIC_NAME, evt);
        for(MessageListener<Event> listener: captureTopicListener.getValues()){
            listener.onMessage(msg);
        }
    }

}
