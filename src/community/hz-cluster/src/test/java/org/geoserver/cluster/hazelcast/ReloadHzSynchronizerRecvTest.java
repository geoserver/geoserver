/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.expectLastCall;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
            public boolean isStarted() {
                return true;
            }
        };
    }

    @Override
    protected void expectationTestDisableLayer(LayerInfo info, String layerName, String layerId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestStoreDelete(
            DataStoreInfo info, String storeName, String storeId, Class clazz) throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestFTDelete(
            FeatureTypeInfo info, String ftName, String ftId, String storeId, Class clazz)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestContactChange(GeoServerInfo info, String storeId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestMultipleChange(
            GeoServerInfo gsInfo, String globalId, LayerInfo layerInfo, String layerId)
            throws Exception {

        getGeoServer().reload();
        expectLastCall();
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestTwoAddressChangeNoPause(GeoServerInfo gsInfo, String globalId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestTwoAddressChangeWithPause(GeoServerInfo gsInfo, String globalId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestTwoLayerChangeNoPause(LayerInfo layerInfo, String layerId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestTwoLayerChangeWithPause(LayerInfo layerInfo, String layerId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestWorkspaceAdd(
            WorkspaceInfo info, String workspaceName, String workspaceId) throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestChangeSettings(
            SettingsInfo info, String settingsId, WorkspaceInfo wsInfo, String workspaceId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestChangeLogging(LoggingInfo info, String loggingId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    @Override
    protected void expectationTestChangeService(ServiceInfo info, String ServiceId)
            throws Exception {
        getGeoServer().reload();
        expectLastCall();
    }

    /** Overrides to wait for {@link ReloadHzSynchronizer}'s executor service to shut down */
    @Override
    protected void verify(Object... mocks) {
        ExecutorService reloadService = ((ReloadHzSynchronizer) sync).reloadService;
        reloadService.shutdown();
        try {
            reloadService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.verify(mocks);
    }
}
