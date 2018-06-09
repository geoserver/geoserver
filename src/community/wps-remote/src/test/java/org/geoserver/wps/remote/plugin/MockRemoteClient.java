/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.util.Map;
import org.geoserver.wps.remote.RemoteProcessClient;
import org.geoserver.wps.remote.RemoteProcessFactoryConfigurationWatcher;
import org.geoserver.wps.remote.RemoteProcessFactoryListener;
import org.geoserver.wps.remote.RemoteServiceDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/** @author Alessio Fabiani, GeoSolutions */
public class MockRemoteClient extends RemoteProcessClient {

    public MockRemoteClient(
            RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher,
            boolean enabled,
            int priority) {
        super(remoteProcessFactoryConfigurationWatcher, enabled, priority);
    }

    @Override
    public void init() throws Exception {}

    @Override
    public void destroy() throws Exception {}

    @Override
    public String execute(
            Name serviceName,
            Map<String, Object> input,
            Map<String, Object> metadata,
            ProgressListener monitor)
            throws Exception {

        if (serviceName != null) {
            for (RemoteProcessFactoryListener listener : getRemoteFactoryListeners()) {
                listener.registerProcess(
                        new RemoteServiceDescriptor(
                                serviceName, "Service", "A test service", null, null, metadata));
            }
        }

        return "DONE";
    }
}
