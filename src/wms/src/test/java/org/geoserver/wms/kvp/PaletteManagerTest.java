/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.kvp;

import static org.junit.Assert.*;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import org.geotools.image.palette.InverseColorMapOp;
import org.junit.Test;

public class PaletteManagerTest {

    @Test
    public void testSameIndexColorModel() {
        IndexColorModel safePalette = PaletteManager.safePalette;
        // using the same palette we get back the same inverter (it's cached, not rebuilt)
        InverseColorMapOp op1 = PaletteManager.getInverseColorMapOp(safePalette);
        InverseColorMapOp op2 = PaletteManager.getInverseColorMapOp(safePalette);
        assertEquals(op1, op2);
    }

    @Test
    public void testDifferentColorModels() {
        IndexColorModel safePalette = PaletteManager.safePalette;
        IndexColorModel grayPalette = buildGrayPalette();
        InverseColorMapOp op1 = PaletteManager.getInverseColorMapOp(safePalette);
        InverseColorMapOp op2 = PaletteManager.getInverseColorMapOp(grayPalette);
        // the hashcode bug in IndexedColorModel would have made it return the same inverter
        assertNotEquals(op1, op2);
    }

    /** Builds a palette with the same structure as the safe one, but fully gray */
    static IndexColorModel buildGrayPalette() {
        int[] cmap = new int[256];

        // The gray scale. Make sure we end up with gray == 255
        final int opaqueAlpha = 255 << 24;
        for (int i = 0; i < 255; i++) {
            cmap[i] = opaqueAlpha | (i << 16) | (i << 8) | i;
        }

        // setup the transparent color (alpha == 0)
        cmap[255] = (255 << 16) | (255 << 8) | 255;

        // create the color model
        return new IndexColorModel(8, 256, cmap, 0, true, 255, DataBuffer.TYPE_BYTE);
    }
}
