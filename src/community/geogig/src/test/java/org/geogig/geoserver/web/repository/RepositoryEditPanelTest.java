/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.web.RepositoryEditPage;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 *
 */
public class RepositoryEditPanelTest extends CommonPanelTest {

    private static final String FORM_PREFIX = "panel:repoForm:";
    private static final String SETTINGS_PREFIX = FORM_PREFIX + "repo:repositoryConfig:settingsContainer:";

    private static final String SAVE_LINK = FORM_PREFIX + "save";
    private static final String FEEDBACK = FORM_PREFIX + "feedback";

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
    public void testPGAddWithEmptyFields() {
        // select PG config from the dropdown
        select(DropDownModel.PG_CONFIG);
        // verify the PG config components are visible
        tester.assertInvisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertVisible(SETTINGS_PREFIX + "pgPanel");
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, 3 required fields will be emtpy: repo name, database and password
        List<String> expectedMsgs = Lists.newArrayList("Field 'Repository Name' is required.",
                "Field 'Database Name' is required.",
                "Field 'Password' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

    @Test
    public void testDirectoryAddWithEmptyFields() {
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        tester.assertVisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, repo parent directory and repo name will be empty
        List<String> expectedMsgs = Lists.newArrayList("Field 'Repository Name' is required.",
                "Field 'Parent directory' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

}
