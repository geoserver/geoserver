/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.awt.Font;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geotools.renderer.style.FontCache;
import org.junit.Assume;
import org.junit.Test;

public class LegendUtilsTest {

    @Test
    public void testGetLabelFromCache() throws Exception {
        Assume.assumeTrue(FontCache.getDefaultInstance().getFont("Anarchist Mustache") == null);

        GetLegendGraphicRequest req = new GetLegendGraphicRequest();
        req.setLegendOptions(
                ImmutableMap.<String, String>builder()
                        .put("fontName", "Anarchist Mustache")
                        .build());

        Font f1 = LegendUtils.getLabelFont(req);
        assertEquals("Dialog", f1.getFamily());

        Font f =
                Font.createFont(
                        Font.TRUETYPE_FONT,
                        LegendUtilsTest.class.getResourceAsStream("Anarchist_Mustache.ttf"));
        FontCache.getDefaultInstance().registerFont(f);
        Font f2 = LegendUtils.getLabelFont(req);
        assertEquals("Anarchist Mustache", f2.getName());
    }
}
