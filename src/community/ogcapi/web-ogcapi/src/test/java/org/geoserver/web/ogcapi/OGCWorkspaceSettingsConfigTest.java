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
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.junit.After;
import org.junit.Before;

public class OGCWorkspaceSettingsConfigTest extends AbstractLinksEditorTest {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // not adding test data here
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void setupPage() {
        WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");

        GeoServer gs = getGeoServer();
        SettingsInfo settings = gs.getFactory().createSettings();
        settings.setWorkspace(cite);
        ArrayList<LinkInfo> links = new ArrayList<>();
        link =
                new LinkInfoImpl(
                        "enclosure",
                        "application/zip",
                        "http://www.geoserver.org/data/dataset.zip");
        link.setTitle("The cite workspace dataset");
        link.setService("Features");
        links.add(link);
        settings.getMetadata().put(LINKS_METADATA_KEY, links);
        getGeoServer().add(settings);

        // start page
        login();
        tester.startPage(new WorkspaceEditPage(cite));

        // figure out the links editor path (it's not predictable, it depends on the number of
        // setting panel extensions available in the classpath
        WebMarkupContainer extensions =
                (WebMarkupContainer)
                        tester.getLastRenderedPage()
                                .get(
                                        "form:tabs:panel:settings:settingsContainer:otherSettings:extensions");
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

    @After
    public void cleanUpSettings() throws Exception {
        WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        GeoServer gs = getGeoServer();
        SettingsInfo settings = gs.getSettings(cite);
        if (settings != null) gs.remove(settings);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<LinkInfo> getLinks() {
        WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        return getGeoServer().getSettings(cite).getMetadata().get(LINKS_METADATA_KEY, List.class);
    }

    @Override
    protected String getFormName() {
        return "form";
    }
}
