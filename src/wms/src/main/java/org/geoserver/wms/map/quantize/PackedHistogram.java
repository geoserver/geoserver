/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import static org.geoserver.wms.map.quantize.ColorUtils.*;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.Comparator;

import org.geoserver.wms.map.quantize.ColorMap.ColorEntry;

/**
 * Builds a histogram of a certain image making sure that we don't end up with too many entries. If
 * the unique colors in the image go above {@link Short#MAX_VALUE} the histogram will apply a bit
 * shift on each color component in order to reduce the unique color count. Most vector maps with
 * alpha channel and antialiasing don't actually need the shift, but satellite and aerial imagery
 * often does
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
class PackedHistogram {

    static final int ALPHA_THRESHOLD = 5;

    /**
     * An entry in the histogram
     */
    static final class HistogramBin {
        int color;

        long count;

        public HistogramBin(int color, long count) {
            this.color = color;
            this.count = count;
        }

    }

    /**
     * Compares two colors based on a specific color component (determined by the subclass)
     */
    static abstract class ColorComparator implements Comparator<HistogramBin> {

        @Override
        public int compare(HistogramBin p1, HistogramBin p2) {
            int c1 = getComponent(p1);
            int c2 = getComponent(p2);
            if (c1 != c2) {
                return c1 - c2;
            } else {
                return compareLong(p1.count, p2.count);
            }
        }

        abstract protected int getComponent(HistogramBin p2);

    }

    /**
     * Enumerates the comparators for the various color components
     */
    public enum SortComponent {
        Red(new ColorComparator() {

            @Override
            protected final int getComponent(HistogramBin pe) {
                return red(pe.color);
            }

        }), Green(new ColorComparator() {

            @Override
            protected final int getComponent(HistogramBin pe) {
                return green(pe.color);
            }

        }), Blue(new ColorComparator() {

            @Override
            protected final int getComponent(HistogramBin pe) {
                return blue(pe.color);
            }

        }), Alpha(new ColorComparator() {

            @Override
            protected final int getComponent(HistogramBin pe) {
                return alpha(pe.color);
            }

        });

        ColorComparator comparator;

        private SortComponent(ColorComparator comparator) {
            this.comparator = comparator;
        }
    };

    private int shift = 0;

    private HistogramBin[] histogram;

    ColorMap colorMap;

    PackedHistogram(RenderedImage image, int stepX, int stepY) {
        // build a reduced map of the colors
        colorMap = new ColorMap();

        final int minX = image.getMinTileX();
        final int maxX = minX + image.getNumXTiles();
        final int minY = image.getMinTileY();
        final int maxY = minY + image.getNumYTiles();
        this.shift = 0;
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                Raster tile = image.getTile(x, y);
                shift = updateColorMap(image, tile, colorMap, shift, stepX, stepY);
            }
        }

        // turn it into an array for indexed access and reduced memory consumption
        histogram = new HistogramBin[colorMap.size()];
        int i = 0;
        for (ColorEntry ce : colorMap) {
            histogram[i++] = new HistogramBin(ce.color, ce.value);
        }
        // System.out.println("Histogram stats");
        // colorMap.printStats();
    }

    private int updateColorMap(RenderedImage image, Raster tile, ColorMap colorMap, int shift,
            int stepX, int stepY) {
        final int minX = Math.max(tile.getMinX(), image.getMinX());
        final int maxX = Math.min(tile.getWidth() + tile.getMinX(),
                image.getMinX() + image.getWidth());
        final int minY = Math.max(tile.getMinY(), image.getMinY());
        final int maxY = Math.min(tile.getHeight() + tile.getMinY(),
                image.getMinY() + image.getHeight());
        int bands = tile.getNumBands();
        int[] pixel = new int[bands];
        for (int x = minX; x < maxX; x += stepX) {
            for (int y = minY; y < maxY; y += stepY) {
                // grab the pixel and the color
                tile.getPixel(x, y, pixel);
                int red, green, blue, alpha;

                if(bands == 1 || bands == 2) {
                    red = green = blue = pixel[0];
                    alpha = bands == 2 ? pixel[1] : 255;
                } else  {
                    red = pixel[0];
                    green = pixel[1];
                    blue = pixel[2];
                    alpha = bands == 4 ? pixel[3] : 255;
                }

                // normalize colors with very low alpha = 0 to just one
                if (alpha <= ALPHA_THRESHOLD) {
                    red = 255;
                    green = 255;
                    blue = 255;
                    alpha = 0;
                }

                if (shift > 0) {
                    red = shift(red, shift);
                    green = shift(green, shift);
                    blue = shift(blue, shift);
                    alpha = shift(alpha, shift);
                }
                colorMap.increment(red, green, blue, alpha);

                if (colorMap.size() > Short.MAX_VALUE) {
                    shift++;
                    shiftColorMap(colorMap);
                }
            }
        }

        return shift;
    }

    /**
     * Shifts every color in the map by one more bit and repacks the color map accordingly
     * 
     * @param colorMap
     */
    private void shiftColorMap(ColorMap colorMap) {
        ColorMap shifted = new ColorMap();
        for (ColorEntry entry : colorMap) {
            int color = entry.color;
            int count = entry.value;
            int alpha = shift(alpha(color), 1);
            int red = shift(red(color), 1);
            int green = shift(green(color), 1);
            int blue = shift(blue(color), 1);

            shifted.increment(red, green, blue, alpha, count);
        }

        assert countPixels(colorMap) == countPixels(shifted);

        colorMap.reset(shifted);
    }

    private long countPixels(ColorMap colorMap) {
        long sum = 0;
        for (ColorEntry entry : colorMap) {
            sum += entry.value;
        }

        return sum;
    }

    public int size() {
        return histogram.length;
    }

    long pixelCount() {
        long count = 0;
        for (HistogramBin bin : histogram) {
            count += bin.count;
        }
        return count;
    }

    public int getPackedColor(int i) {
        return histogram[i].color;
    }

    public int getColor(int i) {
        int color = histogram[i].color;
        if (shift > 0) {
            int alpha = unshift(alpha(color), shift);
            int red = unshift(red(color), shift);
            int green = unshift(green(color), shift);
            int blue = unshift(blue(color), shift);
            color = color(red, green, blue, alpha);
        }
        return color;
    }

    public long getCount(int i) {
        return histogram[i].count;
    }

    public void sort(int start, int end, SortComponent sort) {
        Arrays.sort(histogram, start, end, sort.comparator);
    }

    public int getShift() {
        return shift;
    }

    public void clear() {
        histogram = null;
    }

}
