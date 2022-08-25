/* (c) 2014 - 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2022 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

public class TalliedList<T> implements ImageSizeComputable {

    private final List<T> list = new ArrayList<>();

    private final Tally tally;

    public TalliedList(Tally tally) {
        this.tally = tally;
    }

    public TalliedList(Tally tally, TalliedQueue<? extends T> legendsQueue) {
        this.tally = tally;
        for (int i = 0; i < legendsQueue.size(); i++) {
            T image = legendsQueue.poll();
            list.add(image);
        }
    }

    public void add(T image) {
        increaseUsedMemory(image);
        list.add(image);
    }

    public List<T> get() {
        return list;
    }

    @SuppressWarnings("unchecked")
    private void increaseUsedMemory(T image) {
        RenderedImage renderedImage = (RenderedImage) image;
        int imageSize = computeImageSize(renderedImage);
        int usedMemory = tally.getUsedMemory();
        int maxMemory = tally.getMaxMemory();
        if (usedMemory + imageSize > maxMemory) {
            throw new IllegalArgumentException("Max memory per request exceeded.");
        }
        tally.setMaxMemory(usedMemory + imageSize);
    }
}
