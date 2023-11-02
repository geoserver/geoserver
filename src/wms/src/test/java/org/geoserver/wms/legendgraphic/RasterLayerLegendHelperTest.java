/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.brewer.styling.builder.ColorMapBuilder;
import org.junit.Test;

public class RasterLayerLegendHelperTest {

    @Test
    public void testRemoveDuplicatesNoLabel() throws Exception {
        // mix of entries some of which are sharing the color
        // (remember the value does not get into the legend with single value mode, users
        // can only use color and label to tell them apart)
        ColorMapBuilder cmb = new ColorMapBuilder();
        cmb.entry().quantity(1).color(Color.BLACK);
        cmb.entry().quantity(2).color(Color.BLACK);
        cmb.entry().quantity(3).color(Color.WHITE);
        cmb.entry().quantity(4).color(Color.RED);
        cmb.entry().quantity(5).color(Color.RED);
        cmb.entry().quantity(6).color(Color.BLACK);

        ColorMap cm = cmb.build();
        ColorMapEntry[] entries = cm.getColorMapEntries();

        ColorMapEntry[] valid = RasterLayerLegendHelper.removeDuplicates(entries);
        // could only use the color to tell them apart, there is no label
        assertEquals(3, valid.length);
        assertEquals(Color.BLACK, LegendUtils.color(valid[0]));
        assertEquals(Color.WHITE, LegendUtils.color(valid[1]));
        assertEquals(Color.RED, LegendUtils.color(valid[2]));
    }

    @Test
    public void testRemoveDuplicatesWithLabel() throws Exception {
        // mix of duplicates with different labels and same labels
        ColorMapBuilder cmb = new ColorMapBuilder();
        cmb.entry().quantity(1).color(Color.BLACK).label("one"); // first
        cmb.entry().quantity(1).color(Color.BLACK).label("one"); // dupe
        cmb.entry().quantity(1).color(Color.WHITE).label("one"); // not dupe, different color/label
        cmb.entry().quantity(10).color(Color.RED).label("two"); // not dupe
        cmb.entry().quantity(10).color(Color.RED).label("two"); // dupe
        cmb.entry().quantity(10).color(Color.BLACK).label("two"); // not duep, different color

        ColorMap cm = cmb.build();
        ColorMapEntry[] entries = cm.getColorMapEntries();

        ColorMapEntry[] valid = RasterLayerLegendHelper.removeDuplicates(entries);
        // could only use the color to tell them apart, there is no label
        assertEquals(4, valid.length);
        assertEquals(Color.BLACK, LegendUtils.color(valid[0]));
        assertEquals(Color.BLACK, LegendUtils.color(valid[0]));
        assertEquals(Color.WHITE, LegendUtils.color(valid[1]));
        assertEquals(Color.RED, LegendUtils.color(valid[2]));
    }
}
