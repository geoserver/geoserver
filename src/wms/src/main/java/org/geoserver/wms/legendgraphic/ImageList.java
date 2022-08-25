/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * List of images with max memory control. (provides just enough functionality to support legend
 * graphic construction and layout, not a generic List implementation). Will check the memory limits
 * and throw an exception if they are exceeded.
 */
class ImageList implements Iterable<BufferedImage> {

    private final List<BufferedImage> list = new ArrayList<>();

    private final Tally tally;

    public ImageList(Tally tally) {
        this.tally = tally;
    }

    public ImageList(Tally tally, ImageQueue legendsQueue) {
        this.tally = tally;
        list.addAll(legendsQueue.get());
    }

    public void add(BufferedImage image) {
        tally.addImage(image);
        list.add(image);
    }

    public int size() {
        return list.size();
    }

    public BufferedImage get(int index) {
        return list.get(index);
    }

    /**
     * Returns the memory tally
     *
     * @return
     */
    public Tally getTally() {
        return tally;
    }

    @Override
    public Iterator<BufferedImage> iterator() {
        return list.iterator();
    }
}
