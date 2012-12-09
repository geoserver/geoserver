/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import static org.geoserver.wms.map.quantize.ColorUtils.*;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.map.quantize.ColorMap.ColorEntry;
import org.geoserver.wms.map.quantize.PackedHistogram.SortComponent;

/**
 * Analyzes a {@link RenderedImage} contents (using a Median Cut style algorithm) and builds an
 * optimal RGBA {@link ColorIndexer} that can be used to turn the RGBA image into a paletted one.
 */
public class Quantizer {

    static final Logger LOGGER = Logger.getLogger("Quantizer");

    boolean MEDIAN_SPLIT = true;

    boolean MEDIAN_BOX = true;

    float THRESHOLD = 0.5f;
    
    boolean subsample = false;

    int maxColors;

    public Quantizer(int maxColors) {
        this.maxColors = maxColors;
    }
    
    /**
     * Enables logarithmic subsampling
     * @return
     */
    public Quantizer subsample() {
        subsample = false;
        return this;
    }

    public ColorIndexer buildColorIndexer(RenderedImage image) {
        long totalPixelCount = (long) image.getWidth() * (long) image.getHeight();

        // build a histogram with a subsampling proportional to the log of the image size
        // (for very small images we pick one pixel every two, from 256 we switch to one every 3,
        // and so on)
        int subsx, subsy;
        if(subsample) {
            subsx = 1 + (int) (Math.log(image.getWidth()) / Math.log(8));
            subsy = 1 + (int) (Math.log(image.getHeight()) / Math.log(8));
        } else {
            subsx = 1;
            subsy = 1;
        }
        PackedHistogram histogram = new PackedHistogram(image, subsx, subsy);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Found " + histogram.size() + " unique colors with shift "
                    + histogram.getShift());
            LOGGER.fine("Histogram count " + histogram.pixelCount() + " and pixels "
                    + totalPixelCount);
        }
        int colors = Math.min(histogram.size(), maxColors);

        // setup the first box, that median cut will split in parts
        List<Box> boxes = new ArrayList<Box>();
        boxes.add(new Box(0, histogram.size(), totalPixelCount, histogram, null));

        // perform the box subdivision, first based on box pixel count, then on the box color volume
        // following up Leptonica's paper suggestions
        int sortSwitch = Math.round(colors * THRESHOLD);
        Comparator<Box> comparator = new SumComparator();
        Comparator<Box> volumeComparator = new VolumeComparator();
        while (boxes.size() < colors) {
            // locate a box that we can split
            int boxIndex = 0;
            for (; boxIndex < boxes.size(); boxIndex++) {
                if (boxes.get(boxIndex).colors > 1) {
                    break;
                }
            }

            // did we scan all of them and found nothing? If so, each box has one color, we're done
            if (boxIndex == boxes.size()) {
                break;
            }

            // scan the box contents, find min and max of each color component
            Box box = boxes.get(boxIndex);

            // get the span of each and sort on the component that has the largest span
            int spana = box.getAlphaSpan();
            int spanr = box.getRedSpan();
            int spang = box.getGreenSpan();
            int spanb = box.getBlueSpan();
            SortComponent sort;
            if (spana > spanr && spana > spanb && spana > spang) {
                sort = SortComponent.Alpha;
            } else if (spanr > spang && spanr > spanb) {
                sort = SortComponent.Red;
            } else if (spang > spanb) {
                sort = SortComponent.Green;
            } else {
                sort = SortComponent.Blue;
            }
            box.sort(sort);

            // split the box and add it to the list
            Box newBox = box.split();
            boxes.add(newBox);

            // sort based on size or volume
            if (comparator instanceof SumComparator && boxes.size() > sortSwitch) {
                comparator = volumeComparator;
            }
            Collections.sort(boxes, comparator);
        }

        // dump all the boxes
        if (LOGGER.isLoggable(Level.FINER)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < boxes.size(); i++) {
                Box b = boxes.get(i);
                sb.append("Box " + i + ", pixels: " + b.sum + " colors: " + b.colors + " volume: "
                        + b.getVolume() + " p*v: " + b.getVolume() * b.sum);
            }
            sb.append("\n");
            LOGGER.finer("Median cut resulted in the following boxes:\n" + sb);
        }
        
        // the png encoder goes bananas if we have a single color palette, in this
        // case we need to add an entry to the palette
        if(boxes.size() == 1) {
            boxes.add(boxes.get(0));
        }

        // all right, we have the set of boxes, now we have to pick a color from each.
        // A box might have a single color, but if it has many we have to pick and choose,
        // in such case we do a weighted sum of each component.
        // In this phase we use the real colors (so far we worked with the packed ones)
        PaletteEntry[] palette = new PaletteEntry[boxes.size()];
        int shift = histogram.getShift();
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            byte r, g, b, a;
            if (box.colors == 1) {
                // simple case, no need to mess around with averages
                int color = histogram.getColor(box.idx);
                r = (byte) red(color);
                g = (byte) green(color);
                b = (byte) blue(color);
                a = (byte) alpha(color);
            } else {
                if (MEDIAN_BOX) {
                    // just pick the middle one
                    int color = histogram.getColor(box.idx + box.colors / 2);
                    r = (byte) red(color);
                    g = (byte) green(color);
                    b = (byte) blue(color);
                    a = (byte) alpha(color);
                } else {
                    // compute the weighted sum
                    final int start = box.idx;
                    final int end = box.idx + box.colors;
                    long rs, gs, bs, as, sum;
                    rs = gs = bs = as = sum = 0;
                    for (int idx = start; idx < end; idx++) {
                        int color = histogram.getColor(idx);
                        long count = histogram.getCount(idx);
                        rs += red(color) * count;
                        gs += green(color) * count;
                        bs += blue(color) * count;
                        as += alpha(color) * count;
                        sum += count;
                    }
                    r = (byte) (rs / sum);
                    g = (byte) (gs / sum);
                    b = (byte) (bs / sum);
                    a = (byte) (as / sum);
                }

            }
            palette[i] = new PaletteEntry(r, g, b, a, i);
        }

        // sort palette, non opaque colors first, and build the rgba array
        Arrays.sort(palette);
        byte[][] rgba = new byte[4][palette.length];
        for (int i = 0; i < palette.length; i++) {
            PaletteEntry pe = palette[i];
            rgba[0][i] = pe.r;
            rgba[1][i] = pe.g;
            rgba[2][i] = pe.b;
            rgba[3][i] = pe.a;
        }

        // prepare the reverse map
        ColorIndexer simpleMapper = new SimpleColorIndexer(rgba);
        ColorMap colorMap = histogram.colorMap;
        for (ColorEntry ce : colorMap) {
            int color = ce.color;
            int r = red(color);
            int g = green(color);
            int b = blue(color);
            int a = alpha(color);
            if (shift > 0) {
                r = unshift(r, shift);
                g = unshift(g, shift);
                b = unshift(b, shift);
                a = unshift(a, shift);
            }
            int idx = simpleMapper.getClosestIndex(r, g, b, a) & 0xFF;
            ce.value = idx;
        }

        // dumpPalette(rgba);

        ColorIndexer delegate = new MappedColorIndexer(rgba, colorMap, shift);
        return new CachingColorIndexer(delegate);
    }

    /**
     * A median cut Box
     * 
     * @author Andrea Aime - GeoSolutions
     */
    final class Box {
        /**
         * Start index of the box in the histogram
         */
        private int idx;

        /**
         * Number of colors in the box
         */
        private int colors;

        /**
         * Pixel count
         */
        private long sum;

        /**
         * Current box sort
         */
        private SortComponent sort;

        /**
         * A reference to the packed histogram, used when computing color spans
         */
        private PackedHistogram histogram;

        /**
         * The cached color spans
         */
        private int spana = -1;

        private int spanb = -1;

        private int spanr = -1;

        private int spang = -1;

        public Box(int idx, int colors, long sum, PackedHistogram histogram, SortComponent sort) {
            this.idx = idx;
            this.colors = colors;
            this.histogram = histogram;
            this.sum = sum;
            this.sort = sort;
        }

        /**
         * Sorts the box along the specified component
         */
        public void sort(SortComponent sort) {
            if (this.sort != sort) {
                this.sort = sort;
                final int start = idx + 1;
                final int end = idx + colors;
                histogram.sort(start, end, sort);
            }
        }

        /**
         * Splits the box in two
         * 
         * @return
         */
        public Box split() {
            // accumulate entries until we partitioned the box into two halves by pixel count
            long fullsum = sum;
            long ps = histogram.getCount(idx);
            int i = idx + 1;
            if (MEDIAN_SPLIT) {
                final int mid = idx + colors / 2;
                for (; i < mid; i++) {
                    ps += histogram.getCount(i);
                }
            } else {
                final int end = idx + colors;
                long halfsum = fullsum / 2;
                for (; i < end - 1 && ps < halfsum; i++) {
                    ps += histogram.getCount(i);
                }
            }

            // update the current box to be the first half, and create a new box to be the second
            final int fullColors = colors;
            this.colors = i - idx;
            this.sum = ps;
            this.spana = -1;
            this.spanb = -1;
            this.spanr = -1;
            this.spang = -1;
            long rest = fullsum - ps;
            return new Box(i, fullColors - colors, rest, histogram, sort);
        }

        @Override
        public String toString() {
            return "Box [idx=" + idx + ", colors=" + colors + ", sum=" + sum + "]";
        }

        /**
         * The alpha span (higher alpha - lowest alpha)
         * 
         * @return
         */
        public int getAlphaSpan() {
            if (spana == -1) {
                updateSpans();
            }
            return spana;
        }

        /**
         * The red span (higher red - lowest red)
         * 
         * @return
         */
        public int getRedSpan() {
            if (spanr == -1) {
                updateSpans();
            }
            return spanr;
        }

        /**
         * The green span (highest green - lowest green)
         * 
         * @return
         */
        public int getGreenSpan() {
            if (spang == -1) {
                updateSpans();
            }
            return spang;
        }

        /**
         * The blue span (highest blue - lowest blue)
         * 
         * @return
         */
        public int getBlueSpan() {
            if (spanb == -1) {
                updateSpans();
            }
            return spanb;
        }

        /**
         * Updates the color spans going trought every color in the box
         */
        private void updateSpans() {
            int minr, maxr, ming, maxg, minb, maxb, mina, maxa;
            int color = histogram.getPackedColor(idx);
            minr = maxr = red(color);
            ming = maxg = green(color);
            minb = maxb = blue(color);
            mina = maxa = alpha(color);
            final int start = idx + 1;
            final int end = idx + colors;
            for (int i = start; i < end; i++) {
                color = histogram.getPackedColor(i);
                int r = red(color);
                if (r < minr) {
                    minr = r;
                } else if (r > maxr) {
                    maxr = r;
                }

                int g = green(color);
                if (g < ming) {
                    ming = g;
                } else if (g > maxg) {
                    maxg = g;
                }

                int b = blue(color);
                if (b < minb) {
                    minb = b;
                } else if (b > maxb) {
                    maxb = b;
                }

                int a = alpha(color);
                if (a < mina) {
                    mina = a;
                } else if (a > maxa) {
                    maxa = a;
                }
            }

            // get the span of each and sort on the component that has the largest span
            this.spana = maxa - mina;
            this.spanr = maxr - minr;
            this.spang = maxg - ming;
            this.spanb = maxb - minb;
        }

        /**
         * Returns the "volume" of the box in the RGBA 4-dimensional space
         * 
         * @return
         */
        public long getVolume() {
            if (spana == -1) {
                updateSpans();
            }
            return ((spana + 1l) * (spanr + 1l) * (spang + 1l) * (spanb + 1l))
                    * shift(1, histogram.getShift());
        }

    }

    /**
     * Compares two boxes by pixel count, but makes sure the boxes that cannot be split are at the
     * end of the array
     * 
     * @author Andrea Aime - GeoSolutions
     * 
     */
    static final class SumComparator implements Comparator<Box> {

        @Override
        public int compare(Box b1, Box b2) {
            // move all boxes with just one color at the end, we can't split them anyways
            if (b1.colors == 1) {
                if (b2.colors == 1) {
                    return compareLong(b2.sum, b1.sum);
                } else {
                    return 1;
                }
            } else if (b2.colors == 1) {
                return -1;
            }
            return compareLong(b2.sum, b1.sum);
        }

    }

    /**
     * Compares two boxes by volume weighted by pixel count, but makes sure the boxes that cannot be
     * split are at the end of the array
     * 
     * @author Andrea Aime - GeoSolutions
     * 
     */

    static final class VolumeComparator implements Comparator<Box> {

        @Override
        public int compare(Box b1, Box b2) {
            // move all boxes with just one color at the end, we can't split them anyways
            if (b1.colors == 1) {
                if (b2.colors == 1) {
                    return compareLong(b2.sum, b1.sum);
                } else {
                    return 1;
                }
            } else if (b2.colors == 1) {
                return -1;
            }
            long vs2 = b2.getVolume() * b2.sum;
            long vs1 = b1.getVolume() * b1.sum;
            long diff = vs2 - vs1;
            if (diff == 0) {
                return 0;
            } else if (diff > 0) {
                return 1;
            } else {
                return -1;
            }
        }

    }

    /**
     * An entry in the final palette.
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static final class PaletteEntry implements Comparable<PaletteEntry> {
        byte r;

        byte g;

        byte b;

        byte a;

        int idx;

        public PaletteEntry(byte r, byte g, byte b, byte a, int idx) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.idx = idx;
        }

        @Override
        public int compareTo(PaletteEntry other) {
            return (a & 0xFF) - (other.a & 0xFF);
        }

    }

}
