/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.expectLastCall;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

public class ReloadHzSynchronizerRecvTest extends HzSynchronizerRecvTest {

    @Override
    protected HzSynchronizer getSynchronizer() {
        return new ReloadHzSynchronizer(cluster, getGeoServer()) {

            @Override
            ScheduledExecutorService getNewExecutor() {
                return getMockExecutor();
            }

            @Override
            BlockingQueue<Runnable> getWorkQueue() {
                return new SynchronousQueue<>();
            }

            @Override
            public boolean isStarted() {
                return true;
            }
        };
    }

    @Override
    protected void expectationTestDisableLayer(LayerInfo info, String layerName, String layerId)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestStoreDelete(
            DataStoreInfo info, String storeName, String storeId, Class clazz) throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestFTDelete(
            FeatureTypeInfo info, String ftName, String ftId, String storeId, Class clazz)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestContactChange(GeoServerInfo info, String storeId)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestMultipleChange(
            GeoServerInfo gsInfo, String globalId, LayerInfo layerInfo, String layerId)
            throws Exception {
        expectGeoServerReload();
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestTwoAddressChangeNoPause(GeoServerInfo gsInfo, String globalId)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestTwoAddressChangeWithPause(GeoServerInfo gsInfo, String globalId)
            throws Exception {
        expectGeoServerReload();
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestTwoLayerChangeNoPause(LayerInfo layerInfo, String layerId)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestTwoLayerChangeWithPause(LayerInfo layerInfo, String layerId)
            throws Exception {
        expectGeoServerReload();
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestWorkspaceAdd(
            WorkspaceInfo info, String workspaceName, String workspaceId) throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestChangeSettings(
            SettingsInfo info, String settingsId, WorkspaceInfo wsInfo, String workspaceId)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestChangeLogging(LoggingInfo info, String loggingId)
            throws Exception {
        expectGeoServerReload();
    }

    @Override
    protected void expectationTestChangeService(ServiceInfo info, String ServiceId)
            throws Exception {
        expectGeoServerReload();
    }

    private void expectGeoServerReload() throws Exception {
        // Add a small delay to reload calls to mitigate a race condition.
        getGeoServer().reload();
        expectLastCall()
                .andAnswer(
                        () -> {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                            }
                            return null;
                        });
    }

    @Override
    protected void waitForSync() throws Exception {
        super.waitForSync();
        // Add a small delay to reload calls to mitigate a race condition.
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
        }
    }
}
