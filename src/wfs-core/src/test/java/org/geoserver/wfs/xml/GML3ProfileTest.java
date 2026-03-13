/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.Name;
import org.geotools.geometry.jts.MultiSurface;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;

public class GML3ProfileTest {

    @Test
    public void testMultiSurfacePolygon() {
        GML3Profile profile = new GML3Profile();

        AttributeType multiPolygonType = profile.type(MultiPolygon.class);
        assertNotNull(multiPolygonType);
        assertEquals("MultiSurfacePropertyType", multiPolygonType.getName().getLocalPart());

        AttributeType multiSurfaceType = profile.type(MultiSurface.class);
        assertNotNull(multiSurfaceType);
        assertEquals("MultiSurfacePropertyType", multiSurfaceType.getName().getLocalPart());

        Name multiPolygonName = profile.name(MultiPolygon.class);
        assertNotNull(multiPolygonType);
        assertEquals("MultiSurfacePropertyType", multiPolygonName.getLocalPart());

        Name multiSurfaceName = profile.name(MultiSurface.class);
        assertNotNull(multiSurfaceName);
        assertEquals("MultiSurfacePropertyType", multiSurfaceName.getLocalPart());
    }
}
