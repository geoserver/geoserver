/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.cluster.hazelcast.HzSynchronizer;
import org.geoserver.cluster.hazelcast.HzSynchronizerTest;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author Alessio Fabiani, GeoSolutions */
public class ConfigTest extends HzSynchronizerTest {

    private Resource tmpDir;
    private Resource tmpDir1;
    private Resource tmpDir2;

    @Before
    public void createTempDirs() throws IOException {
        tmpDir = tmpDir();
        tmpDir1 = tmpDir();
        tmpDir2 = tmpDir();
    }

    @After
    public void deleteTempDirs() {
        FileUtils.deleteQuietly(tmpDir.dir());
        FileUtils.deleteQuietly(tmpDir1.dir());
        FileUtils.deleteQuietly(tmpDir2.dir());
    }

    @Test
    public void testConfigurationReload() throws IOException {
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tmpDir.dir());
        GeoServerResourceLoader resourceLoader1 = new GeoServerResourceLoader(tmpDir1.dir());
        GeoServerResourceLoader resourceLoader2 = new GeoServerResourceLoader(tmpDir2.dir());
        Resources.directory(tmpDir.get("cluster"), true);
        Resources.directory(tmpDir1.get("cluster"), true);
        Resources.directory(tmpDir2.get("cluster"), true);

        this.cluster.setResourceStore(resourceLoader.getResourceStore());
        this.cluster.saveConfiguration(resourceLoader1);

        assertNotNull(cluster.getFileLocations());
        assertEquals(2, cluster.getFileLocations().size());

        assertTrue(
                "The file 'cluster.properties' does not exist!",
                Resources.exists(tmpDir1.get("cluster/cluster.properties")));
        assertTrue(
                "The file 'hazelcast.xml' does not exist!",
                Resources.exists(tmpDir1.get("cluster/hazelcast.xml")));

        this.cluster.saveConfiguration(resourceLoader2);

        assertTrue(
                "The file 'cluster.properties' does not exist!",
                Resources.exists(tmpDir2.get("cluster/cluster.properties")));
        assertTrue(
                "The file 'hazelcast.xml' does not exist!",
                Resources.exists(tmpDir2.get("cluster/hazelcast.xml")));

        assertEquals(
                lines(tmpDir1, "cluster/cluster.properties"),
                lines(tmpDir2, "cluster/cluster.properties"));

        assertEquals(
                lines(tmpDir1, "cluster/hazelcast.xml"), lines(tmpDir2, "cluster/hazelcast.xml"));
    }

    @Override
    protected HzSynchronizer getSynchronizer() {
        return null;
    }

    private static final List<String> lines(Resource dir, String path) throws IOException {
        try (InputStream input = dir.get(path).in()) {
            return IOUtils.readLines(input, StandardCharsets.UTF_8);
        }
    }
}
