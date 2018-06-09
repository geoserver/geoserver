/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.util.Arrays;
import org.geoserver.importer.transform.GdalAddoTransform;
import org.geoserver.importer.transform.GdalTranslateTransform;
import org.geoserver.importer.transform.GdalWarpTransform;
import org.junit.Test;

public class GdalTransformTest extends TransformTestSupport {

    @Test
    public void testGdalTransformJSON() throws Exception {
        doJSONTest(
                new GdalTranslateTransform(
                        Arrays.asList("-co", "blockxsize=128", "-co", "blockysize=128")));
    }

    @Test
    public void testGdalWarpJSON() throws Exception {
        doJSONTest(new GdalWarpTransform(Arrays.asList("-t_srs", "EPSG:4326")));
    }

    @Test
    public void testAddoTransformJSON() throws Exception {
        doJSONTest(new GdalAddoTransform(Arrays.asList("-r", "average"), Arrays.asList(2, 4, 8)));
    }
}
