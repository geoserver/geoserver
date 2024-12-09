/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import static org.geoserver.ogcapi.LinkInfo.LINKS_METADATA_KEY;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.junit.Before;

public class OGCSettingsConfigTest extends AbstractLinksEditorTest {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // not adding test data here
    }

    @Before
    public void setupPage() {
        GeoServerInfo gsi = getGeoServer().getGlobal();
        SettingsInfo settings = gsi.getSettings();
        ArrayList<LinkInfo> links = new ArrayList<>();
        link =
                new LinkInfoImpl(
                        "enclosure",
                        "application/zip",
                        "http://www.geoserver.org/data/dataset.zip");
        link.setTitle("The whole dataset published by GeoServer");
        link.setService("Features");
        links.add(link);
        settings.getMetadata().put(LINKS_METADATA_KEY, links);
        getGeoServer().save(gsi);

        // start page
        login();
        tester.startPage(GlobalSettingsPage.class);

        // figure out the links editor path (it's not predictable, it depends on the number of
        // setting panel extensions available in the classpath
        WebMarkupContainer extensions =
                (WebMarkupContainer) tester.getLastRenderedPage().get("form:extensions");
        extensions.visitChildren(
                Component.class,
                (component, visit) -> {
                    if (component instanceof LinkInfoEditor) {
                        visit.stop();
                        EDITOR = component.getPageRelativePath();
                        EDITOR_FT = EDITOR.substring(EDITOR.indexOf(":") + 1);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<LinkInfo> getLinks() {
        return getGeoServer().getSettings().getMetadata().get(LINKS_METADATA_KEY, List.class);
    }

    @Override
    protected String getFormName() {
        return "form";
    }
}
