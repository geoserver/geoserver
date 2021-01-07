/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class SupplementalFileProviderTest {

    @Test
    public void testSupportedSupplementalFiles() throws Exception {

        SpatialFileExtensionsProvider provider = new SpatialFileExtensionsProvider();

        // Test some unsupported base extensions
        Assert.assertFalse(provider.canHandle("txt"));
        Assert.assertFalse(provider.canHandle("pdf"));

        // Test some supported extensions
        Assert.assertTrue(provider.canHandle("tif"));
        Set<String> extensions = provider.getExtensions("tif");
        Assert.assertTrue(extensions.contains("tfw"));
        Assert.assertTrue(extensions.contains("prj"));
        Assert.assertTrue(extensions.contains("wld"));
        Assert.assertTrue(extensions.contains("rrd"));

        Assert.assertTrue(provider.canHandle("jpg"));
        extensions = provider.getExtensions("jpg");
        Assert.assertTrue(extensions.contains("jpw"));
        Assert.assertTrue(extensions.contains("prj"));
        Assert.assertTrue(extensions.contains("wld"));

        // Test the UPPERCASE support
        extensions = provider.getExtensions("SHP");
        Assert.assertTrue(extensions.contains("DBF"));
        Assert.assertTrue(extensions.contains("SHX"));
    }
}
