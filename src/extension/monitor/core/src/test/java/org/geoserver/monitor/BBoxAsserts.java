/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.api.geometry.BoundingBox;

public class BBoxAsserts {

    /** Asserts two BoundingBoxes are equal to within delta. */
    public static void assertEqualsBbox(BoundingBox expected, BoundingBox result, double delta) {
        assertNotNull("Expected %s but got null".formatted(expected), result);
        assertEquals(expected.getMaxX(), result.getMaxX(), delta);
        assertEquals(expected.getMinX(), result.getMinX(), delta);
        assertEquals(expected.getMaxY(), result.getMaxY(), delta);
        assertEquals(expected.getMinY(), result.getMinY(), delta);
        assertEquals(expected.getCoordinateReferenceSystem(), result.getCoordinateReferenceSystem());
    }
}
