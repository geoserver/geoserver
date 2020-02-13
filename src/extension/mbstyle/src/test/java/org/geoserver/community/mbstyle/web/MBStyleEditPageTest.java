/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.mbstyle.web;

import static org.junit.Assert.*;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.data.OpenLayersPreviewPanel;
import org.geoserver.wms.web.data.StyleEditPage;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.PointSymbolizer;
import org.junit.Before;
import org.junit.Test;

public class MBStyleEditPageTest extends GeoServerWicketTestSupport {

    StyleInfo mbstyle;
    StyleEditPage edit;

    @Before
    public void setUp() throws Exception {
        Catalog catalog = getCatalog();
        login();

        mbstyle = new StyleInfoImpl(null);
        mbstyle.setName("mbstyle");
        mbstyle.setFilename("mbstyle.json");
        mbstyle.setFormat(MBStyleHandler.FORMAT);
        catalog.add(mbstyle);
        mbstyle = catalog.getStyleByName("mbstyle");
        catalog.save(mbstyle);

        edit = new StyleEditPage(mbstyle);
        tester.startPage(edit);
    }

    @Test
    public void testMbstyleChange() throws Exception {

        String json =
                "{\n"
                        + "  \"version\": 8, \n"
                        + "  \"name\": \"places\",\n"
                        + "  \"sprite\": \"http://localhost:8080/geoserver/styles/mbsprites\",\n"
                        + "  \"layers\": [\n"
                        + "    {\n"
                        + "      \"id\": \"circle\",\n"
                        + "      \"source-layer\": \"Buildings\",\n"
                        + "      \"type\": \"symbol\",\n"
                        + "      \"layout\": {\n"
                        + "        \"icon-image\": \"circle\",\n"
                        + "        \"icon-size\": {\n"
                        + "          \"property\": \"POP_MAX\",\n"
                        + "          \"type\": \"exponential\",\n"
                        + "          \"stops\": [\n"
                        + "            [0, 0.7],\n"
                        + "            [40000000, 3.7]\n"
                        + "          ]\n"
                        + "        }\n"
                        + "      }\n"
                        + "    }\n"
                        + "  ]\n"
                        + " }\n";

        FormTester form = tester.newFormTester("styleForm");
        form.setValue("context:panel:format", MBStyleHandler.FORMAT);
        form.setValue("context:panel:name", "mbstyleTest");
        tester.executeAjaxEvent("apply", "click");
        tester.executeAjaxEvent("styleForm:context:tabs-container:tabs:2:link", "click");
        tester.assertComponent("styleForm:context:panel", OpenLayersPreviewPanel.class);
        tester.assertModelValue("styleForm:context:panel:previewStyleGroup", false);
        form.setValue("context:panel:previewStyleGroup", true);
        form.setValue("styleEditor:editorContainer:editorParent:editor", json);
        tester.executeAjaxEvent("apply", "click");
        tester.assertModelValue("styleForm:context:panel:previewStyleGroup", true);
        assertNotNull(getCatalog().getStyleByName("mbstyle").getSLD());
        PointSymbolizer ps =
                (PointSymbolizer)
                        getCatalog()
                                .getStyleByName("mbstyle")
                                .getStyle()
                                .featureTypeStyles()
                                .get(0)
                                .rules()
                                .get(0)
                                .symbolizers()
                                .get(0);
        ExternalGraphic eg = (ExternalGraphic) ps.getGraphic().graphicalSymbols().get(0);
        assertEquals(
                eg.getURI(),
                "http://localhost:8080/geoserver/styles/mbsprites#icon=${strURLEncode('circle')}&size=${strURLEncode(Interpolate(POP_MAX,0,0.7,40000000,3.7,'numeric'))}");
    }
}
