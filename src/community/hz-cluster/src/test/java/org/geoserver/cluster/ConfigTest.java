/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.geoserver.cluster.hazelcast.HzSynchronizer;
import org.geoserver.cluster.hazelcast.HzSynchronizerTest;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.junit.Test;

/** @author Alessio Fabiani, GeoSolutions */
public class ConfigTest extends HzSynchronizerTest {

    @Test
    public void testConfigurationReload() throws IOException {
        Resource tmpDir1 = tmpDir();
        Resource tmpDir2 = tmpDir();
        GeoServerResourceLoader resourceLoader1 = new GeoServerResourceLoader(tmpDir1.dir());
        GeoServerResourceLoader resourceLoader2 = new GeoServerResourceLoader(tmpDir2.dir());
        Resources.directory(tmpDir1.get("cluster"), true);
        Resources.directory(tmpDir2.get("cluster"), true);

        this.cluster.setResourceStore(resourceLoader1.getResourceStore());
        this.cluster.saveConfiguration(resourceLoader1);

        assertNotNull(cluster.getFileLocations());
        assertEquals(2, cluster.getFileLocations().size());

        this.cluster.saveConfiguration(resourceLoader2);

        assertTrue(
                "The file 'cluster.properties' does not exist!",
                Resources.exists(tmpDir2.get("cluster/cluster.properties")));
        assertTrue(
                "The file 'hazelcast.xml' does not exist!",
                Resources.exists(tmpDir2.get("cluster/hazelcast.xml")));

        assertEquals(
                IOUtils.readLines(tmpDir1.get("cluster/cluster.properties").in()),
                IOUtils.readLines(tmpDir2.get("cluster/cluster.properties").in()));

        assertEquals(
                IOUtils.readLines(tmpDir1.get("cluster/hazelcast.xml").in()),
                IOUtils.readLines(tmpDir2.get("cluster/hazelcast.xml").in()));
    }

    @Override
    protected HzSynchronizer getSynchronizer() {
        // TODO Auto-generated method stub
        return null;
    }
}
