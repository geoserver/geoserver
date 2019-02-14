/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.easymock.IAnswer;
import org.easymock.IExpectationSetters;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.hamcrest.integration.EasyMock2Adapter;
import org.junit.Before;
import org.junit.Test;

public class EventHzSynchronizerRecvTest extends HzSynchronizerRecvTest {

    ConfigurationListener configListener;
    CatalogListener catListener;

    @Before
    public void setUpListeners() {
        configListener = createMock(ConfigurationListener.class);
        catListener = createMock(CatalogListener.class);
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

            @Override
            public boolean isStarted() {
                return true;
            }
        };
    }

    @Test
    public void testPublishAck() throws Exception {
        // This is just testStoreDelete with an extra assert to test if the ack was sent
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
        ConfigChangeEvent evt =
                new ConfigChangeEvent(storeId, storeName, DataStoreInfoImpl.class, Type.REMOVE);
        {
            sync = getSynchronizer();
            sync.initialize(configWatcher);
            evt.setWorkspaceId(storeWorkspace);

            // Mock a message coming in from the cluster

            mockMessage(evt);
        }
        waitForSync();

        // Did the ack get sent
        this.assertAcked(evt.getUUID());
        verify(info, wsInfo);
    }

    @Override
    protected void expectationTestDisableLayer(LayerInfo info, String layerName, String id)
            throws Exception {
        expect(getCatalog().getLayer(id)).andReturn(info);
        expectCatalogFire(info, id, Type.MODIFY);
    }

    @Override
    protected void expectationTestStoreDelete(
            DataStoreInfo info, String storeName, String storeId, Class clazz) throws Exception {
        expect(getCatalog().getStore(storeId, clazz))
                .andStubReturn(null); // It's been deleted so return null
        expectCatalogFire(info, storeId, Type.REMOVE);
    }

    @Override
    protected void expectationTestFTDelete(
            FeatureTypeInfo info, String ftName, String ftId, String dsId, Class clazz)
            throws Exception {
        expect(getCatalog().getFeatureType(ftId))
                .andStubReturn(null); // It's been deleted so return null
        final String id = ftId;
        expectCatalogGetListeners();
        CatalogRemoveEvent catEvent =
                (CatalogRemoveEvent) catResEvent(id, dsId); // Want to make sure the DS is included
        catListener.handleRemoveEvent(catEvent);
        expectLastCall();
    }

    @Override
    protected void expectationTestContactChange(GeoServerInfo info, String globalId)
            throws Exception {
        expect(getGeoServer().getGlobal()).andReturn(info);

        // TODO: Expect this instead of mocking ConfigurationListener
        // expect(getGeoServer().fireGlobalPostModified());

        configListener.handlePostGlobalChange((GeoServerInfo) info(globalId));
        expectLastCall();
        expectConfigGetListeners();
    }

    @Override
    public List<Object> myMocks() {
        List<Object> mocks = new ArrayList<Object>();
        mocks.addAll(super.myMocks());
        mocks.add(configListener);
        mocks.add(catListener);
        return mocks;
    }

    @Override
    protected void expectationTestMultipleChange(
            GeoServerInfo gsInfo, String globalId, LayerInfo layerInfo, String layerId)
            throws Exception {

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
    protected void expectationTestTwoAddressChangeNoPause(GeoServerInfo gsInfo, String globalId)
            throws Exception {
        expectationTestContactChange(gsInfo, globalId);
        expectationTestContactChange(gsInfo, globalId);
    }

    @Override
    protected void expectationTestTwoAddressChangeWithPause(GeoServerInfo gsInfo, String globalId)
            throws Exception {
        expectationTestContactChange(gsInfo, globalId);
        expectationTestContactChange(gsInfo, globalId);
    }

    @Override
    protected void expectationTestTwoLayerChangeNoPause(final LayerInfo layerInfo, String layerId)
            throws Exception {
        expect(getCatalog().getLayer(layerId)).andReturn(layerInfo).anyTimes();
        expectCatalogFire(layerInfo, layerId, Type.MODIFY).times(2);
    }

    @Override
    protected void expectationTestTwoLayerChangeWithPause(final LayerInfo layerInfo, String layerId)
            throws Exception {
        expect(getCatalog().getLayer(layerId)).andReturn(layerInfo).anyTimes();
        expectCatalogFire(layerInfo, layerId, Type.MODIFY).times(2);
    }

    @Override
    protected void expectationTestWorkspaceAdd(
            final WorkspaceInfo info, String workspaceName, String workspaceId) throws Exception {
        expect(getCatalog().getWorkspace(workspaceId)).andReturn(info);
        expectCatalogFire(info, workspaceId, Type.ADD);
    }

    @Override
    protected void expectationTestChangeSettings(
            SettingsInfo info, String settingsId, WorkspaceInfo wsInfo, String workspaceId)
            throws Exception {

        // TODO: Expect this instead of mocking ConfigurationListener
        // expect(getGeoServer().fireSettingsPostModified());

        configListener.handleSettingsPostModified((SettingsInfo) info(settingsId));
        expectLastCall();
        expectConfigGetListeners();
    }

    @Override
    protected void expectationTestChangeLogging(LoggingInfo info, String loggingId)
            throws Exception {
        // TODO: Expect this instead of mocking ConfigurationListener
        // expect(getGeoServer().fireLoggingPostModified());

        configListener.handlePostLoggingChange((LoggingInfo) info(loggingId));
        expectLastCall();
        expectConfigGetListeners();
    }

    @Override
    protected void expectationTestChangeService(ServiceInfo info, String serviceId)
            throws Exception {
        // TODO: Expect this instead of mocking ConfigurationListener
        // expect(getGeoServer().fireLoggingPostModified());
        configListener.handlePostServiceChange((ServiceInfo) info(serviceId));
        expectLastCall();
        expectConfigGetListeners();
    }

    protected void expectConfigGetListeners() {
        expect(getGeoServer().getListeners())
                .andStubAnswer(
                        new IAnswer<Collection<ConfigurationListener>>() {

                            @Override
                            public Collection<ConfigurationListener> answer() throws Throwable {
                                return Arrays.asList(sync, configListener);
                            }
                        });
    }

    protected void expectCatalogGetListeners() {
        expect(getCatalog().getListeners())
                .andStubAnswer(
                        new IAnswer<Collection<CatalogListener>>() {

                            @Override
                            public Collection<CatalogListener> answer() throws Throwable {
                                return Arrays.asList(sync, catListener);
                            }
                        });
    }

    CatalogEvent catEvent(final CatalogInfo info) {
        EasyMock2Adapter.adapt(hasProperty("source", is(info)));
        return null;
    }

    CatalogEvent catEvent(final String id) {
        EasyMock2Adapter.adapt(hasProperty("source", hasProperty("id", is(id))));
        return null;
    }
    /**
     * Matches a Catalog Event that has a source with the given ID, and a Store property, the value
     * of which has the given ID.
     *
     * @param id id of the source
     * @param storeId id of the source's store
     */
    CatalogEvent catResEvent(final String id, final String storeId) {
        EasyMock2Adapter.adapt(
                hasProperty(
                        "source",
                        allOf(
                                hasProperty("id", is(id)),
                                hasProperty("store", hasProperty("id", is(storeId))))));
        return null;
    }

    protected IExpectationSetters<Object> expectCatalogFire(
            final CatalogInfo info, final String id, final ConfigChangeEvent.Type type) {
        expectCatalogGetListeners();
        switch (type) {
            case ADD:
                catListener.handleAddEvent((CatalogAddEvent) catEvent(info));
                break;
            case MODIFY:
                catListener.handleModifyEvent((CatalogModifyEvent) catEvent(info));
                break;
            case POST_MODIFY:
                catListener.handlePostModifyEvent((CatalogPostModifyEvent) catEvent(info));
                break;
            case REMOVE:
                catListener.handleRemoveEvent((CatalogRemoveEvent) catEvent(id));
                break;
        }
        return expectLastCall();
    }

    /*    protected IExpectationSetters<Object> expectCatalogFire(final CatalogInfo info, final String id, final ConfigChangeEvent.Type type){
           switch(type) {
           case ADD:
               getCatalog().fireAdded((CatalogInfo)info(id));
               break;
           case MODIFY:
               getCatalog().firePostModified((CatalogInfo)info(id));
               break;
           case REMOVE:
               getCatalog().fireRemoved((CatalogInfo)info(id));
               break;
           }
           return expectLastCall().andAnswer(new IAnswer<Object>() {
               @Override
               public Object answer() throws Throwable {
                   switch(type) {
                   case ADD:
                       CatalogAddEventImpl addEvt = new CatalogAddEventImpl();
                       addEvt.setSource(info);
                       sync.handleAddEvent(addEvt);
                       break;
                   case MODIFY:
                       CatalogPostModifyEventImpl modEvt = new CatalogPostModifyEventImpl();
                       modEvt.setSource(info);
                       sync.handlePostModifyEvent(modEvt);
                       break;
                   case REMOVE:
                       CatalogRemoveEventImpl remEvt = new CatalogRemoveEventImpl();
                       remEvt.setSource(info);
                       sync.handleRemoveEvent(remEvt);
                       break;
                   }
                   return null;
               }});
       }
    */
}
