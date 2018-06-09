/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.web.RepositoriesPage;
import org.geogig.geoserver.web.RepositoryImportPage;
import org.junit.Test;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Repository;

/** */
public class RepositoryImportPanelTest extends CommonPanelTest {

    private static final String FORM_PREFIX = "panel:repoForm:";
    private static final String SETTINGS_PREFIX = FORM_PREFIX + "repo:settingsContainer:";

    private static final String IMPORT_LINK = FORM_PREFIX + "import";
    private static final String FEEDBACK = FORM_PREFIX + "feedback";

    @Override
    protected String getStartPage() {
        return "headerPanel:importExisting";
    }

    @Override
    protected Class<? extends Page> getStartPageClass() {
        return RepositoryImportPage.class;
    }

    @Override
    protected String getFormChoiceComponent() {
        return "repo:configChoicePanel:border:border_body:paramValue";
    }

    @Override
    protected String getFrom() {
        return "panel:repoForm";
    }

    @Override
    protected String getChoicePanel() {
        return "panel:repoForm:repo:configChoicePanel";
    }

    @Test
    public void testPGImportMissingFields() throws IOException {
        navigateToStartPage();
        // select PG config from the dropdown
        select(DropDownModel.PG_CONFIG);
        // verify the PG config components are visible
        verifyPostgreSQLBackendComponents();
        // click the Import button
        tester.executeAjaxEvent(IMPORT_LINK, "click");
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
    public void testDirectoryImportMissingFields() throws IOException {
        navigateToStartPage();
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
        // click the Import button
        tester.executeAjaxEvent(IMPORT_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, repo directory will be empty
        List<String> expectedMsgs = Lists.newArrayList("Field 'Repository directory' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

    private File setupExistingRocksDBRepo() throws IOException {
        // get a tempdirectory
        File repoFolder = temp.newFolder();
        URI uri = repoFolder.toURI();
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_URL, uri.toString());
        Repository repo = RepositoryManager.get().createRepo(hints);
        assertNotNull(repo);
        repo.command(InitOp.class).call();
        repo.command(ConfigOp.class)
                .setAction(ConfigOp.ConfigAction.CONFIG_SET)
                .setName("user.name")
                .setValue("TestUser")
                .call();
        repo.command(ConfigOp.class)
                .setAction(ConfigOp.ConfigAction.CONFIG_SET)
                .setName("user.email")
                .setValue("test@user.com")
                .call();
        repo.close();
        return new File(repo.getLocation()).getParentFile();
    }

    @Test
    public void testImportExistingRocksDBRepo() throws IOException {
        navigateToStartPage();
        final File repoDir = setupExistingRocksDBRepo();
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
        // get the form
        FormTester formTester = tester.newFormTester(getFrom());
        // and a directory
        TextField parentDirectory =
                (TextField)
                        tester.getComponentFromLastRenderedPage(
                                SETTINGS_PREFIX
                                        + "repoDirectoryPanel:wrapper:wrapper_body:repoDirectory");
        formTester.setValue(parentDirectory, repoDir.getCanonicalPath());
        // click the Save button
        tester.executeAjaxEvent(IMPORT_LINK, "click");
        // get the page. It should be a RepositoriesPage if the SAVE was successful
        tester.assertRenderedPage(RepositoriesPage.class);
    }

    @Override
    protected void verifyDirectoryBackendComponents() {
        // verify Directory config components are visible
        tester.assertVisible(SETTINGS_PREFIX + "repoDirectoryPanel");
        tester.assertInvisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(IMPORT_LINK);
    }

    @Override
    protected void verifyPostgreSQLBackendComponents() {
        // verify the PG config components are visible
        tester.assertInvisible(SETTINGS_PREFIX + "repoDirectoryPanel");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertVisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(IMPORT_LINK);
    }

    @Override
    protected void verifyNoBackendComponents() {
        // verify Directory and PostgreSQL config components are invisible
        tester.assertInvisible(SETTINGS_PREFIX + "repoDirectoryPanel");
        tester.assertInvisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(IMPORT_LINK);
    }
}
