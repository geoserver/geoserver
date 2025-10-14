/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.mbstyle.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.SLD;
import org.junit.Test;

public class MBStyleHandlerTest extends GeoServerSystemTestSupport {

    @Test
    public void testParseThroughStyles() throws IOException {
        String mbstyle =
                """
                {"layers": [{
                    "type": "line",
                    "paint": {
                        "line-color": "#0099ff",
                        "line-width": 10,
                    }
                }]}\
                """;
        StyledLayerDescriptor sld = Styles.handler(MBStyleHandler.FORMAT).parse(mbstyle, null, null, null);
        assertNotNull(sld);

        LineSymbolizer ls = SLD.lineSymbolizer(Styles.style(sld));
        assertNotNull(ls);
    }

    @Test
    public void testRoundTripMBStyleGroup() throws IOException {
        Catalog catalog = getCatalog();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("citeGroup");
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.LAKES)));
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS)));
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES)));
        lg.getStyles().add(null);
        lg.getStyles().add(null);
        lg.getStyles().add(null);

        catalog.add(lg);

        StyledLayerDescriptor sld = Styles.handler(MBStyleHandler.FORMAT)
                .parse(getClass().getResourceAsStream("citeGroup.json"), null, null, null);

        assertEquals(3, sld.getStyledLayers().length);

        StyleHandler sldHandler = Styles.handler(SLDHandler.FORMAT);
        File sldFile = Files.createTempFile("citeGroup", "sld").toFile();
        try (OutputStream fout = new FileOutputStream(sldFile)) {
            sldHandler.encode(sld, SLDHandler.VERSION_10, true, fout);

            StyledLayerDescriptor sld2 =
                    sldHandler.parse(new FileInputStream(sldFile), SLDHandler.VERSION_10, null, null);
            assertEquals(3, sld2.getStyledLayers().length);
        }
    }
}
