package org.geoserver.cluster.hazelcast;

import java.util.logging.Logger;

import org.geoserver.cluster.Event;
import org.geoserver.cluster.GeoServerSynchronizer;
import org.geotools.util.logging.Logging;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public abstract class HzSynchronizer extends GeoServerSynchronizer implements MessageListener<Event> {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    protected ITopic<Event> topic;

    public HzSynchronizer(HazelcastInstance hz) {
        topic = hz.getTopic("geoserver.config");
        topic.addMessageListener(this);
    }

}
