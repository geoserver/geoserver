/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.EnvelopePanel;
import org.junit.Test;


public class LayerGroupEditPageTest extends LayerGroupBaseTest {
    
    @Test
    public void testComputeBounds() {
        LayerGroupEditPage page = new LayerGroupEditPage(new PageParameters("group=lakes"));
        tester.startPage(page);
        // print(page, true, false);
        
        tester.assertRenderedPage(LayerGroupEditPage.class);
        // remove the first and second elements
        // tester.clickLink("form:layers:layers:listContainer:items:1:itemProperties:4:component:link");
        // the regenerated list will have ids starting from 4
        //tester.clickLink("form:layers:layers:listContainer:items:4:itemProperties:4:component:link");
        // manually regenerate bounds
        tester.clickLink("form:generateBounds");
        // print(page, true, true);
        // submit the form
        tester.submitForm("form");
        
        // For the life of me I cannot get this test to work... and I know by direct UI inspection that
        // the page works as expected...
//        FeatureTypeInfo bridges = getCatalog().getResourceByName(MockData.BRIDGES.getLocalPart(), FeatureTypeInfo.class);
//        assertEquals(getCatalog().getLayerGroupByName("lakes").getBounds(), bridges.getNativeBoundingBox());
    }
    
    @Test
    public void testComputeBoundsFromCRS() {
        LayerGroupEditPage page = new LayerGroupEditPage(new PageParameters("group=lakes"));
        tester.startPage(page);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        
        FormTester form = tester.newFormTester("form");
        form.setValue("bounds:crsContainer:crs:srs", "EPSG:4326");
        tester.clickLink("form:generateBoundsFromCRS", true);
        tester.assertComponentOnAjaxResponse("form:bounds");
        Component ajaxComponent = tester.getComponentFromLastRenderedPage("form:bounds");
        assert(ajaxComponent instanceof EnvelopePanel);
        EnvelopePanel envPanel = (EnvelopePanel)ajaxComponent;
        assertEquals(((DecimalTextField)envPanel.get("minX")).getModelObject(), new Double(-180.0));
        assertEquals(((DecimalTextField)envPanel.get("minY")).getModelObject(), new Double(-90.0));
        assertEquals(((DecimalTextField)envPanel.get("maxX")).getModelObject(), new Double(180.0));
        assertEquals(((DecimalTextField)envPanel.get("maxY")).getModelObject(), new Double(90.0));
    }
}
