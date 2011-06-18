package org.geoserver.web.security;

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.Component;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;

public class ConfirmRemovalServicePanelTest extends GeoServerWicketTestSupport {
    
    void setupPanel(final ServiceAccessRule... roots) {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            
            public Component buildComponent(String id) {
                return new ConfirmRemovalServicePanel(id, roots);
            }
        }));
    }
    
    public void testRemoveDataAccessRule() {
        Set<String> roles = new HashSet<String>();
        roles.add("*");
        setupPanel(new ServiceAccessRule("*","*",roles));
        
        // print(tester.getLastRenderedPage(), true, true);
        
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertLabel("form:panel:removedObjects:rulesRemoved:rules", "*.*=[*]");
    }

}
