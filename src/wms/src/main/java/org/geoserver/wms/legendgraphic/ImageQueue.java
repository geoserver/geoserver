/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A queue of images with memory control (provides just enough functionality to support legend
 * graphic construction and layout, not a generic Queue implementation). Will check the memory
 * limits and throw an exception if they are exceeded.
 */
class ImageQueue {

    private final Queue<BufferedImage> queue = new LinkedList<>();

    private final Tally tally;

    public ImageQueue(Tally tally) {
        this.tally = tally;
    }

    public void add(BufferedImage image) {
        tally.addImage(image);
        queue.add(image);
    }

    public void addAll(ImageQueue imageQueue) {
        for (int i = 0; i < imageQueue.size(); i++) {
            BufferedImage item = imageQueue.poll();
            queue.add(item);
        }
    }

    public BufferedImage remove() {
        return queue.remove();
    }

    public BufferedImage poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public Queue<BufferedImage> get() {
        return queue;
    }
}
