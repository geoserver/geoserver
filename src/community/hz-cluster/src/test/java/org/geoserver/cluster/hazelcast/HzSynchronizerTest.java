/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.cluster.ClusterConfig;
import org.geoserver.cluster.ClusterConfigWatcher;
import org.geoserver.cluster.Event;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.junit.Before;

public abstract class HzSynchronizerTest {

    public static final String TOPIC_NAME = "geoserver.config";
    public static final String ACK_TOPIC_NAME = "geoserver.config.ack";
    public static final int SYNC_DELAY = 1;
    // protected void setUpSpring(List<String> springContextLocations) {

    // We're going to set up the synchronizer manually so ignore the spring context.
    // }

    public static Resource tmpDir() throws IOException {
        Resource root = Files.asResource(new File(System.getProperty("java.io.tmpdir", ".")));
        Resource directory = Resources.createRandom("tmp", "", root);

        do {
            FileUtils.forceDelete(directory.dir());
        } while (Resources.exists(directory));

        FileUtils.forceMkdir(directory.dir());

        return Files.asResource(directory.dir());
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        hz = createMock(HazelcastInstance.class);
        // Create a "partial mock" of the HzCluster
        cluster =
                createMockBuilder(HzCluster.class)
                        .addMockedMethods(
                                "getHz", "isEnabled", "getRawCatalog", "getAckTimeoutMillis")
                        .createMock();
        topic = createMock(ITopic.class);
        ackTopic = createMock(ITopic.class);
        configWatcher = createMock(ClusterConfigWatcher.class);
        clusterConfig = createMock(ClusterConfig.class);

        captureTopicListener = new Capture<MessageListener<Event>>();
        captureAckTopicListener = new Capture<MessageListener<UUID>>();
        captureAckTopicPublish = new Capture<UUID>();

        localAddress = new InetSocketAddress(localAddress(42), 5000);
        remoteAddress = new InetSocketAddress(localAddress(54), 5000);

        catalog = createMock(Catalog.class);

        Cluster cluster = createMock(Cluster.class);
        Member localMember = createMock(Member.class);
        Member remoteMember = createMock(Member.class);

        expect(this.cluster.getHz()).andStubReturn(hz);
        expect(this.cluster.isEnabled()).andStubReturn(true);
        expect(this.cluster.getRawCatalog()).andStubReturn(catalog);
        ;
        expect(this.cluster.getAckTimeoutMillis()).andStubReturn(100);

        expect(hz.<Event>getTopic(TOPIC_NAME)).andStubReturn(topic);
        expect(topic.addMessageListener(capture(captureTopicListener))).andReturn("fake-id");
        expectLastCall().anyTimes();

        expect(hz.<UUID>getTopic(ACK_TOPIC_NAME)).andStubReturn(ackTopic);
        expect(ackTopic.addMessageListener(capture(captureAckTopicListener))).andReturn("fake-id");
        expectLastCall().anyTimes();

        ackTopic.publish(EasyMock.capture(captureAckTopicPublish));
        EasyMock.expectLastCall()
                .andStubAnswer(
                        new IAnswer<Object>() {

                            @Override
                            public Object answer() throws Throwable {
                                Message<UUID> message = createMock(Message.class);
                                expect(message.getMessageObject())
                                        .andStubReturn(captureAckTopicPublish.getValue());
                                EasyMock.replay(message);
                                for (MessageListener<UUID> listener :
                                        captureAckTopicListener.getValues()) {
                                    listener.onMessage(message);
                                }
                                return null;
                            }
                        });

        expect(cluster.getLocalMember()).andStubReturn(localMember);
        expect(localMember.getSocketAddress()).andStubReturn(localAddress);
        expect(remoteMember.getSocketAddress()).andStubReturn(remoteAddress);
        expect(localMember.localMember()).andStubReturn(true);
        expect(remoteMember.localMember()).andStubReturn(false);

        expect(cluster.getMembers()).andStubReturn(Sets.newHashSet(localMember, remoteMember));

        EasyMock.replay(cluster, localMember, remoteMember);

        expect(hz.getCluster()).andStubReturn(cluster);

        expect(configWatcher.get()).andStubReturn(clusterConfig);

        expect(clusterConfig.getSyncDelay()).andStubReturn(SYNC_DELAY);

        geoServer = createMock(GeoServer.class);

        expect(geoServer.getCatalog()).andStubReturn(catalog);

        gsListenerCapture = new Capture<ConfigurationListener>();
        geoServer.addListener(capture(gsListenerCapture));
        expectLastCall().atLeastOnce();

        catListenerCapture = new Capture<CatalogListener>();
        catalog.addListener(capture(catListenerCapture));
        expectLastCall().atLeastOnce();

        executor = createMock(ScheduledExecutorService.class);
        captureExecutor = new Capture<>(CaptureType.ALL);
        expect(executor.schedule(capture(captureExecutor), anyLong(), (TimeUnit) anyObject()))
                .andStubReturn(null);
    }

