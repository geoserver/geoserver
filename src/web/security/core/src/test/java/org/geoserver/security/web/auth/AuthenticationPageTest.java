/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.TagTester;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class AuthenticationPageTest extends AbstractSecurityWicketTestSupport {

    AuthenticationPage page;

    @Before
    public void init() throws Exception {
        deactivateRORoleService();
        // needed to use the tag tester
        tester.getApplication().getMarkupSettings().setStripWicketTags(false);
    }

    @Test
    public void testSetProvider() throws Exception {
        initializeForXML();
        createUserPasswordAuthProvider("default2", "default");
        activateRORoleService();

        tester.startPage(page = new AuthenticationPage());
        tester.assertComponent("form:providerChain:authProviderNames:recorder", Recorder.class);

        @SuppressWarnings("unchecked")
        List<String> selected =
                (List<String>)
                        (page.get("form:providerChain:authProviderNames")).getDefaultModelObject();
        assertEquals(1, selected.size());
        assertTrue(selected.contains("default"));

        FormTester form = tester.newFormTester("form");
        form.setValue("providerChain:authProviderNames:recorder", "default2");
        form.submit("save");
        tester.assertNoErrorMessage();

        boolean authProvFound = false;
        for (GeoServerAuthenticationProvider prov :
                getSecurityManager().getAuthenticationProviders()) {
            if (UsernamePasswordAuthenticationProvider.class.isAssignableFrom(prov.getClass())) {
                if (prov.getName().equals("default2")) {
                    authProvFound = true;
                    break;
                }
            }
        }
        assertTrue(authProvFound);
    }

    //    protected void assignAuthProvider(String providerName) throws Exception {
    //        form.setValue("config.authProviderNames:recorder", providerName);
    //        // tester.executeAjaxEvent(formComponentId+":config.authProviderNames:recorder",
    //        // change);
    //        // newFormTester();
    //    }
    //
    protected boolean hasAuthProviderImpl(Class<?> aClass) {
        for (Object o : getSecurityManager().getProviders()) {
            if (o.getClass() == aClass) return true;
        }
        return false;
    }

    @Test
    public void testLogoutLocation() {
        login();
        tester.startPage(AuthenticationPage.class);
        tester.assertRenderedPage(AuthenticationPage.class);
        TagTester logoutform = tester.getTagByWicketId("logoutform");
        // used to be http://localhost/j_spring_security_logout
        assertEquals(
                "http://localhost/context/j_spring_security_logout",
                logoutform.getAttribute("action"));
    }
}
