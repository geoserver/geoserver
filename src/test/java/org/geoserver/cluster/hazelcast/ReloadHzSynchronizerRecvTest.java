package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.*;

import java.util.concurrent.ScheduledExecutorService;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;

public class ReloadHzSynchronizerRecvTest extends HzSynchronizerRecvTest {

    @Override
    protected HzSynchronizer getSynchronizer() {
        return new ReloadHzSynchronizer(hz, getGeoServer()) {

            @Override
            ScheduledExecutorService getNewExecutor() {
                return getMockExecutor();
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

}
