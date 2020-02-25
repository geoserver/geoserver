/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.apache.wicket.Page;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Before;
import org.junit.Test;

public class PasswordParamPanelTest {

    private WicketTester tester;

    @Before
    public void setUp() {
        tester =
                new WicketTester(
                        new WebApplication() {
                            @Override
                            public Class<? extends Page> getHomePage() {
                                return null;
                            }
                        });
    }

    @Test
    public void homepageRendersSuccessfully() {
        // start and render the test page
        String password = "thePassword";
        Model<String> pwModel = new Model<>(password);
        FormTestPage testPage =
                new FormTestPage(
                        (ComponentBuilder)
                                id ->
                                        new PasswordParamPanel(
                                                id, pwModel, new Model("label"), true));
        tester.startPage(testPage);

        // check the password is not visible in source
        String text = tester.getLastResponseAsString();
        assertThat(text, not(containsString(password)));

        // submit a new value
        FormTester formTester = this.tester.newFormTester(FormTestPage.FORM);
        formTester.setValue("panel:border:border_body:paramValue", "newPassword");
        formTester.submit();

        // still does not show
        assertThat(tester.getLastResponseAsString(), not(containsString(password)));

        // but the model has been updated
        assertEquals("newPassword", pwModel.getObject());
    }
}
