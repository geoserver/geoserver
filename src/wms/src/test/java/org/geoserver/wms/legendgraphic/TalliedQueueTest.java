/* (c) 2014 - 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2022 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.junit.Test;
import org.mockito.Mockito;

public class TalliedQueueTest {

    @Test
    public void testAddingToTalliedQueueWithoutExceedingMemoryLimit() {
        // Given
        Tally tally = new Tally(100000);
        TalliedQueue<RenderedImage> talliedQueue = new TalliedQueue<>(tally);
        RenderedImage renderedImage = Mockito.mock(RenderedImage.class);
        SampleModel sampleModel = Mockito.mock(SampleModel.class);
        int[] sampleSize = {100, 100, 200, 500};
        Mockito.when(renderedImage.getSampleModel()).thenReturn(sampleModel);
        Mockito.when(sampleModel.getSampleSize()).thenReturn(sampleSize);
        Mockito.when(renderedImage.getWidth()).thenReturn(10);
        Mockito.when(renderedImage.getHeight()).thenReturn(10);

        // When
        talliedQueue.add(renderedImage);

        // Then
        assertEquals(1, talliedQueue.get().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceedingMaxMemoryWhenAddingToTallieQueue() {
        // Given
        Tally tally = new Tally(1000);
        TalliedQueue<RenderedImage> talliedQueue = new TalliedQueue<>(tally);
        RenderedImage renderedImage = Mockito.mock(RenderedImage.class);
        SampleModel sampleModel = Mockito.mock(SampleModel.class);
        int[] sampleSize = {100, 100, 200, 500};
        Mockito.when(renderedImage.getSampleModel()).thenReturn(sampleModel);
        Mockito.when(sampleModel.getSampleSize()).thenReturn(sampleSize);
        Mockito.when(renderedImage.getWidth()).thenReturn(10);
        Mockito.when(renderedImage.getHeight()).thenReturn(10);

        // When
        talliedQueue.add(renderedImage);
    }
}
