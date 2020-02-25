/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import org.geoserver.wps.AbstractProcessStoreTest;
import org.geoserver.wps.ProcessStatusStore;
import org.junit.After;

/**
 * Tests the hazelcast based process status store with a single hazelcast instance
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MultinodeHazelcastStatusStoreTest extends AbstractProcessStoreTest {

    private HazelcastStatusStore store1;

    private HazelcastStatusStore store2;

    private TestHazelcastInstanceFactory factory;

    @Override
    protected ProcessStatusStore buildStore() {
        // builds two hazelcast instance isolated from the network, but communcating
        // with each other
        Config config = new Config();
        config.addMapConfig(new MapConfig(HazelcastStatusStore.EXECUTION_STATUS_MAP));
        factory = new TestHazelcastInstanceFactory(2);

        HazelcastInstance[] instances = factory.newInstances(config);
        this.store1 = new HazelcastStatusStore(new HazelcastLoader(instances[0]));
        this.store2 = new HazelcastStatusStore(new HazelcastLoader(instances[1]));

        return store1;
    }

    @After
    public void shutdown() {
        factory.shutdownAll();
    }

    @Override
    protected void fillStore() {
        store1.save(s1);
        store1.save(s2);
        store2.save(s3);
        store2.save(s4);
    }
}
