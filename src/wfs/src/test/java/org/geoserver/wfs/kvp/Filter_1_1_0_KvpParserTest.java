/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;

public class Filter_1_1_0_KvpParserTest {

    @Test
    public void testParse() throws Exception {
        final String filterString =
                "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "<ogc:Intersects><ogc:PropertyName>the_geom</ogc:PropertyName>"
                        + "<gml:Polygon><gml:exterior><gml:LinearRing>"
                        + "<gml:posList>-112 46 -109 46 -109 47 -112 47 -112 46</gml:posList>"
                        + "</gml:LinearRing></gml:exterior></gml:Polygon></ogc:Intersects></ogc:Filter>";
        List filters = (List) new Filter_1_1_0_KvpParser(null).parse(filterString);
        assertNotNull(filters);
        assertEquals(1, filters.size());

        Filter f = (Filter) filters.get(0);
        assertTrue(f instanceof Intersects);
    }
}
