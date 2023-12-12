/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Test;

public class GraticuleDataStoreFactoryTest {
    @Test
    public void testParamParserWGS84() throws Throwable {
        ReferencedEnvelope bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        checkBounds(bounds);
    }

    @Test
    public void testParamParserOSGB() throws Throwable {
        ReferencedEnvelope bounds = new ReferencedEnvelope(CRS.decode("EPSG:27700"));
        bounds.expandToInclude(0, 0);
        bounds.expandToInclude(700000, 1300000);
        checkBounds(bounds);
    }

    private static void checkBounds(ReferencedEnvelope bounds) throws Throwable {
        String text = GraticuleDataStoreFactory.BOUNDS.text(bounds);
        Object obs = GraticuleDataStoreFactory.BOUNDS.parse(text);
        Assert.assertEquals(obs, bounds);
    }
}
