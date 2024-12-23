/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.monitor.kafka;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.TopicDescription;
import org.geoserver.monitor.Monitor;
import org.geotools.util.logging.Logging;

public class ConnectionTester {

    private final KafkaMonitorConfig config;
    static Logger LOGGER = Logging.getLogger(Monitor.class);

    public ConnectionTester(KafkaMonitorConfig config) {
        this.config = config;
    }

    public boolean testConnection() {
        return testConnection(config.getKafkaProperties(), config.getTopic());
    }

    public boolean testConnection(Properties properties, String topic) {
        if (!config.isEnabled()) {
            return false;
        }
        try (AdminClient adminClient = AdminClient.create(properties)) {
            Map<String, TopicDescription> topicDescribe = adminClient
                    .describeTopics(List.of(topic), new DescribeTopicsOptions().timeoutMs(10_000))
                    .allTopicNames()
                    .get();
            boolean topicExists = topicDescribe.containsKey(topic);
            LOGGER.info("Kafka connection established"
                    + (topicExists ? " and topic " + topic + " exists" : " but topic " + topic + " does not exist"));
            return topicExists;
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.warning("Kafka connection failed " + e.getMessage());
            config.setEnabled(false);
            return false;
        }
    }
}
