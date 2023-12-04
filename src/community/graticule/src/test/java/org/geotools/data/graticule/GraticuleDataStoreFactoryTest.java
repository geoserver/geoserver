package org.geotools.data.graticule;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class GraticuleDataStoreFactoryTest {
    @Test
    public void testParamParser() throws Throwable {
        ReferencedEnvelope bounds = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        bounds.expandToInclude(-180, -90);
        bounds.expandToInclude(180, 90);
        String text = GraticuleDataStoreFactory.BOUNDS.text(bounds);
        System.out.println(text);
        Object obs = GraticuleDataStoreFactory.BOUNDS.parse(text);
        System.out.println(obs);
    }
}
