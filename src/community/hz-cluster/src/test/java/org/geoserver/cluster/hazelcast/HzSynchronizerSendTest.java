/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.*;
import static org.geoserver.cluster.ConfigChangeEventMatcher.configChangeEvent;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import org.easymock.IExpectationSetters;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.cluster.ConfigChangeEvent;
import org.geoserver.cluster.ConfigChangeEvent.Type;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServerInfo;
import org.junit.Test;

/**
 * Test that the Synchronizer sends appropriate messages to the shared topic in response to
 * config/catalog changes.
 *
 * @author smithkm
 */
public abstract class HzSynchronizerSendTest extends HzSynchronizerTest {

    IExpectationSetters<Object> expectEvent(
            Object source,
            String name,
            String workspace,
            String id,
            Class<? extends Info> clazz,
            ConfigChangeEvent.Type type) {
        topic.publish(configChangeEvent(localAddress, id, name, workspace, clazz, type));
        return expectLastCall();
    }

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

            expectEvent(
                    localAddress, layerName, layerWorkspace, layerId, LayerInfo.class, Type.MODIFY);
            expectEvent(
                    localAddress,
                    layerName,
                    layerWorkspace,
                    layerId,
                    LayerInfo.class,
                    Type.POST_MODIFY);
        }
        replay(info);
        {
            HzSynchronizer sync = getSynchronizer();

            // Mock the result of doing this:
            // info.setEnabled(false);
            // getCatalog().save(info);

            CatalogModifyEventImpl preEvent = new CatalogModifyEventImpl();

            preEvent.setSource(info);
            preEvent.setPropertyNames(Arrays.asList("enabled"));
            preEvent.setOldValues(Arrays.asList("true"));
            preEvent.setNewValues(Arrays.asList("false"));

            for (CatalogListener listener : catListenerCapture.getValues()) {
                listener.handleModifyEvent(preEvent);
            }

            CatalogPostModifyEventImpl postEvent = new CatalogPostModifyEventImpl();
            postEvent.setSource(info);

            for (CatalogListener listener : catListenerCapture.getValues()) {
                listener.handlePostModifyEvent(postEvent);
            }
        }
        waitForSync();
        verify(info);
    }

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

            expectEvent(
                    localAddress,
                    storeName,
                    storeWorkspace,
                    storeId,
                    DataStoreInfo.class,
                    Type.REMOVE);
        }
        replay(info, wsInfo);
        {
            HzSynchronizer sync = getSynchronizer();

            // Mock the result of doing this:
            // getCatalog().remove(info);

            CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();

            event.setSource(info);

            for (CatalogListener listener : catListenerCapture.getValues()) {
                listener.handleRemoveEvent(event);
            }
        }
        waitForSync();
        verify(info, wsInfo);
    }

    @Test
    public void testContactChange() throws Exception {
        GeoServerInfo info;
        final String globalName = null;
        final String globalId = "GeoServer-TEST";
        final String globalWorkspace = null;

        {
            info = createMock(GeoServerInfo.class);

            expect(info.getId()).andStubReturn(globalId);

            expectEvent(
                    localAddress,
                    globalName,
                    globalWorkspace,
                    globalId,
                    GeoServerInfo.class,
                    Type.MODIFY);
            expectEvent(
                    localAddress,
                    globalName,
                    globalWorkspace,
                    globalId,
                    GeoServerInfo.class,
                    Type.POST_MODIFY);
        }
        replay(info);
        {
            HzSynchronizer sync = getSynchronizer();
            sync.initialize(configWatcher);

            // Mock the result of doing this:
            // GeoServerInfo gsInfo = getGeoServer().getGlobal();;
            // gsInfo.getSettings().getContact().setAddress("42 Test Street");
            // getGeoServer().save(gsInfo);

            for (ConfigurationListener listener : gsListenerCapture.getValues()) {
                listener.handleGlobalChange(
                        info,
                        Arrays.asList("contact.address"),
                        Arrays.<Object>asList("69 Old Avenue"),
                        Arrays.<Object>asList("42 Test Street"));
            }
            for (ConfigurationListener listener : gsListenerCapture.getValues()) {
                listener.handlePostGlobalChange(info);
            }
        }
        waitForSync();
        verify(info);
    }

    @Test
    public void testWorkspaceAdded() throws Exception {
        WorkspaceInfo info;
        final String workspaceName = "testWorkspace";
        final String workspaceId = "Workspace-TEST";
        final String workspaceWorkspace = null; // LayerInfo doesn't have a workspace property

        {
            info = createMock(WorkspaceInfo.class);

            expect(info.getName()).andStubReturn(workspaceName);
            expect(info.getId()).andStubReturn(workspaceId);

            expectEvent(
                    localAddress,
                    workspaceName,
                    workspaceWorkspace,
                    workspaceId,
                    WorkspaceInfo.class,
                    Type.ADD);
        }
        replay(info);
        {
            HzSynchronizer sync = getSynchronizer();

            // Mock the result of doing this:

            // getCatalog().add(info);

            CatalogAddEventImpl preEvent = new CatalogAddEventImpl();

            preEvent.setSource(info);

            for (CatalogListener listener : catListenerCapture.getValues()) {
                listener.handleAddEvent(preEvent);
            }
        }
        waitForSync();
        verify(info);
    }
}
