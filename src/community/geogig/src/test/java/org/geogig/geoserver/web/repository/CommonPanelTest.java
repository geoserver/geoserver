/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.web.RepositoriesPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.junit.Before;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Parent test class to hold common methods.
 */
public abstract class CommonPanelTest extends GeoServerWicketTestSupport {

    protected RepositoriesPage repoPage;

    /**
     * Before method that navigates all subclass tests to their respective starting pages.
     */
    @Before
    public void navigateToStartPage() {
        // login
        login();
        // create RepositoriesPage
        repoPage = new RepositoriesPage();
        // start on the repos page
        tester.startPage(repoPage);
        // click the appropriate link
        tester.clickLink(getStartPage());
        // verify the page is the correct type
        tester.assertRenderedPage(getStartPageClass());
    }

    /**
     * Asserts that FeedbackMessages match the expected list of messages.
     *
     * @param actualMsgs List of FeedbackMessages received from submitting the form.
     * @param expectedMsgs List of expected String messages that should appear in the Feedback Panel.
     */
    protected void assertFeedbackMessages(List<FeedbackMessage> actualMsgs, List<String> expectedMsgs) {
        // assert sizes are equal
        assertEquals("Unexpected number of FeedbackMessages", expectedMsgs.size(), actualMsgs.size());
        // loop through expected and assert they are present in the actuals
        final List<String> actuals = Lists.transform(actualMsgs, new Function<FeedbackMessage, String>() {
            @Override
            public String apply(FeedbackMessage input) {
                return input.getMessage().toString();
            }
        });
        for (String expectedMsg : expectedMsgs) {
            assertTrue(String.format("Missing expected FeedbackMessage: %s", expectedMsg), actuals.contains(expectedMsg));
        }
    }

    /**
     * Simulates a user selecting the repository configuration type from the Add/Edit/Import panels.
     *
     * @param type The config type to select from the DropDownChoice.
     */
    protected void select(final String type) {
        // get the component holding the dropdown
        DropDownChoiceParamPanel panel = (DropDownChoiceParamPanel) tester.getComponentFromLastRenderedPage(
                getChoicePanel());
        // get the dropdown choice component
        DropDownChoice<Serializable> choice = panel.getFormComponent();
        // get the form
        FormTester formTester = tester.newFormTester(getFrom());
        // make the selection
        formTester.select(getFormChoiceComponent(), choice.getChoices().indexOf(type));
        // fire the ajax event to simulate the selection
        tester.executeAjaxEvent(choice, "change");
    }

    /**
     * Retrieve the String representation of the DropDownChoice component on the form.
     *
     * @return String representation of the path to the Choice component, relative to the containing form.
     */
    protected abstract String getFormChoiceComponent();

    /**
     * Retrieve the String representation of the Form.
     *
     * @return String representation of the path to the Form component.
     */
    protected abstract String getFrom();

    /**
     * Retrieve the String representation of the DropDownChoice panel.
     *
     * @return String representation of the path to the panel containing the DropDownChoice component, relative to the
     *         start page.
     */
    protected abstract String getChoicePanel();

    /**
     * Retrieve the String representation of the Page from which this test should start.
     *
     * @return String representation of the path to the Page from which subclasses should start testing.
     */
    protected abstract String getStartPage();

    /**
     * Retrieve the Class of the Page from which this test should start.
     *
     * @return Page subclass Class type of the start page for this test. Used for asserting the correct start page.
     */
    protected abstract Class<? extends Page> getStartPageClass();
}
