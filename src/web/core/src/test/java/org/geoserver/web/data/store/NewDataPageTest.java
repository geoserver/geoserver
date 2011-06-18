/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.web.GeoServerWicketTestSupport;

public class NewDataPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        tester.startPage(new NewDataPage());
        
        //print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        tester.assertRenderedPage(NewDataPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("storeForm:vectorResources", ListView.class);
        tester.assertComponent("storeForm:rasterResources", ListView.class);
    }
    
    /**
     * Need to use a static class so it has no back pointer to NewDataPageTest which is not serializable
     * @author groldan
     *
     */
    private static class NewDataPageWithFakeCatalog extends NewDataPage{
        @Override
        protected Catalog getCatalog(){
            return new CatalogImpl();
        }
    }
    
    public void testLoadWithNoWorkspaces() {
        tester.startPage(new NewDataPageWithFakeCatalog());
        tester.assertRenderedPage(NewDataPageWithFakeCatalog.class);

        String expectedErrMsg = (String) new ResourceModel("NewDataPage.noWorkspacesErrorMessage")
                .getObject();
        assertNotNull(expectedErrMsg);
        tester.assertErrorMessages(new String[] { expectedErrMsg });
    }

    public void testClickLink() {
        Label label = (Label) findComponentByContent(tester.getLastRenderedPage(), "Properties", Label.class);
        // getPath() will start with 0: which indicates the page
        tester.clickLink(label.getParent().getPath().substring(2));
        
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(DataAccessNewPage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
        tester.assertModelValue("dataStoreForm:storeType", "Properties");
    
    }

}
