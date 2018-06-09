package org.geoserver.cluster.hazelcast;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.logging.Logger;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.geotools.util.logging.Logging;

/**
 * A {@link ResourceNotificationDispatcher} implementation .
 *
 * <p>A Spring bean of this type shall be configured in the project's {@code applicationContext.xml}
 * spring configuration file in order for {@link ResourceStore} to find it.
 */
public class HzResourceNotificationDispatcher extends SimpleResourceNotificationDispatcher
        implements MessageListener<ResourceNotification> {

    static final String TOPIC_NAME = "resourceWatcher";

    private static final Logger LOGGER = Logging.getLogger(HzResourceNotificationDispatcher.class);

    private HzCluster cluster;

    public HzResourceNotificationDispatcher() {
        // lazy loaded cluster
    }

    public HzResourceNotificationDispatcher(HzCluster cluster) {
        this.cluster = cluster;
        topic().addMessageListener(this);
    }

    private ITopic<ResourceNotification> topic() {
        if (cluster == null) {
            cluster = HzCluster.getInstanceIfAvailable().orNull();
            if (cluster != null) {
                topic().addMessageListener(this);
            }
        }
        return cluster == null ? null : cluster.getHz().getTopic(TOPIC_NAME);
    }

    @Override
    public void changed(ResourceNotification event) {
        ITopic<ResourceNotification> topic = topic();
        if (topic != null) {
            topic.publish(event);
        } else {
            LOGGER.warning(
                    "Failed to publish resource notification, cluster not initialized (yet).");
            super.changed(event);
        }
    }

    @Override
    public void onMessage(Message<ResourceNotification> msg) {
        LOGGER.info("Received ResourceNotification from HazelCast: " + msg.getMessageObject());

        super.changed(msg.getMessageObject());
    }
}
