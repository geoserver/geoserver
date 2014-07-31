package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.*;

import java.util.concurrent.ScheduledExecutorService;

import org.geoserver.catalog.DataStoreInfo;
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
            public boolean isStarted(){
                return true;
            }
        };
    }

    @Override
    protected void expectationTestDisableLayer(LayerInfo info, String layerName, String layerId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestStoreDelete(DataStoreInfo info, String storeName, String storeId, Class clazz)
            throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestContactChange(GeoServerInfo info,
            String storeId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestMultipleChange(GeoServerInfo gsInfo,
            String globalId, LayerInfo layerInfo, String layerId) throws Exception {
        
        getGeoServer().reload(); expectLastCall();
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestTwoAddressChangeNoPause(GeoServerInfo gsInfo,
            String globalId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestTwoAddressChangeWithPause(
            GeoServerInfo gsInfo, String globalId) throws Exception {
        getGeoServer().reload(); expectLastCall();
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestTwoLayerChangeNoPause(LayerInfo layerInfo,
            String layerId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestTwoLayerChangeWithPause(LayerInfo layerInfo,
            String layerId) throws Exception {
        getGeoServer().reload(); expectLastCall();
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestWorkspaceAdd(WorkspaceInfo info,
            String workspaceName, String workspaceId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestChangeSettings(SettingsInfo info,
            String settingsId, WorkspaceInfo wsInfo, String workspaceId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestChangeLogging(LoggingInfo info,
            String loggingId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

    @Override
    protected void expectationTestChangeService(ServiceInfo info,
            String ServiceId) throws Exception {
        getGeoServer().reload(); expectLastCall();
    }

}
