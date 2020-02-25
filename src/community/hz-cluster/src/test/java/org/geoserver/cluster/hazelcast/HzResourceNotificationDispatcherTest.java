/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.platform.resource.AbstractResourceNotificationDispatcherTest;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;

/** @author Niels Charlier */
public class HzResourceNotificationDispatcherTest
        extends AbstractResourceNotificationDispatcherTest {

    @Override
    protected ResourceNotificationDispatcher initWatcher() throws Exception {
        final Capture<MessageListener<ResourceNotification>> captureTopicListener =
                new Capture<MessageListener<ResourceNotification>>();
        final Capture<ResourceNotification> captureTopicPublish =
                new Capture<ResourceNotification>();

        final Cluster cluster = createMock(Cluster.class);
        final ITopic<ResourceNotification> topic = createMock(ITopic.class);
        final HazelcastInstance hz = createMock(HazelcastInstance.class);
        final HzCluster hzCluster = createMock(HzCluster.class);

        expect(hz.getCluster()).andStubReturn(cluster);
        expect(hz.<ResourceNotification>getTopic(HzResourceNotificationDispatcher.TOPIC_NAME))
                .andStubReturn(topic);
        expect(topic.addMessageListener(capture(captureTopicListener))).andReturn("fake-id");
        topic.publish(EasyMock.capture(captureTopicPublish));
        expectLastCall()
                .andStubAnswer(
                        new IAnswer<Object>() {
                            @Override
                            public Object answer() throws Throwable {
                                Message<ResourceNotification> message = createMock(Message.class);
                                expect(message.getMessageObject())
                                        .andStubReturn(captureTopicPublish.getValue());
                                EasyMock.replay(message);
                                for (MessageListener<ResourceNotification> listener :
                                        captureTopicListener.getValues()) {
                                    listener.onMessage(message);
                                }
                                return null;
                            }
                        });

        expect(hzCluster.isEnabled()).andStubReturn(true);
        expect(hzCluster.isRunning()).andStubReturn(true);
        expect(hzCluster.getHz()).andStubReturn(hz);

        replay(cluster, topic, hz, hzCluster);

        return new HzResourceNotificationDispatcher(hzCluster);
    }
}
