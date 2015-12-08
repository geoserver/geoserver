package org.geoserver.cluster.hazelcast;

import java.util.logging.Logger;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceWatcher;
import org.geoserver.platform.resource.SimpleResourceWatcher;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.base.Preconditions;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

/**
 * A {@link ResourceWatcher} implementation .
 * <p>
 * A Spring bean of this type shall be configured in the project's {@code applicationContext.xml}
 * spring configuration file in order for {@link ResourceStore} to find it.
 * 
 */
public class HzResourceWatcher extends SimpleResourceWatcher implements InitializingBean, MessageListener<ResourceNotification> {
    
    static final String TOPIC_NAME = "resourceWatcher";
    
    private static final Logger LOGGER = Logging.getLogger(HzResourceWatcher.class);
        
    private HzCluster cluster;  
    
    private ITopic<ResourceNotification> topic() {
        return cluster.getHz().getTopic(TOPIC_NAME);
    }
    
    /**
     * {@code cluster} property to be set in {@code applicationContext.xml}
     */
    public void setCluster(HzCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Preconditions.checkNotNull(cluster, "HzCluster is not set");
        topic().addMessageListener(this);
    }

    @Override
    public void changed(ResourceNotification event) {
        topic().publish(event);
    }

    @Override
    public void onMessage(Message<ResourceNotification> msg) {
        LOGGER.info( "Received ResourceNotification from HazelCast: " + 
               msg.getMessageObject());
        
        super.changed(msg.getMessageObject());        
    }
    
}
