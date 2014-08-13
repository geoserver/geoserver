/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.dataset;

import gmx.iderc.geoserver.tjs.web.TJSWicketTestSupport;
import org.apache.wicket.PageParameters;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class DatasetEditPageTest extends TJSWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        try {
            login();
            PageParameters params = new PageParameters();
            params.add("dataStoreId", "DataStoreInfoImpl-40cce177:1399704da19:-7fff");
            params.add("datasetName", "DatosdeProvincias");
            tester.startPage(new DatasetEditPage(params));
        } catch (Exception ex) {
            Logger.getLogger(DatasetEditPageTest.class.getName()).log(Level.SEVERE, "Excepcion en setUpInternal(): " + ex.getMessage());
            throw ex;
        }
//        tester.assertRenderedPage(FrameworkPage.class);
//        tester.assertNoErrorMessage();
    }


    public void testBasicActions() {

//        if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated()){
//            Logger.getLogger(FrameworkPageTest.class.getName()).info("No se ha autenticado");
//            login();
//        }

        // test that we can load the page
        //System.out.println(tester.getLastRenderedPage().toString(true));
        // check it has one framework
        //GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
//        assertEquals(2, table.getDataProvider().size());
    }

}
