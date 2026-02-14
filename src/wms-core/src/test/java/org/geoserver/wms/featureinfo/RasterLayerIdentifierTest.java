/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link RasterLayerIdentifier}. */
public class RasterLayerIdentifierTest {

    /** Test that a null description is converted to "Unknown". */
    @Test
    public void testNullDescriptionToNcCame() {
        Assert.assertEquals("Unknown", RasterLayerIdentifier.descriptionToNcName(null));
    }

    /** Test that an empty description is converted to "Unknown". */
    @Test
    public void testEmptyDescriptionToNcCame() {
        Assert.assertEquals("Unknown", RasterLayerIdentifier.descriptionToNcName(""));
    }

    /** Test that a description that is already a valid NCName is unchanged. */
    @Test
    public void testUnchangedDescriptionToNcCame() {
        Assert.assertEquals("Band", RasterLayerIdentifier.descriptionToNcName("Band"));
    }

    /** Test that a space (not permitted in an NCName) is replaced with an underscore. */
    @Test
    public void testSpaceDescriptionToNcCame() {
        Assert.assertEquals("Band_1", RasterLayerIdentifier.descriptionToNcName("Band 1"));
    }

    /** Test that a leading digit (not permitted in an NCName) is replaced with an underscore. */
    @Test
    public void testLeadingDigitDescriptionToNcCame() {
        Assert.assertEquals("_Band", RasterLayerIdentifier.descriptionToNcName("1Band"));
    }
}
