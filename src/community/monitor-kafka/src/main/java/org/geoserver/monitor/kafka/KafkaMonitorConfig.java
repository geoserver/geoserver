/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.monitor.kafka;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geotools.util.logging.Logging;

public class KafkaMonitorConfig {

    private String topic = "geoserver-monitor";
    private Properties props;
    private boolean enabled = true;
    private MonitorConfig config;

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    public KafkaMonitorConfig(MonitorConfig config) {
        this.config = config;
    }

    public String getTopic() {
        return getTopicFromProperties();
    }

    public Properties getKafkaProperties() {
        return extractKafkaProperties();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private Properties extractKafkaProperties() {
        if (props == null) {
            props = new Properties();
            config.getProperties().entrySet().forEach((p) -> {
                if (p.getKey().toString().startsWith("kafka.")
                        && !p.getKey().toString().equals("kafka.topic")) {
                    String propName = p.getKey().toString().substring(6);
                    props.put(propName, p.getValue());
                    LOGGER.info("using kafka property: " + propName);
                }
            });

            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        }
        return props;
    }

    String getTopicFromProperties() {
        if (config.getProperties().get("kafka.topic") != null) {
            topic = (String) config.getProperties().get("kafka.topic");
        }
        return topic;
    }
}
