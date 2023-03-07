/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.junit.Before;

public class OGCLayerConfigTest extends AbstractLinksEditorTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(SystemTestData.LAKES, getCatalog());
    }

    private FeatureTypeInfo getLakes() {
        String lakesName = getLayerId(SystemTestData.LAKES);
        return getCatalog().getResourceByName(lakesName, FeatureTypeInfo.class);
    }

    @Before
    public void setupPage() {
        FeatureTypeInfo lakes = getLakes();
        ArrayList<LinkInfo> links = new ArrayList<>();
        link =
                new LinkInfoImpl(
                        "enclosure", "application/zip", "http://www.geoserver.org/data/lakes.zip");
        link.setTitle("A few very simple lakes");
        link.setService("Features");
        links.add(link);
        lakes.getMetadata().put(LinkInfo.LINKS_METADATA_KEY, links);

        // start page and switch to the publishing tab
        login();
        tester.startPage(new ResourceConfigurationPage(lakes, false));
        tester.newFormTester("publishedinfo").submit("tabs:tabs-container:tabs:1:link");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<LinkInfo> getLinks() {
        return getLakes().getMetadata().get(LinkInfo.LINKS_METADATA_KEY, List.class);
    }

    @Override
    protected String getFormName() {
        return "publishedinfo";
    }
}
