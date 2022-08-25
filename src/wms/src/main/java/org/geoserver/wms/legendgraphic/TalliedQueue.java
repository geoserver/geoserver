/* (c) 2014 - 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2022 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.util.LinkedList;
import java.util.Queue;

public class TalliedQueue<T> implements ImageSizeComputable {

    private final Queue<T> queue = new LinkedList<>();

    private final Tally tally;

    public TalliedQueue(Tally tally) {
        this.tally = tally;
    }

    public void add(T image) {
        increaseUsedMemory(image);
        queue.add(image);
    }

    public void addAll(TalliedQueue<T> talliedQueue) {
        for (int i = 0; i < talliedQueue.size(); i++) {
            queue.add(talliedQueue.poll());
        }
    }

    public T remove() {
        return queue.remove();
    }

    public T poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public Queue<T> get() {
        return queue;
    }

    @SuppressWarnings("unchecked")
    private void increaseUsedMemory(T image) {
        int imageSize = computeImageSize(image);
        int usedMemory = tally.getUsedMemory();
        int maxMemory = tally.getMaxMemory();
        if (usedMemory + imageSize > maxMemory) {
            throw new IllegalArgumentException("Max memory per request exceeded.");
        }
        tally.setMaxMemory(usedMemory + imageSize);
    }
}
