/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.support;

import com.google.common.io.Files;
import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;

public class BrokerManager {
    private static final String INITIAL_ANONYMOUS_CONFIG_PATH =
            "src/test/resources/qpid-anonymous-config.json";

    private static final String INITIAL_CONFIG_PATH = "src/test/resources/qpid-config.json";

    private static final String PWD_PATH = "src/test/resources/passwd.properties";

    private static final String PORT = "4432";

    private final Broker broker = new Broker();

    public void startBroker(Boolean isAnonymous) throws Exception {
        final BrokerOptions brokerOptions = new BrokerOptions();
        brokerOptions.setConfigProperty("qpid.amqp_port", PORT);
        String cfg = INITIAL_ANONYMOUS_CONFIG_PATH;
        if (!isAnonymous) {
            cfg = INITIAL_CONFIG_PATH;
            brokerOptions.setConfigProperty("qpid.pass_file", PWD_PATH);
        }
        brokerOptions.setConfigProperty("qpid.work_dir", Files.createTempDir().getAbsolutePath());
        brokerOptions.setInitialConfigurationLocation(cfg);
        broker.startup(brokerOptions);
    }

    public void stopBroker() {
        broker.shutdown();
    }
}
