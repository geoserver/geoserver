package org.geoserver.web.wicket;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;

public class SRSPanelTest extends GeoServerWicketTestSupport implements Serializable {
    
    @Override
    protected void setUpInternal() throws Exception {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            
            public Component buildComponent(String id) {
                return new SRSListPanel(id) {
                    
                    private String codeClicked;

                    @Override
                    protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                        codeClicked = epsgCode;
                    }
                };
            }
        }));
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
    }
}
