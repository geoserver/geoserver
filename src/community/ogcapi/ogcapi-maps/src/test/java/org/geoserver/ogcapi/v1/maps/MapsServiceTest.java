/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.geoserver.catalog.ResourcePool;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;

/** Unit tests for the isolated {@link MapsService} helpers that need no running GeoServer. */
public class MapsServiceTest {

    // a transverse mercator with a non-standard central meridian, so it matches no EPSG code and has no identifier
    private static final String CUSTOM_WKT =
            """
            PROJCS["Custom TM",
              GEOGCS["WGS 84",
                DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563]],
                PRIMEM["Greenwich",0],UNIT["degree",0.0174532925199433]],
              PROJECTION["Transverse_Mercator"],
              PARAMETER["central_meridian",7.3],
              PARAMETER["latitude_of_origin",0],
              PARAMETER["scale_factor",0.9996],
              PARAMETER["false_easting",500000],
              PARAMETER["false_northing",0],
              UNIT["metre",1]]""";

    @Test
    public void testContentHeadersUnnamedCrs() throws Exception {
        CoordinateReferenceSystem custom = CRS.parseWKT(CUSTOM_WKT);
        assertNull("premise: the custom CRS has no identifier", ResourcePool.lookupIdentifier(custom, false));

        MapsService.ContentCrsBbox headers =
                MapsService.contentCrsAndBbox(new ReferencedEnvelope(1, 2, 3, 4, custom), null);
        assertNull("Content-Crs must be omitted, not <null>", headers.crs());
        assertEquals("1.0,3.0,2.0,4.0", headers.bbox());
    }

    /** A longitude-first WGS84 map is CRS84, which /req/core/map-response keeps out of the Content-Crs header. */
    @Test
    public void testContentHeadersCRS84() throws Exception {
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        MapsService.ContentCrsBbox headers =
                MapsService.contentCrsAndBbox(new ReferencedEnvelope(1, 2, 3, 4, wgs84), null);
        assertNull("CRS84 carries no Content-Crs", headers.crs());
        assertEquals("1.0,3.0,2.0,4.0", headers.bbox());
    }

    /** The SafeCURIE names the latitude-first authority CRS, so the header is emitted and the ordinates flip. */
    @Test
    public void testContentHeadersLatLonAxisOrder() throws Exception {
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        MapsService.ContentCrsBbox headers =
                MapsService.contentCrsAndBbox(new ReferencedEnvelope(1, 2, 3, 4, wgs84), "[EPSG:4326]");
        assertEquals("<http://www.opengis.net/def/crs/EPSG/0/4326>", headers.crs());
        // EPSG:4326 is latitude first, so the delivered order is minY,minX,maxY,maxX
        assertEquals("3.0,1.0,4.0,2.0", headers.bbox());
    }

    /** A projected CRS keeps its header, and its ordinates stay easting first. */
    @Test
    public void testContentHeadersProjected() throws Exception {
        CoordinateReferenceSystem webMercator = CRS.decode("EPSG:3857", true);
        MapsService.ContentCrsBbox headers =
                MapsService.contentCrsAndBbox(new ReferencedEnvelope(1, 2, 3, 4, webMercator), null);
        assertEquals("<http://www.opengis.net/def/crs/EPSG/0/3857>", headers.crs());
        assertEquals("1.0,3.0,2.0,4.0", headers.bbox());
    }
}
