/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RenderTimeStatisticsTest {
    List<Layer> layers;

    @Before
    public void setUp() {
        layers = new ArrayList<Layer>(2);
        layers.add(
                new Layer() {

                    public ReferencedEnvelope getBounds() {
                        return null;
                    }

                    @Override
                    public String getTitle() {
                        return "Layer1";
                    }
                });

        layers.add(
                new Layer() {

                    public ReferencedEnvelope getBounds() {
                        return null;
                    }

                    @Override
                    public String getTitle() {
                        return "Layer2";
                    }
                });
    }

    @Test
    public void testRenderingTimeStatistics() {
        RenderTimeStatistics statistics = new RenderTimeStatistics();
        ServletRequestAttributes attrs =
                new ServletRequestAttributes(createMockHttpRequest(statistics));
        RequestContextHolder.setRequestAttributes(attrs);
        for (Layer l : layers) {
            statistics.layerStart(l);
            statistics.labellingStart();
            statistics.labellingEnd();
            statistics.layerEnd(l);
        }
        statistics.renderingComplete();
        assertEquals(statistics.getRenderingLayersIdxs(), Arrays.asList(0, 1));
        assertEquals(statistics.getLayerNames().get(0), "Layer1");
        assertEquals(statistics.getLayerNames().get(1), "Layer2");
        assertNotNull(statistics.getRenderingTime(0));
        assertNotNull(statistics.getRenderingTime(1));
        assertNotNull(statistics.getLabellingTime());
        assertNotNull(attrs.getAttribute(RenderTimeStatistics.ID, 0));
    }

    public HttpServletRequest createMockHttpRequest(RenderTimeStatistics statistics) {
        HttpServletRequest httpReq = Mockito.mock(HttpServletRequest.class);

        httpReq.setAttribute(RenderTimeStatistics.ID, statistics);
        Mockito.when(httpReq.getAttribute(RenderTimeStatistics.ID)).thenReturn(statistics);
        Mockito.doNothing().when(httpReq).setAttribute(RenderTimeStatistics.ID, statistics);
        return httpReq;
    }
}
