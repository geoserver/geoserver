package org.geoserver.web.data.store;

import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerWicketTestSupport;

public class StorePageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        login();
        tester.startPage(StorePage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();
        
        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), getCatalog().getStores(StoreInfo.class).size());
        StoreInfo ws = (StoreInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals(getCatalog().getStores(StoreInfo.class).get(0), ws);
    }
}
