/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import org.easymock.Capture;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.cluster.ConfigChangeEvent;
import org.junit.Ignore;
import org.junit.Test;

public class EventHzSynchronizerSendTest extends HzSynchronizerSendTest {

    @Override
    protected HzSynchronizer getSynchronizer() {
        return new EventHzSynchronizer(cluster, getGeoServer()) {

            @Override
            ScheduledExecutorService getNewExecutor() {
                return getMockExecutor();
            }

            @Override
            public boolean isStarted() {
                return true;
            }
        };
    }

    @Ignore // Spin lock based wait makes this hard to test KS
    @Test
    public void testWaitForAck() throws Exception {
        DataStoreInfo info;
        WorkspaceInfo wsInfo;
        final String storeName = "testStore";
        final String storeId = "Store-TEST";
        final String storeWorkspace = "Workspace-TEST";

        final Capture<ConfigChangeEvent> capture = new Capture<ConfigChangeEvent>();

        {
            info = createMock(DataStoreInfo.class);
            wsInfo = createMock(WorkspaceInfo.class);

            expect(info.getName()).andStubReturn(storeName);
            expect(info.getId()).andStubReturn(storeId);
            expect(info.getWorkspace()).andStubReturn(wsInfo);

            expect(wsInfo.getId()).andStubReturn(storeWorkspace);

            topic.publish(capture(capture));
            expectLastCall();
        }
        replay(info, wsInfo);
        {
            HzSynchronizer sync = getSynchronizer();

            // Mock the result of doing this:
            // getCatalog().remove(info);

            CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();

            event.setSource(info);

            for (CatalogListener listener : catListenerCapture.getValues()) {
                listener.handleRemoveEvent(event);
            }

            for (MessageListener<UUID> listener : this.captureAckTopicListener.getValues()) {
                String topicName = "geoserver.config.ack";
                UUID messageObject = capture.getValue().getUUID();
                int publishTime = 0;
                Member publishingMember = null;
                Message<UUID> message =
                        new Message<UUID>(topicName, messageObject, publishTime, publishingMember);
                listener.onMessage(message);
            }
        }
        waitForSync();
        verify(info, wsInfo);
    }
}
