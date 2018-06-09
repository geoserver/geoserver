/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cluster.hazelcast;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import org.easymock.EasyMock;
import org.geoserver.platform.ExtensionFilter;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.util.CacheProvider;
import org.geoserver.util.DefaultCacheProvider;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class HzExtensionFilterTest {

    @Rule
    public GeoServerExtensionsHelper.ExtensionsHelperRule extensions =
            new GeoServerExtensionsHelper.ExtensionsHelperRule();

    @Test
    public void testActive() {
        CacheProvider rivalProvider = EasyMock.createMock("rivalProvider", CacheProvider.class);
        CacheProvider hzProvider = EasyMock.createMock("hzProvider", HzCacheProvider.class);

        EasyMock.replay(rivalProvider, hzProvider);

        ExtensionFilter filter = new HzExtensionFilter();

        extensions.singleton("filter", filter, ExtensionFilter.class);

        extensions.singleton("rivalProvider", rivalProvider, CacheProvider.class);
        extensions.singleton("hzProvider", hzProvider, CacheProvider.class, HzCacheProvider.class);

        CacheProvider result = DefaultCacheProvider.findProvider();

        assertThat(result, sameInstance(hzProvider)); // Clustered provider used

        EasyMock.verify(rivalProvider, hzProvider);
    }

    @Ignore // Ran into circular dependency issued in Spring trying to implement this KS
    @Test
    public void testInactive() {
        CacheProvider rivalProvider = EasyMock.createMock("rivalProvider", CacheProvider.class);
        CacheProvider hzProvider = EasyMock.createMock("hzProvider", HzCacheProvider.class);
        HzCluster cluster = EasyMock.createMock("cluster", HzCluster.class);

        EasyMock.expect(cluster.isEnabled()).andStubReturn(false); // Cluster is disabled

        EasyMock.replay(rivalProvider, hzProvider, cluster);

        ExtensionFilter filter = new HzExtensionFilter(/*cluster*/ );

        extensions.singleton("filter", filter, ExtensionFilter.class);

        extensions.singleton("rivalProvider", rivalProvider, CacheProvider.class);
        extensions.singleton("hzProvider", hzProvider, CacheProvider.class, HzCacheProvider.class);

        CacheProvider result = DefaultCacheProvider.findProvider();

        assertThat(result, sameInstance(rivalProvider)); // Other provider used

        EasyMock.verify(rivalProvider, hzProvider, cluster);
    }
}
