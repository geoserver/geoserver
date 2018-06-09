/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.geoserver.platform.resource.Resource.Lock;
import org.junit.Before;
import org.junit.Test;

public class HzLockProviderTest {

    private HazelcastInstance hz;
    private HzLockProvider lockProvider;
    private HzCluster cluster;

    @Before
    public void setUp() throws Exception {
        Cluster hzCluster = createMock(Cluster.class);

        hz = createMock(HazelcastInstance.class);
        expect(hz.getCluster()).andStubReturn(hzCluster);

        cluster = createMock(HzCluster.class);
        expect(this.cluster.isEnabled()).andStubReturn(true);
        expect(this.cluster.isRunning()).andStubReturn(true);
        expect(this.cluster.getHz()).andStubReturn(hz);

        lockProvider = new HzLockProvider();
        lockProvider.setCluster(cluster);
        lockProvider.afterPropertiesSet();
    }

    @Test
    public void testAqcuire() {

        ILock lock = createMock(ILock.class);
        expect(this.hz.getLock(eq("path1"))).andReturn(lock);
        expect(lock.isLockedByCurrentThread()).andStubReturn(true);

        lock.lock();
        expectLastCall();

        expect(lock.isLocked()).andReturn(true).times(1);

        lock.unlock();
        expectLastCall();

        replay(lock, hz, cluster);

        Lock gsLock = lockProvider.acquire("path1");

        gsLock.release();

        verify(hz);
        verify(cluster);
        verify(lock);
    }
}
