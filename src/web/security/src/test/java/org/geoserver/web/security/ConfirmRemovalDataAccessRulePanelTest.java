package org.geoserver.web.security;

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.Component;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;

public class ConfirmRemovalDataAccessRulePanelTest extends GeoServerWicketTestSupport {

    void setupPanel(final DataAccessRule... roots) {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            
            public Component buildComponent(String id) {
                return new ConfirmRemovalDataAccessRulePanel(id, roots);
            }
        }));
    }
    
    public void testRemoveDataAccessRule() {
        Set<String> roles = new HashSet<String>();
        roles.add("*");
        setupPanel(new DataAccessRule("*","*",AccessMode.READ,roles));
        
        // print(tester.getLastRenderedPage(), true, true);
        
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertLabel("form:panel:removedObjects:rulesRemoved:rules", "*.*.READ=[*]");
        
//        String layers = tester.getComponentFromLastRenderedPage("form:panel:removedObjects:layersRemoved:layers").getModelObjectAsString();
//        String[] layerArray = layers.split(", ");
//        DataStoreInfo citeStore = getCatalog().getStoreByName("cite", DataStoreInfo.class);
//        List<FeatureTypeInfo> typeInfos = getCatalog().getResourcesByStore(citeStore, FeatureTypeInfo.class);
//        assertEquals(typeInfos.size(), layerArray.length);
    }
}
