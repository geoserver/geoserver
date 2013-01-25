package org.geoserver.cluster.hazelcast;

import static org.geoserver.cluster.hazelcast.HazelcastUtil.*;

import java.util.logging.Logger;

import org.geoserver.cluster.Event;
import org.geoserver.cluster.GeoServerSynchronizer;
import org.geotools.util.logging.Logging;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public abstract class HzSynchronizer extends GeoServerSynchronizer implements MessageListener<Event> {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    protected HazelcastInstance hz;

    protected ITopic<Event> topic;

    public HzSynchronizer(HazelcastInstance hz) {
        this.hz = hz;

        topic = hz.getTopic("geoserver.config");
        topic.addMessageListener(this);
    }

    protected Event newEvent() {
        Event e = new Event();
        e.setSource(localAddress(hz));
        return e;
    }
}