    protected static InetAddress localAddress(int i) throws Exception {
        return InetAddress.getByAddress(new byte[] {(byte) 192, (byte) 168, 0, (byte) i});
    }

    MessageListener<Event> getListener() {
        return captureTopicListener.getValue();
    }

    protected HazelcastInstance hz;
    protected HzCluster cluster;
    protected ITopic<Event> topic;
    protected ITopic<UUID> ackTopic;
    protected GeoServer geoServer;
    protected Catalog catalog;
    protected ClusterConfigWatcher configWatcher;
    protected ClusterConfig clusterConfig;
    protected ScheduledExecutorService executor;

    protected InetSocketAddress localAddress;
    protected InetSocketAddress remoteAddress;

    protected Capture<ConfigurationListener> gsListenerCapture;
    protected Capture<CatalogListener> catListenerCapture;
    protected Capture<MessageListener<Event>> captureTopicListener;
    protected Capture<MessageListener<UUID>> captureAckTopicListener;
    protected Capture<Callable<Future<?>>> captureExecutor;
    protected Capture<UUID> captureAckTopicPublish;

    public List<Object> myMocks() {
        return Arrays.asList(
                topic,
                ackTopic,
                configWatcher,
                clusterConfig,
                geoServer,
                catalog,
                hz,
                executor,
                cluster);
    }

    public HzSynchronizerTest() {
        super();
    }

    protected ScheduledExecutorService getMockExecutor() {
        return executor;
    }

    /**
     * Return the HzSynchronizer instance to be tested. Override {@link HzSyncronizer#getExecutor}
     * to return {@link #getMockExecutor}. Provide it with {@link #hz} and {@link #geoServer}.
     */
    protected abstract HzSynchronizer getSynchronizer();

    protected void initSynchronizer(HzSynchronizer sync) {
        sync.initialize(configWatcher);
    }

    protected GeoServer getGeoServer() {
        return geoServer;
    }

    protected Catalog getCatalog() {
        return catalog;
    }

    /** Replay all the mocks on this test class, plus those specified */
    protected void replay(Object... mocks) {
        EasyMock.replay(myMocks().toArray());
        EasyMock.replay(mocks);
    }
    /** Reset all the mocks on this test class, plus those specified */
    protected void reset(Object... mocks) {
        EasyMock.reset(myMocks().toArray());
        EasyMock.reset(mocks);
    }
    /** Verify all the mocks on this test class, plus those specified */
    protected void verify(Object... mocks) {
        EasyMock.verify(myMocks().toArray());
        EasyMock.verify(mocks);
    }

    protected void waitForSync() throws Exception {

        // Thread.sleep(SYNC_DELAY*1000+500); // Convert to millis, then add a little extra to be
        // sure

        List<Callable<Future<?>>> tasks = captureExecutor.getValues();
        List<Future<?>> futures = new ArrayList<>(1);

        for (Iterator<Callable<Future<?>>> i = tasks.iterator(); i.hasNext(); ) {
            Callable<Future<?>> task = i.next();
            i.remove();
            Future<?> future = task.call();
            if (future != null) {
                futures.add(future);
            }
        }
        for (Future<?> future : futures) {
            future.get();
        }
    }

    void assertAcked(UUID... eventId) {
        assertThat(captureAckTopicPublish.getValues(), hasItems(eventId));
    }
}
