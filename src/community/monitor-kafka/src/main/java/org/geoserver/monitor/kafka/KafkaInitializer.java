/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.monitor.kafka;

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.monitor.Monitor;
import org.geotools.util.logging.Logging;

public class KafkaInitializer implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    Monitor monitor;

    public KafkaInitializer(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void initialize(GeoServer geoServer) {
        LOGGER.info("stating up kafka monitor");

        if (!monitor.isEnabled()) {
            LOGGER.info("monitor is not enabled");
            return;
        }

        if (monitor.getDAO() instanceof KafkaDAO) {
            KafkaDAO kafkaDAO = ((KafkaDAO) monitor.getDAO());
            kafkaDAO.config = new KafkaMonitorConfig(monitor.getConfig());
            kafkaDAO.connectionTester = new ConnectionTester(kafkaDAO.config);
            if (kafkaDAO.connectionTester.testConnection()) {
                LOGGER.info("Monitor kafka extension enabled");
            } else {
                LOGGER.warning(
                        "Monitor kafka extension disabled. Kafka connection failed. We will not log any requests until restart with proper kafka connection.");
            }
        }
    }
}
