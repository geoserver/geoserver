package org.geoserver.cluster.hazelcast;

import java.util.concurrent.ScheduledExecutorService;

public class EventHzSynchronizerSendTest extends HzSynchronizerSendTest {

    @Override
    protected HzSynchronizer getSynchronizer() {
        return new EventHzSynchronizer(cluster, getGeoServer()) {

            @Override
            ScheduledExecutorService getNewExecutor() {
                return getMockExecutor();
            }
            
        };
    }

}
