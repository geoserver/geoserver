/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import java.util.concurrent.ScheduledExecutorService;

public class ReloadHzSynchronizerSendTest extends HzSynchronizerSendTest {

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
}
