/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CaseInsensitiveMapTest {

    @Test
    public void testWholeMap() {
        Map source = new HashMap();
        source.put("AbC", "test");
        CaseInsensitiveMap cim = new CaseInsensitiveMap(source);
        assertEquals("test", cim.get("abc"));
    }
}
