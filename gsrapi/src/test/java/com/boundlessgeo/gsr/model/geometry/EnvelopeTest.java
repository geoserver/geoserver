/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnvelopeTest {

    /*
     * A.3.6: geometry/order - Verify that xmin is smaller than or equal to xmax and that ymin is smaller than or equal to ymax
     */

    @Test
    public void testIsValid() {
        double xmin1 = -105.89;
        double xmax1 = -77.09;
        double ymin1 = 25.8;
        double ymax1 = 68.56;

        double xmin2 = -105.89;
        double xmax2 = -107.09;
        double ymin2 = 25.8;
        double ymax2 = 68.56;

        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Envelope validEnvelope = new Envelope(xmin1, ymin1, xmax1, ymax1, spatialReference);
        Envelope invalidEnvelope = new Envelope(xmin2, ymin2, xmax2, ymax2, spatialReference);

        assertEquals(true, validEnvelope.isValid());
        assertEquals(false, invalidEnvelope.isValid());
    }
}
