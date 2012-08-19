package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public abstract class GeoServerWicketTestSupport extends GeoServerSecurityTestSupport {
    public static WicketTester tester;

    public void oneTimeSetUp() throws Exception {        
        super.oneTimeSetUp();
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");
        
        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
        
        GeoServerApplication app = 
            (GeoServerApplication) applicationContext.getBean("webApplication");
        tester = new WicketTester(app);
        app.init();
        
    }

    @Override
    protected void oneTimeTearDown() throws Exception {
        super.oneTimeTearDown();
        tester.destroy();
    }

    public GeoServerApplication getGeoServerApplication(){
        return GeoServerApplication.get();
    }
    
    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }

    /**
     * Logs in as administrator.
     */
    public void login(){
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    /**
     * Logs in with the specified credentials and associates the specified roles with the resulting
     * authentication. 
     */
    public void login(String user, String passwd, String... roles) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        for (String role : roles) {
            l.add(new GrantedAuthorityImpl(role));
        }
        
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,passwd,l));
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
       WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
   }
    
    public void prefillForm(final FormTester tester) {
        Form form = tester.getForm();
        form.visitChildren(new IVisitor<Component, Void>() {
            
            public void component(Component component, IVisit<Void> visit) {
                if(component instanceof FormComponent) {
                    FormComponent fc = (FormComponent) component;
                    String name = fc.getInputName();
                    String value = fc.getValue();
                    
                    tester.setValue(name, value);
                }
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
    
    class ComponentContentFinder implements IVisitor<Component, Void> {
        Component candidate;
        Object content;
        
        ComponentContentFinder(Object content) {
            this.content = content;
        }
        

        public void component(Component component, IVisit<Void> visit) {
            if(content.equals(component.getDefaultModelObject())) {
                this.candidate = component;
                visit.stop();
            }
        }
        
    }
    
    /**
     * Helper method to initialize a standalone WicketTester with the proper 
     * customizations to do message lookups.
     */
    public static void initResourceSettings(WicketTester tester) {
        tester.getApplication().getResourceSettings().setResourceStreamLocator(new GeoServerResourceStreamLocator());
        tester.getApplication().getResourceSettings().getStringResourceLoaders().add(0, new GeoServerStringResourceLoader());
    }
}
