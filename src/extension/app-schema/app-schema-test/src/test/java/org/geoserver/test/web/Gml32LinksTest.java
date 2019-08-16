/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test.web;

import java.util.Arrays;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.Gsml30MockData;
import org.junit.Test;

public class Gml32LinksTest extends AbstractMapPreviewPageTest {

    public Gml32LinksTest() {
        super(
                Arrays.asList(
                        "http://localhost/context/gsml/ows?service=WFS&amp;version=1.1.0&amp;request=GetFeature&amp;typeName=gsml%3AMappedFeature&amp;outputFormat=gml32&amp;maxFeatures=50"));
    }

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new Gsml30MockData();
    }

    @Test
    public void testGml32Links() {
        super.testAppSchemaGmlLinks();
    }
}
