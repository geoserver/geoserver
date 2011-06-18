package org.geoserver.web.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.apache.wicket.Component;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;

public class ConfirmRemovalUserPanelTest extends GeoServerWicketTestSupport {
    
    void setupPanel(final User... roots) {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            
            public Component buildComponent(String id) {
                return new ConfirmRemovalUserPanel(id, roots);
            }
        }));
    }
    
    public void testRemoveDataAccessRule() {
        GrantedAuthority[] authorities = new GrantedAuthority[] { new GrantedAuthorityImpl("ROLE_FRANK") };
        setupPanel(new User("frank", "francesco", true, true, true, true, authorities));
        
        // print(tester.getLastRenderedPage(), true, true);
        
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertLabel("form:panel:removedObjects:rulesRemoved:rules", "frank");
    }

}
