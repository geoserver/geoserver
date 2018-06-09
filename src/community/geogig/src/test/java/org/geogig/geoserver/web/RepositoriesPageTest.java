/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.model.DropDownModel;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.junit.Test;

/** */
public class RepositoriesPageTest extends GeoServerWicketTestSupport {

    private RepositoriesPage repoPage;

    private void navigateToRepositoriesPage() {
        login();
        repoPage = new RepositoriesPage();
        tester.startPage(repoPage);
    }

    @Test
    public void testAddNewPanel() {
        navigateToRepositoriesPage();
        // click the Add New link in the header panel
        tester.clickLink("headerPanel:addNew");
        // verify the page is a RepositoriesEditPage
        tester.assertRenderedPage(RepositoryEditPage.class);
        // verify the type dropdown
        DropDownChoiceParamPanel panel =
                (DropDownChoiceParamPanel)
                        tester.getComponentFromLastRenderedPage(
                                "panel:repoForm:repo:repositoryConfig:configChoicePanel");
        DropDownChoice<Serializable> choice = panel.getFormComponent();
        // make sure Directory is selected
        assertEquals(
                "Expected DropDwon to be set to " + DropDownModel.DIRECTORY_CONFIG,
                DropDownModel.DIRECTORY_CONFIG,
                choice.getModelObject());
        // verify that Directory components are visible
        final String settings = "panel:repoForm:repo:repositoryConfig:settingsContainer:";
        tester.assertVisible(settings + "repositoryNamePanel");
        tester.assertVisible(settings + "parentDirectory");
        tester.assertInvisible(settings + "pgPanel");
        // now select PG config
        FormTester formTester = tester.newFormTester("panel:repoForm");
        formTester.select(
                "repo:repositoryConfig:configChoicePanel:border:border_body:paramValue",
                choice.getChoices().indexOf(DropDownModel.PG_CONFIG));
        tester.executeAjaxEvent(choice, "change");
        // verify the Directory components go away and the PG config is visible
        tester.assertVisible(settings + "repositoryNamePanel");
        tester.assertInvisible(settings + "parentDirectory");
        tester.assertVisible(settings + "pgPanel");
    }

    @Test
    public void testImportPanel() {
        navigateToRepositoriesPage();
        tester.clickLink("headerPanel:importExisting");
        // verify the page is a RepositoryImportPage
        tester.assertRenderedPage(RepositoryImportPage.class);
        // verify the type dropdown
        DropDownChoiceParamPanel panel =
                (DropDownChoiceParamPanel)
                        tester.getComponentFromLastRenderedPage(
                                "panel:repoForm:repo:configChoicePanel");
        DropDownChoice<Serializable> choice = panel.getFormComponent();
        // verify Directory is selected
        assertEquals(
                "Expected DropDwon to be set to " + DropDownModel.DIRECTORY_CONFIG,
                DropDownModel.DIRECTORY_CONFIG,
                choice.getModelObject());
        // verify Parent Directory component
        final String settings = "panel:repoForm:repo:settingsContainer:";
        tester.assertVisible(settings + "repoDirectoryPanel");
        tester.assertInvisible(settings + "repositoryNamePanel");
        tester.assertInvisible(settings + "pgPanel");
        // now select PG config
        FormTester formTester = tester.newFormTester("panel:repoForm");
        formTester.select(
                "repo:configChoicePanel:border:border_body:paramValue",
                choice.getChoices().indexOf(DropDownModel.PG_CONFIG));
        tester.executeAjaxEvent(choice, "change");
        // verify PG config
        tester.assertInvisible(settings + "repoDirectoryPanel");
        tester.assertVisible(settings + "repositoryNamePanel");
        tester.assertVisible(settings + "pgPanel");
    }
}
