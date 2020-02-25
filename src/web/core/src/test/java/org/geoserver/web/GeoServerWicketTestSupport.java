/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.web.GeoServerApplication.GEOSERVER_CSRF_DISABLED;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.After;
import org.junit.BeforeClass;

public abstract class GeoServerWicketTestSupport extends GeoServerSecurityTestSupport {
    public static WicketTester tester;

    @BeforeClass
    public static void disableBrowserDetection() {
        // disable browser detection, makes testing harder for nothing
        GeoServerApplication.DETECT_BROWSER = false;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        System.setProperty(GEOSERVER_CSRF_DISABLED, "true");

        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);

        GeoServerApplication app =
                (GeoServerApplication) applicationContext.getBean("webApplication");
        tester = new WicketTester(app, false);
        app.init();
    }

    @After
    public void clearErrorMessages() {
        if (tester != null && !tester.getFeedbackMessages(IFeedbackMessageFilter.ALL).isEmpty()) {
            tester.cleanupFeedbackMessages();
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        tester.destroy();
    }

    public GeoServerApplication getGeoServerApplication() {
        return GeoServerApplication.get();
    }

    /** Logs in as administrator. */
    public void login() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    public void logout() {
        login("anonymousUser", "", "ROLE_ANONYMOUS");
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * Finds the component whose model value equals to the specified content, and the component
     * class is equal, subclass or implementor of the specified class
     *
     * @param root the component under which the search is to be performed
     * @param componentClass the target class, or null if any component will do
     */
    public Component findComponentByContent(
            MarkupContainer root, Object content, Class<?> componentClass) {
        ComponentContentFinder finder = new ComponentContentFinder(content);
        root.visitChildren(componentClass, finder);
        return finder.candidate;
    }

    class ComponentContentFinder implements IVisitor<Component, Void> {
        Component candidate;
        Object content;

        ComponentContentFinder(Object content) {
            this.content = content;
        }

        @Override
        public void component(Component component, IVisit<Void> visit) {
            if (content.equals(component.getDefaultModelObject())) {
                this.candidate = component;
                visit.stop();
            }
        }
    }

    /**
     * Helper method to initialize a standalone WicketTester with the proper customizations to do
     * message lookups.
     */
    public static void initResourceSettings(WicketTester tester) {
        tester.getApplication()
                .getResourceSettings()
                .setResourceStreamLocator(new GeoServerResourceStreamLocator());
        tester.getApplication()
                .getResourceSettings()
                .getStringResourceLoaders()
                .add(0, new GeoServerStringResourceLoader());
    }

    /**
     * Get Ajax Event Behavior attached to a component.
     *
     * @param path path to component
     * @param event the name of the event
     */
    protected AjaxEventBehavior getAjaxBehavior(String path, String event) {
        for (Behavior b : tester.getComponentFromLastRenderedPage(path).getBehaviors()) {
            if (b instanceof AjaxEventBehavior
                    && ((AjaxEventBehavior) b).getEvent().equals(event)) {
                return (AjaxEventBehavior) b;
            }
        }
        return null;
    }

    /** Execute Ajax Event Behavior with attached value. */
    protected void executeAjaxEventBehavior(String path, String event, String value) {
        String[] ids = path.split(":");
        String id = ids[ids.length - 1];
        tester.getRequest().setParameter(id, value);
        tester.executeAjaxEvent(path, event);
    }

    /**
     * Sets the value of a form component that might not be included in a form (because maybe we are
     * using it via Ajax). By itself it just prepares the stage for a subsequent Ajax request
     *
     * @param component The {@link FormComponent} whose value we are going to set
     * @param value The form value (as we'd set it in a HTML form)
     */
    protected void setFormComponentValue(FormComponent component, String value) {
        tester.getRequest().getPostParameters().setParameterValue(component.getInputName(), value);
    }
}
