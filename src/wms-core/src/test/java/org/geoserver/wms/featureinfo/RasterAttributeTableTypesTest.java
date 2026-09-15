/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.RasterAttributeTableTypes.getBinding;
import static org.geoserver.wms.featureinfo.RasterAttributeTableTypes.toValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldType;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;

/** Checks the cell conversions on values a well formed table does not carry. */
public class RasterAttributeTableTypesTest {

    @Test
    public void testUnreadableValues() {
        assertNull(toValue(FieldType.Integer, "twelve"));
        assertNull(toValue(FieldType.Real, "12,5"));
        assertNull(toValue(FieldType.DateTime, "2024-03-01"));
        assertNull(toValue(FieldType.Boolean, "1"));
    }

    @Test
    public void testEmptyValues() {
        assertNull(toValue(FieldType.Integer, ""));
        assertNull(toValue(FieldType.Real, ""));
        assertNull(toValue(FieldType.Boolean, ""));
        assertNull(toValue(FieldType.DateTime, ""));
        // a text attribute can carry an empty value as it is
        assertEquals("", toValue(FieldType.String, ""));
        assertEquals("", toValue(FieldType.WKBGeometry, ""));
    }

    @Test
    public void testMissingType() {
        // a field with no type element, the value stays text
        assertEquals(String.class, getBinding(null));
        assertEquals("54602", toValue(null, "54602"));
        assertNull(toValue(null, null));
    }

    @Test
    public void testQuarterHourOffset() {
        // GDAL writes offsets of 0, 15, 30 and 45 minutes
        assertEquals(
                Date.from(Instant.parse("2025-01-02T22:44:59Z")),
                toValue(FieldType.DateTime, "2025-01-02T23:59:59.000+01:15"));
    }

    @Test
    public void testSurroundingSpaces() {
        assertEquals(12l, toValue(FieldType.Integer, " 12 "));
        assertEquals(
                Date.from(Instant.parse("2024-03-01T00:00:00Z")),
                toValue(FieldType.DateTime, " 2024-03-01T00:00:00Z "));
    }
}
