/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;

import org.geotools.geometry.GeneralEnvelope;
import org.junit.Test;

public class WCSEnvelopeTest {

    @Test
    public void testDatelineCrossing() {
        WCSEnvelope env = new WCSEnvelope(WGS84);
        env.setRange(0, 150, 210);
        env.setRange(1, -10, 10);

        // normalize, should split in two
        GeneralEnvelope[] envelopes = env.getNormalizedEnvelopes();
        assertEquals(2, envelopes.length);
        assertEquals(newGeneralEnvelope(150, -10, 180, 10), envelopes[0]);
        assertEquals(newGeneralEnvelope(-180, -10, -150, 10), envelopes[1]);

        // intersection test
        env.intersect(newGeneralEnvelope(160, -20, 180, 20));
        assertEquals(newGeneralEnvelope(160, -10, 180, 10), new GeneralEnvelope(env));
    }

    @Test
    public void testMoreThanWorldWest() {
        WCSEnvelope env = new WCSEnvelope(WGS84);
        env.setRange(0, -200, 165);
        env.setRange(1, -10, 10);

        // normalize should just give the whole world
        GeneralEnvelope[] envelopes = env.getNormalizedEnvelopes();
        assertEquals(1, envelopes.length);
        assertEquals(newGeneralEnvelope(-180, -10, 180, 10), envelopes[0]);

        // intersection test
        env.intersect(newGeneralEnvelope(160, -20, 180, 20));
        assertEquals(newGeneralEnvelope(160, -10, 180, 10), new GeneralEnvelope(env));
    }

    @Test
    public void testMoreThanWorldEast() {
        WCSEnvelope env = new WCSEnvelope(WGS84);
        env.setRange(0, -160, 240);
        env.setRange(1, -10, 10);

        // normalize should just give the whole world
        GeneralEnvelope[] envelopes = env.getNormalizedEnvelopes();
        assertEquals(1, envelopes.length);
        assertEquals(newGeneralEnvelope(-180, -10, 180, 10), envelopes[0]);

        // intersection test
        env.intersect(newGeneralEnvelope(160, -20, 180, 20));
        assertEquals(newGeneralEnvelope(160, -10, 180, 10), new GeneralEnvelope(env));
    }

    private static GeneralEnvelope newGeneralEnvelope(
            int minLon, int minLat, int maxLon, int maxLat) {
        GeneralEnvelope expected =
                new GeneralEnvelope(new double[] {minLon, minLat}, new double[] {maxLon, maxLat});
        expected.setCoordinateReferenceSystem(WGS84);
        return expected;
    }
}
