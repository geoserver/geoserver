/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.RenderedImage;
import java.util.stream.IntStream;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;

class Tally {

    static final long UNLIMITED = -1;

    private final long maxMemory;

    private long usedMemory;

    public Tally(long maxMemory) {
        this.maxMemory = maxMemory;
        this.usedMemory = 0;
    }

    public Tally(WMS wms) {
        if (wms != null && wms.getMaxRequestMemory() > 0) {
            this.maxMemory = wms.getMaxRequestMemory() * 1024L; // KB to bytes
        } else {
            this.maxMemory = UNLIMITED;
        }
    }

    public void addImage(RenderedImage image) {
        if (maxMemory != UNLIMITED) {
            long imageSize = computeImageSize(image);
            // compute sum as long to avoid overflow
            if (usedMemory + imageSize > maxMemory) {
                // we don't report the max value as this could be a sub-list
                throw new ServiceException(
                        LegendGraphicBuilder.MEMORY_USAGE_EXCEEDED, ServiceException.MAX_MEMORY_EXCEEDED);
            }
            usedMemory += imageSize;
        }
    }

    public static long computeImageSize(RenderedImage image) {
        int pixelSize = IntStream.of(image.getSampleModel().getSampleSize()).sum() / 8;
        return (long) image.getWidth() * image.getHeight() * (pixelSize);
    }

    /** Returns an object representing the residual memory still available before limits kicks in */
    public Tally getRemaining() {
        return new Tally(maxMemory - usedMemory);
    }

    /** Returns an object representing the maximum memory this Tally is tracking */
    public Tally getFull() {
        return new Tally(maxMemory);
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public long getUsedMemory() {
        return usedMemory;
    }
}
