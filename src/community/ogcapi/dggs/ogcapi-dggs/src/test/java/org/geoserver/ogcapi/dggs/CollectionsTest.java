/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.dggs;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class CollectionsTest extends DGGSTestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/collections", 200);
    }
}
