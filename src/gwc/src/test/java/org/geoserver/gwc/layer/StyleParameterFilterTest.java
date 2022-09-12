/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.GWCTestHelpers;
import org.junit.Test;

/**
 * Covers functionality of {@link StyleParameterFilter}.
 *
 * @author jbegic
 */
public class StyleParameterFilterTest {
    /**
     * Test covers a scenario where multiple threads are concurrently invoking {@link
     * StyleParameterFilter#setLayer(LayerInfo)} method. Purpose of the test is to make sure that
     * concurrent modification will not lead to corruption of internal state of the parameter filter
     * class (TreeSet is not thread-safe and does not support concurrent modifications).
     *
     * <p>Corruption manifests it-self as infinite loop in the internal tree set structure which
     * leads to out of memory errors when attempts are made to iterate through the set.
     *
     * <p>See more at:
     * https://ivoanjo.me/blog/2018/07/21/writing-to-a-java-treemap-concurrently-can-lead-to-an-infinite-loop-during-reads/
     *
     * @throws Exception
     */
    @Test(timeout = 10000L)
    public void testConcurrentModification() throws Exception {
        StyleParameterFilter filter = new StyleParameterFilter();

        String[] styleNames = {
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        };

        LayerInfo layerInfo =
                GWCTestHelpers.mockLayer(
                        UUID.randomUUID().toString(), styleNames, PublishedType.WMS);

        int concurrency = 10;
        int opsPerThread = 1000;

        List<Thread> modifierThreads = new ArrayList<>(concurrency);

        for (int i = 0; i < concurrency; i++) {
            Thread thread =
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    for (int j = 0; j < opsPerThread; j++) {
                                        filter.setLayer(layerInfo);
                                    }
                                }
                            });
            modifierThreads.add(thread);
        }

        for (Thread modifierThread : modifierThreads) {
            modifierThread.start();
        }

        for (Thread modifierThread : modifierThreads) {
            modifierThread.join();
        }

        String value = styleNames[0];

        assertEquals(value, filter.apply(value));
    }
}
