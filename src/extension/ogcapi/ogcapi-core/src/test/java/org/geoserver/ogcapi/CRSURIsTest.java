/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class CRSURIsTest {

    @Test
    public void testUriFromBareCode() {
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4326", CRSURIs.uri("4326"));
    }

    @Test
    public void testUriFromAuthorityCode() {
        assertEquals("http://www.opengis.net/def/crs/IAU/0/49900", CRSURIs.uri("IAU:49900"));
    }

    @Test
    public void testListCRS84First() {
        assertEquals(
                List.of(
                        CollectionExtents.WGS84,
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/EPSG/0/32632"),
                CRSURIs.list(List.of("3857", "EPSG:32632")));
    }

    /** An empty per-resource override yields CRS84 alone, it must not fall back to the referencing database. */
    @Test
    public void testEmptyListIsCRS84Only() {
        assertEquals(List.of(CollectionExtents.WGS84), CRSURIs.list(List.of()));
    }

    /** An empty service list means every code the referencing database knows, CRS84 still first. */
    @Test
    public void testServiceListFallsBackToDatabase() {
        List<String> uris = CRSURIs.serviceList(List.of());
        assertEquals(CollectionExtents.WGS84, uris.get(0));
        assertTrue(uris.size() > 1000);
        assertTrue(uris.contains("http://www.opengis.net/def/crs/EPSG/0/32632"));
        assertEquals(1, uris.stream().filter(CollectionExtents.WGS84::equals).count());
    }
}
