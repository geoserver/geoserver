/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.web.RepositoriesPage;
import org.geogig.geoserver.web.RepositoryEditPage;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.geogig.cli.test.functional.CLITestContextBuilder;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;
import org.locationtech.geogig.test.TestPlatform;

/** */
public class RepositoryEditPanelTest extends CommonPanelTest {

    private static final String FORM_PREFIX = "panel:repoForm:";

    private static final String SETTINGS_PREFIX =
            FORM_PREFIX + "repo:repositoryConfig:settingsContainer:";

    private static final String SAVE_LINK = FORM_PREFIX + "save";

    private static final String FEEDBACK = FORM_PREFIX + "feedback";

    @BeforeClass
    public static void beforeClass() {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        GlobalContextBuilder.builder(new CLITestContextBuilder(new TestPlatform(tmp, tmp)));
    }

    @Override
    protected String getStartPage() {
        return "headerPanel:addNew";
    }

    @Override
    protected Class<? extends Page> getStartPageClass() {
        return RepositoryEditPage.class;
    }

    @Override
    protected String getFormChoiceComponent() {
        return "repo:repositoryConfig:configChoicePanel:border:border_body:paramValue";
    }

    @Override
    protected String getFrom() {
        return "panel:repoForm";
    }

    @Override
    protected String getChoicePanel() {
        return "panel:repoForm:repo:repositoryConfig:configChoicePanel";
    }

    @Test
    public void testPGAddWithEmptyFields() throws IOException {
        navigateToStartPage();
        // select PG config from the dropdown
        select(DropDownModel.PG_CONFIG);
        // verify the PG config components are visible
        verifyPostgreSQLBackendComponents();
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, 3 required fields will be emtpy: repo name, database and password
        List<String> expectedMsgs =
                Lists.newArrayList(
                        "Field 'Repository Name' is required.",
                        "Field 'Database Name' is required.",
                        "Field 'Password' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

    @Test
    public void testDirectoryAddWithEmptyFields() throws IOException {
        navigateToStartPage();
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, repo parent directory and repo name will be empty
        List<String> expectedMsgs =
                Lists.newArrayList(
                        "Field 'Repository Name' is required.",
                        "Field 'Parent directory' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

    @Test
    public void testAddNewRocksDBRepo() throws IOException {
        navigateToStartPage();
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
        // get the form
        FormTester formTester = tester.newFormTester(getFrom());
        // now set a name
        TextParamPanel repoNamePanel =
                (TextParamPanel)
                        tester.getComponentFromLastRenderedPage(
                                SETTINGS_PREFIX + "repositoryNamePanel");
        formTester.setValue(repoNamePanel.getFormComponent(), "temp_repo");
        // and a directory
        TextField parentDirectory =
                (TextField)
                        tester.getComponentFromLastRenderedPage(
                                SETTINGS_PREFIX + "parentDirectory:wrapper:wrapper_body:value");
        formTester.setValue(parentDirectory, temp.getRoot().getCanonicalPath());
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        // get the page. It should be a RepositoriesPage if the SAVE was successful
        tester.assertRenderedPage(RepositoriesPage.class);
    }

    @Override
    protected void verifyDirectoryBackendComponents() {
        // verify Directory config components are visible
        tester.assertVisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(SAVE_LINK);
    }

    @Override
    protected void verifyPostgreSQLBackendComponents() {
        // verify PostgreSQL config components are visible
        tester.assertInvisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertVisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(SAVE_LINK);
    }

    @Override
    protected void verifyNoBackendComponents() {
        // verify Directory and PostgreSQL config components are invisible
        tester.assertInvisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(SAVE_LINK);
    }
}
