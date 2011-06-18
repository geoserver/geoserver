package org.geoserver.web.data.layergroup;

import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.LayerGroupInfo;

public class LayerGroupPageTest extends LayerGroupBaseTest {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        login();
        tester.startPage(LayerGroupPage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();
        
        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(getCatalog().getLayerGroups().size(), dv.size());
        LayerGroupInfo lg = (LayerGroupInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals(getCatalog().getLayerGroups().get(0), lg);
    }
}
