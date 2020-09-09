/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Set;
import junit.framework.TestCase;

public class SupplementalFileProviderTest extends TestCase {

    public void testSupportedSupplementalFiles() throws Exception {

        SpatialFileExtensionsProvider provider = new SpatialFileExtensionsProvider();

        // Test some unsupported base extensions
        assertFalse(provider.canHandle("txt"));
        assertFalse(provider.canHandle("pdf"));

        // Test some supported extensions
        assertTrue(provider.canHandle("tif"));
        Set<String> extensions = provider.getExtensions("tif");
        assertTrue(extensions.contains("tfw"));
        assertTrue(extensions.contains("prj"));
        assertTrue(extensions.contains("wld"));
        assertTrue(extensions.contains("rrd"));

        assertTrue(provider.canHandle("jpg"));
        extensions = provider.getExtensions("jpg");
        assertTrue(extensions.contains("jpw"));
        assertTrue(extensions.contains("prj"));
        assertTrue(extensions.contains("wld"));

        // Test the UPPERCASE support
        extensions = provider.getExtensions("SHP");
        assertTrue(extensions.contains("DBF"));
        assertTrue(extensions.contains("SHX"));
    }
}
