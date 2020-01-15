/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.easymock.EasyMock;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.cluster.Event;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.Test;

public abstract class HzSynchronizerRecvTest extends HzSynchronizerTest {

    protected abstract void expectationTestDisableLayer(
            LayerInfo info, String layerName, String layerId) throws Exception;

    HzSynchronizer sync;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            sync.start();
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }

        waitForSync();
        verify(info);
    }

    protected abstract void expectationTestStoreDelete(
            DataStoreInfo info, String StoreName, String storeId, Class clazz) throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(storeId, storeName, DataStoreInfoImpl.class, Type.REMOVE);
            evt.setWorkspaceId(storeWorkspace);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info, wsInfo);
    }

    protected abstract void expectationTestFTDelete(
            FeatureTypeInfo info, String ftName, String ftId, String dsId, Class clazz)
            throws Exception;

    @SuppressWarnings("unchecked")
    @Test
    public void testFTDelete() throws Exception {
        FeatureTypeInfo info;
        final String ftName = "testFT";
        final String ftId = "FeatureType-TEST";
        DataStoreInfo dsInfo;
        final String dsName = "testStore";
        final String dsId = "DataStore-TEST";

        {
            dsInfo = createMock(DataStoreInfo.class);
            info = createMock(FeatureTypeInfo.class);

            expect(dsInfo.getName()).andStubReturn(dsName);
            expect(dsInfo.getId()).andStubReturn(dsId);

            expect(info.getName()).andStubReturn(ftName);
            expect(info.getId()).andStubReturn(ftId);
            expect(info.getStore()).andStubReturn(dsInfo);

            expect(
                            catalog.getStore(
                                    EasyMock.eq(dsId),
                                    (Class<DataStoreInfo>) EasyMock.anyObject(Class.class)))
                    .andStubReturn(dsInfo);
            ;

            expectationTestFTDelete(info, ftName, ftId, dsId, FeatureTypeInfo.class);
        }
        replay(info, dsInfo);
        {
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(ftId, ftName, FeatureTypeInfoImpl.class, Type.REMOVE);
            evt.setStoreId(dsId);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info, dsInfo);
    }

    protected abstract void expectationTestContactChange(GeoServerInfo info, String storeId)
            throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(
                            globalId, null, GeoServerInfoImpl.class, Type.POST_MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info);
    }

    protected abstract void expectationTestMultipleChange(
            GeoServerInfo gsInfo, String globalId, LayerInfo layerInfo, String layerId)
            throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtGs =
                    new ConfigChangeEvent(
                            globalId, null, GeoServerInfoImpl.class, Type.POST_MODIFY);
            ConfigChangeEvent evtLayer =
                    new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);

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

    protected abstract void expectationTestTwoAddressChangeNoPause(
            GeoServerInfo gsInfo, String globalId) throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtGs =
                    new ConfigChangeEvent(
                            globalId, null, GeoServerInfoImpl.class, Type.POST_MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evtGs);
            mockMessage(evtGs);

            waitForSync();
        }
        verify(gsInfo);
    }

    protected abstract void expectationTestTwoAddressChangeWithPause(
            GeoServerInfo gsInfo, String globalId) throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtGs =
                    new ConfigChangeEvent(
                            globalId, null, GeoServerInfoImpl.class, Type.POST_MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evtGs);

            waitForSync();

            mockMessage(evtGs);

            waitForSync();
        }
        verify(gsInfo);
    }

    protected abstract void expectationTestTwoLayerChangeNoPause(
            LayerInfo layerInfo, String layerId) throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtLayer =
                    new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evtLayer);
            mockMessage(evtLayer);

            waitForSync();
        }
        verify(layerInfo);
    }

    protected abstract void expectationTestTwoLayerChangeWithPause(
            LayerInfo layerInfo, String layerId) throws Exception;

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
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evtLayer =
                    new ConfigChangeEvent(layerId, layerName, LayerInfoImpl.class, Type.MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evtLayer);
            waitForSync();

            mockMessage(evtLayer);
            waitForSync();
        }
        verify(layerInfo);
    }

    @Test
    public void testWorkspaceAdd() throws Exception {
        WorkspaceInfo info;
        final String workspaceName = "testStore";
        final String workspaceId = "Store-TEST";

        {
            info = createMock(WorkspaceInfo.class);

            expect(info.getName()).andStubReturn(workspaceName);
            expect(info.getId()).andStubReturn(workspaceId);

            expectationTestWorkspaceAdd(info, workspaceName, workspaceId);
        }
        replay(info);
        {
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(
                            workspaceId, workspaceName, WorkspaceInfoImpl.class, Type.ADD);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info);
    }

    protected abstract void expectationTestWorkspaceAdd(
            WorkspaceInfo info, String workspaceName, String workspaceId) throws Exception;

    protected void mockMessage(ConfigChangeEvent evt) {
        evt.setSource(remoteAddress);
        Message<Event> msg = new Message<Event>(TOPIC_NAME, evt, 0, null);
        for (MessageListener<Event> listener : captureTopicListener.getValues()) {
            listener.onMessage(msg);
        }
    }

    @Test
    public void testChangeSettings() throws Exception {
        SettingsInfo info;
        WorkspaceInfo wsInfo;
        final String settingsId = "Settings-TEST";
        final String workspaceId = "Workspace-TEST";

        {
            info = createMock(SettingsInfo.class);
            wsInfo = createMock(WorkspaceInfo.class);

            expect(wsInfo.getId()).andStubReturn(workspaceId);

            expect(info.getWorkspace()).andStubReturn(wsInfo);
            expect(getCatalog().getWorkspace(workspaceId)).andStubReturn(wsInfo);

            expect(info.getId()).andStubReturn(settingsId);

            expect(getGeoServer().getSettings(wsInfo)).andStubReturn(info);

            expectationTestChangeSettings(info, settingsId, wsInfo, workspaceId);
        }
        replay(info, wsInfo);
        {
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(
                            settingsId, null, SettingsInfoImpl.class, Type.POST_MODIFY);
            evt.setWorkspaceId(workspaceId);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info, wsInfo);
    }

    protected abstract void expectationTestChangeSettings(
            SettingsInfo info, String settingsId, WorkspaceInfo wsInfo, String workspaceId)
            throws Exception;

    @Test
    public void testChangeLogging() throws Exception {
        LoggingInfo info;
        final String settingsId = "Logging-TEST";

        {
            info = createMock(LoggingInfo.class);

            expect(info.getId()).andStubReturn(settingsId);

            expect(getGeoServer().getLogging()).andStubReturn(info);

            expectationTestChangeLogging(info, settingsId);
        }
        replay(info);
        {
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            ConfigChangeEvent evt =
                    new ConfigChangeEvent(
                            settingsId, null, LoggingInfoImpl.class, Type.POST_MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info);
    }

    protected abstract void expectationTestChangeLogging(LoggingInfo info, String loggingId)
            throws Exception;

    @Test
    public void testChangeService() throws Exception {
        WMSInfo info;
        final String serviceId = "Service-TEST";

        {
            info = createMock(WMSInfo.class);

            expect(info.getId()).andStubReturn(serviceId);

            expect(getGeoServer().getService(serviceId, WMSInfo.class)).andStubReturn(info);
            expect(getGeoServer().getService(serviceId, ServiceInfo.class)).andStubReturn(info);

            expectationTestChangeService(info, serviceId);
        }
        replay(info);
        {
            sync = getSynchronizer();
            sync.initialize(configWatcher);

            ConfigChangeEvent evt =
                    new ConfigChangeEvent(serviceId, null, WMSInfoImpl.class, Type.POST_MODIFY);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();
        verify(info);
    }

    protected abstract void expectationTestChangeService(ServiceInfo info, String serviceId)
            throws Exception;
}
