package org.geoserver.web;

import static org.geoserver.web.GeoServerApplication.GEOSERVER_CSRF_DISABLED;
import static org.geoserver.web.GeoServerApplication.GEOSERVER_CSRF_WHITELIST;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import org.apache.wicket.markup.html.form.IFormSubmitListener;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class GeoServerWicketCsrfTest extends GeoServerWicketTestSupport {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {{"true", "foo.com"}, {"false", "geoserver.org"}, {"false", ""}});
    }

    @Parameter(0)
    public String csrfDisabled;

    @Parameter(1)
    public String csrfWhitelist;

    private WorkspaceInfo citeWorkspace;

    @Before
    public void init() {
        // Update the Csrf properties and re-init the test app
        System.setProperty(GEOSERVER_CSRF_WHITELIST, csrfWhitelist);
        System.setProperty(GEOSERVER_CSRF_DISABLED, csrfDisabled);
        GeoServerApplication app =
                (GeoServerApplication) applicationContext.getBean("webApplication");
        tester = new WicketTester(app, false);
        app.init();

        login();
        // initialize a workspace to test with
        citeWorkspace = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);

        GeoServer gs = getGeoServer();
        SettingsInfo s = gs.getSettings(citeWorkspace);
        if (s != null) {
            gs.remove(s);
        }
        NamespaceInfo citeNS = getCatalog().getNamespaceByPrefix(MockData.CITE_PREFIX);
        citeNS.setURI(MockData.CITE_URI);
        getCatalog().save(citeNS);

        tester.startPage(new WorkspaceEditPage(citeWorkspace));
    }

    @Test
    // form succeeds if disabled, or geoserver.org
    public void testFormSubmitWhitelistedDomain() {
        FormTester form = tester.newFormTester("form");

        // Set up HTTP requst with necessary headers
        MockHttpServletRequest request = tester.getRequest();

        String relativePath =
                form.getForm()
                        .getRootForm()
                        .urlFor(IFormSubmitListener.INTERFACE, new PageParameters())
                        .toString()
                        .substring(1);

        request.setServerName("geoserver.org");
        request.setHeader("Origin", "http://www.geoserver.org");
        request.setHeader("Referer", "http://www.geoserver.org" + relativePath);

        // try changing the URI of a workspace
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("save");

        if ("".equals(csrfWhitelist)) {
            // form submit should fail
            assertNull(tester.getLastRenderedPage());
        } else {
            // form submit should succeed
            tester.assertRenderedPage(WorkspacePage.class);
            tester.assertNoErrorMessage();
        }
    }

    @Test
    // form fails if geoserver.org or no whitlist
    public void testFormSubmitNotWhitelistedDomain() {

        FormTester form = tester.newFormTester("form");

        // Set up HTTP requst with necessary headers
        MockHttpServletRequest request = tester.getRequest();

        String relativePath =
                form.getForm()
                        .getRootForm()
                        .urlFor(IFormSubmitListener.INTERFACE, new PageParameters())
                        .toString()
                        .substring(1);

        request.setServerName("geoserver.org");
        request.setHeader("Origin", "http://www.remote.com");
        request.setHeader("Referer", "http://www.remote.com" + relativePath);

        // try changing the URI of a workspace
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("save");

        if ("true".equals(csrfDisabled)) {
            // form submit should succeed
            tester.assertRenderedPage(WorkspacePage.class);
            tester.assertNoErrorMessage();
        } else {
            // form submit should fail
            assertNull(tester.getLastRenderedPage());
        }
    }
}
