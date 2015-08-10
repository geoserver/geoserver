/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
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
        
        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
        
        GeoServerApplication app = 
 (GeoServerApplication) applicationContext
                .getBean("webApplication");
        tester = new WicketTester(
                (GeoServerApplication) applicationContext.getBean("webApplication"));
        app.init();
    }

    @After
    public void clearErrorMessages() {
        Session.get().cleanupFeedbackMessages();
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        tester.destroy();
    }

    public GeoServerApplication getGeoServerApplication(){
        return GeoServerApplication.get();
    }

    /**
     * Logs in as administrator.
     */
    public void login(){
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    public void logout(){
        login("anonymousUser","", "ROLE_ANONYMOUS");
    }
    
    /**
     * Prints the specified component/page containment hierarchy to the standard output
     * <p>
     * Each line in the dump looks like: <componentId>(class) 'value'
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
    * <p>
    * Each line in the dump looks like: <componentId>(class) 'value'
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
    
    public void prefillForm(final FormTester tester) {
        Form form = tester.getForm();
        form.visitChildren(new Component.IVisitor() {
            
            public Object component(Component component) {
                if(component instanceof FormComponent) {
                    FormComponent fc = (FormComponent) component;
                    String name = fc.getInputName();
                    String value = fc.getValue();
                    
                    tester.setValue(name, value);
                }
                return Component.IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }
    
    /**
     * Finds the component whose model value equals to the specified content, and
     * the component class is equal, subclass or implementor of the specified class
     * @param root the component under which the search is to be performed
     * @param content 
     * @param componentClass the target class, or null if any component will do
     * @return
     */
    public Component findComponentByContent(MarkupContainer root, Object content, Class componentClass) {
        ComponentContentFinder finder = new ComponentContentFinder(content);
        root.visitChildren(componentClass, finder);
        return finder.candidate;
    }
    
    class ComponentContentFinder implements Component.IVisitor {
        Component candidate;
        Object content;
        
        ComponentContentFinder(Object content) {
            this.content = content;
        }
        

        public Object component(Component component) {
            if(content.equals(component.getDefaultModelObject())) {
                this.candidate = component;
                return Component.IVisitor.STOP_TRAVERSAL;
            }
            return Component.IVisitor.CONTINUE_TRAVERSAL;
        }
        
    }
    
    /**
     * Helper method to initialize a standalone WicketTester with the proper 
     * customizations to do message lookups.
     */
    public static void initResourceSettings(WicketTester tester) {
        tester.getApplication().getResourceSettings().setResourceStreamLocator(new GeoServerResourceStreamLocator());
        tester.getApplication().getResourceSettings().addStringResourceLoader(0, new GeoServerStringResourceLoader());
    }
    
    /**
     * Get Ajax Event Behavior attached to a component. 
     * 
     * @param path path to component
     * @param event the name of the event
     * @return
     */
    protected AjaxEventBehavior getAjaxBehavior(String path, String event) {
        for (IBehavior b : tester.getComponentFromLastRenderedPage(path).getBehaviors()) {
            if (b instanceof AjaxEventBehavior && ((AjaxEventBehavior) b).getEvent().equals(event)) {
                return (AjaxEventBehavior) b;            
            }
        }
        return null;
    }

    /**
     * Execute Ajax Event Behavior with attached value.
     * Particularly useful to execute an onchange in a DropDownChoice (not supported by tester
     * or formtester in wicket 1.4). 
     * 
     * @param path
     * @param event
     * @param value
     */
    protected void executeAjaxEventBehavior(String path, String event, String value) {        
        AjaxEventBehavior behavior = getAjaxBehavior(path, event);
        CharSequence url = behavior.getCallbackUrl(false);
        WebRequestCycle cycle = tester.setupRequestAndResponse(true);
        tester.getServletRequest().setRequestToRedirectString(url.toString());
        String[] ids = path.split(":");
        String id = ids[ids.length-1];
        tester.getServletRequest().setParameter(id, value);
        tester.processRequestCycle(cycle);
    }
}
