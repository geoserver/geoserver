/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.geoserver.data.test.CiteTestData.ROAD_SEGMENTS;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class MetadataLinkEditorTest extends GeoServerWicketTestSupport {

    private static final String METADATA_TYPE = "FGDC";
    private static final String ABOUT = "http://www.geoserver.org/about";
    private static final String FORMAT = "text/xml";
    private static final String METADATA_LINK = "http://www.geoserver.org/meta";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data here
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // just need a single layer
        testData.addVectorLayer(ROAD_SEGMENTS, getCatalog());
    }

    @Test
    public void testEditLinks() throws Exception {
        FeatureTypeInfo featureType = getCatalog().getFeatureTypeByName(getLayerId(ROAD_SEGMENTS));
        assertEquals(0, featureType.getMetadataLinks().size());
        tester.startPage(
                new FormTestPage(
                        (ComponentBuilder)
                                id -> new MetadataLinkEditor(id, new Model<>(featureType))));

        tester.executeAjaxEvent("form:panel:addlink", "click");
        print(tester.getLastRenderedPage(), true, true, true);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:container:table:links:0:type", METADATA_TYPE);
        ft.setValue("panel:container:table:links:0:about", ABOUT);
        ft.setValue("panel:container:table:links:0:format", FORMAT);
        ft.setValue(
                "panel:container:table:links:0:urlBorder:urlBorder_body:metadataLinkURL",
                METADATA_LINK);
        ft.submit();

        // check the link as edited
        List<MetadataLinkInfo> links = featureType.getMetadataLinks();
        assertEquals(1, links.size());
        MetadataLinkInfo link = links.get(0);
        assertEquals(METADATA_TYPE, link.getMetadataType());
        assertEquals(ABOUT, link.getAbout());
        assertEquals(FORMAT, link.getType());
        assertEquals(METADATA_LINK, link.getContent());
    }
}
