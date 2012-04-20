/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import java.awt.image.IndexColorModel;

/**
 * Palette that re-uses the ColorMap used to build the palette itsel to speedup the lookups. When
 * there is no shift every color found in the map can be also found in the color map.
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class MappedColorIndexer implements ColorIndexer {

    byte[][] colors;

    ColorMap colorMap;

    int shift;

    SimpleColorIndexer delegate;

    /**
     * Builds a new {@link MappedColorIndexer}
     * 
     * @param colors The target palette
     * @param colorMap The color map used to build the palette, mapping from shifted colors to the
     *        palette index
     * @param shift The bit shift applied while building the palette
     */
    public MappedColorIndexer(byte[][] colors, ColorMap colorMap, int shift) {
        this.colors = colors;
        this.shift = shift;
        this.colorMap = colorMap;
        this.delegate = new SimpleColorIndexer(colors);
    }

    public IndexColorModel toIndexColorModel() {
        return delegate.toIndexColorModel();
    }

    public int getClosestIndex(final int r, final int g, final int b, final int a) {
        int sr = r;
        int sg = g;
        int sb = b;
        int sa = a;
        if (shift > 0) {
            sr = r >> shift;
            sg = g >> shift;
            sb = b >> shift;
            sa = a >> shift;
        }
        if (a <= PackedHistogram.ALPHA_THRESHOLD) {
            sr = 255;
            sg = 255;
            sb = 255;
            sa = 0;
        }

        synchronized (colorMap) {
            int idx = colorMap.get(sr, sg, sb, sa);
            if (idx < 0) {
                idx = delegate.getClosestIndex(r, g, b, a);
                colorMap.put(sr, sg, sb, sa, idx);
            }
            return idx;
        }
    }

}
