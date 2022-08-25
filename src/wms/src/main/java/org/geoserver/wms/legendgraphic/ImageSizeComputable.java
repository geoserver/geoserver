/* (c) 2014 - 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2022 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.RenderedImage;
import java.util.stream.IntStream;

public interface ImageSizeComputable<T> {
    default int computeImageSize(T image) {
        if (image instanceof RenderedImage) {
            RenderedImage renderedImage = (RenderedImage) image;
            int pixelSize = IntStream.of(renderedImage.getSampleModel().getSampleSize()).sum() / 8;
            return renderedImage.getWidth() * renderedImage.getHeight() * (pixelSize);
        }
        throw new RuntimeException("Unsupported image class: " + image.getClass().getName());
    }
}
